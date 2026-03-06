package com.softwarearchi.archi.services;

import com.softwarearchi.archi.utils.JwtUtil;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.UserRepository;
import com.softwarearchi.archi.models.Permission;
import com.softwarearchi.archi.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import com.softwarearchi.archi.models.VerificationToken;
import com.softwarearchi.archi.models.JwtToken;
import com.softwarearchi.archi.repository.VerificationTokenRepository;
import com.softwarearchi.archi.repository.JwtTokenRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import java.time.ZoneId;

/**
 * Service d'authentification : gère la connexion, l'inscription et la déconnexion.
 * C'est ici que réside la logique métier principale d'authentification.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final JwtUtil jwtUtil;
    private final VerificationTokenRepository tokenRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.mq.exchange:auth.events}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered:auth.user-registered}")
    private String userRegisteredRoutingKey;

    public AuthService(UserService userService, UserRepository userRepository, PermissionRepository permissionRepository,
            JwtUtil jwtUtil, VerificationTokenRepository tokenRepository, JwtTokenRepository jwtTokenRepository,
            RabbitTemplate rabbitTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.jwtUtil = jwtUtil;
        this.tokenRepository = tokenRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Inscrit un nouvel utilisateur dans le système.
     * 
     * @return Le token JWT d'authentification généré.
     */
    public String register(String firstName, String lastName, String email,
            String password, String phoneNumber) {
        logger.info("[SERVICE-AUTH] Starting registration for email: {}", email);
        logger.debug("[SERVICE-AUTH] Checking if email exists");
        if (userService.existsByEmail(email)) {
            logger.warn("[SERVICE-AUTH] Registration failed: Email {} already exists", email);
            throw new RuntimeException("User with email " + email + " already exists");
        }

        Permission basicPermission = getOrCreatePermission("READ_PROFILE", "Lire son propre profil");
        Permission editPermission = getOrCreatePermission("EDIT_PROFILE", "Modifier son propre profil");
        Permission deleteAccountPermission = getOrCreatePermission("DELETE_ACCOUNT", "Supprimer son propre compte");

        // Création d'une nouvelle entité utilisateur
        logger.info("[SERVICE-USER] Creating user: {}", email);
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(userService.hashPassword(password)); // Hashage du mot de passe avant de le stocker
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(true);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(basicPermission);
        permissions.add(editPermission);
        permissions.add(deleteAccountPermission);
        user.setPermissions(permissions);

        // Sauvegarde de l'utilisateur avec le(s) rôle(s) assigné(s)
        userRepository.save(user);
        logger.info("[SERVICE-USER] User saved with ID: {}", user.getId());

        // Génération du token de vérification
        String eventId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        String tokenClear = UUID.randomUUID().toString(); // The secret in the email
        String tokenHash = passwordEncoder.encode(tokenClear);

        VerificationToken verificationToken = new VerificationToken(
                tokenId, tokenHash, user.getId(), LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(verificationToken);

        // Envoi de l'événement à RabbitMQ
        Map<String, Object> eventData = Map.of(
                "eventId", eventId,
                "userId", user.getId(),
                "email", user.getEmail(),
                "tokenId", tokenId,
                "tokenClear", tokenClear,
                "occurredAt", LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, eventData);
        logger.info("[SERVICE-AUTH] Published UserRegisteredEvent for email: {}", email);
        // Génération du JWT pour le nouvel utilisateur
        logger.info("[SERVICE-AUTH] Generating new JWT for user ID: {}", user.getId());
        String token = jwtUtil.generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "permissions", user.getPermissions().stream().map(Permission::getName).toArray()));
        java.util.Date expiresAt = jwtUtil.extractExpiration(token);
        
        // Sauvegarde du JWT en base de données
        saveJwtToken(token, user.getEmail(), user.getId(), expiresAt);
        
        logger.info("[SERVICE-AUTH] JWT generated and saved, expires at: {}", expiresAt);
        return token;
    }

    /**
     * Authentifie un utilisateur avec les identifiants fournis.
     * 
     * @return Le token JWT d'authentification en cas de succès.
     */
    public String login(String email, String password) {
        // La tentative de connexion est loggée dans AuthController
        User user = userService.findByEmail(email);
        if (user == null) {
            logger.warn("[SERVICE-AUTH] Login failed: User not found");
            throw new RuntimeException("Invalid email or password");
        }
        logger.debug("[SERVICE-AUTH] User found: ID={}", user.getId());
        if (!userService.verifyPassword(password, user.getPassword())) {
            logger.warn("[SERVICE-AUTH] Login failed: Invalid password");
            throw new RuntimeException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            logger.warn("[SERVICE-AUTH] Login failed: Account disabled for email: {}", email);
            throw new RuntimeException("Account is disabled");
        }
        logger.info("[SERVICE-AUTH] Generating new JWT");
        String token = jwtUtil.generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "permissions", user.getPermissions().stream().map(Permission::getName).toArray()));
        
        // Sauvegarde du JWT en base de données
        java.util.Date expiresAt = jwtUtil.extractExpiration(token);
        saveJwtToken(token, user.getEmail(), user.getId(), expiresAt);
        
        logger.info("[SERVICE-AUTH] JWT generated and saved to database");
        return token;
    }

    // Déconnecte l'utilisateur en révoquant le token dans la base de données
    public void logout(String token) {
        int revoked = jwtTokenRepository.revokeToken(token);
        if (revoked > 0) {
            logger.info("[SERVICE-AUTH] Token revoked successfully");
        } else {
            logger.warn("[SERVICE-AUTH] Token not found in database");
        }
    }

    // Vérifie si un token JWT est valide (existe en base et non révoqué)
    public boolean isTokenValid(String token) {
        return jwtTokenRepository.existsByTokenAndRevokedFalse(token);
    }

    // Sauvegarde un token JWT en base de données
    private void saveJwtToken(String token, String email, Long userId, java.util.Date expiresAt) {
        LocalDateTime expiresAtLocal = expiresAt.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        JwtToken jwtToken = new JwtToken(token, email, userId, expiresAtLocal);
        jwtTokenRepository.save(jwtToken);
        logger.debug("[SERVICE-AUTH] JWT token saved to database for user: {}", email);
    }

    // Révoque tous les tokens d'un utilisateur (logout de toutes les sessions)
    public void logoutAllSessions(Long userId) {
        int revoked = jwtTokenRepository.revokeAllUserTokens(userId);
        logger.info("[SERVICE-AUTH] Revoked {} tokens for user ID: {}", revoked, userId);
    }

    // Récupère un utilisateur par son token d'authentification (JWT).
    public User getUserByToken(String token) {
        try {
            String email = jwtUtil.extractUsername(token);
            User user = userService.findByEmail(email);
            if (user == null) {
                logger.warn("[SERVICE-AUTH] User not found for email: {}", email);
                throw new RuntimeException("User not found");
            }
            logger.info("[SERVICE-AUTH] User info retrieved for: {}", user.getEmail());
            return user;
        } catch (Exception e) {
            logger.warn("[SERVICE-AUTH] Invalid or expired token");
            throw new RuntimeException("Invalid or expired token");
        }
    }

    // Méthode utilitaire pour obtenir une permission existante ou la créer si elle n'existe pas
    private Permission getOrCreatePermission(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    logger.debug("[SERVICE-AUTH] Creating permission: {}", name);
                    Permission newPermission = new Permission(name);
                    newPermission.setDescription(description);
                    return permissionRepository.save(newPermission);
                });
    }

    // Supprime le compte de l'utilisateur (auto-résiliation)
    public void deleteAccount(String token) {
        User user = getUserByToken(token);
        
        // Vérifier que l'utilisateur a la permission DELETE_ACCOUNT
        if (!userService.hasPermission(user, "DELETE_ACCOUNT") && !userService.hasPermission(user, "ADMIN")) {
            throw new RuntimeException("Permission denied: DELETE_ACCOUNT required");
        }

        // Révoquer tous les tokens JWT de l'utilisateur
        jwtTokenRepository.revokeAllUserTokens(user.getId());
        
        // Supprimer les tokens de vérification
        tokenRepository.deleteByUserId(user.getId());
        
        // Supprimer l'utilisateur
        userRepository.delete(user);
        
        logger.info("[SERVICE-AUTH] Account deleted for user ID: {}", user.getId());
    }

    // Vérifie un token de vérification d'email et active l'utilisateur si le token est valide.
    public void verifyEmail(String tokenId, String tokenClear) {
        VerificationToken token = tokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        if (!passwordEncoder.matches(tokenClear, token.getTokenHash())) {
            throw new RuntimeException("Invalid token signature");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Idempotent: if already verified, just clean up the token
        if (user.isVerified()) {
            tokenRepository.delete(token);
            logger.info("[SERVICE-AUTH] User {} already verified (idempotent)", user.getId());
            return;
        }

        user.setVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);

        logger.info("[SERVICE-AUTH] Email verified successfully for user ID: {}", user.getId());
    }
}
