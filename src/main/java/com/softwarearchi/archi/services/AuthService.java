package com.softwarearchi.archi.services;

import com.softwarearchi.archi.utils.JwtUtil;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.UserRepository;
import com.softwarearchi.archi.models.Role;
import com.softwarearchi.archi.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import com.softwarearchi.archi.models.VerificationToken;
import com.softwarearchi.archi.repository.VerificationTokenRepository;
import com.softwarearchi.archi.events.UserRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service d'authentification : gère la connexion, l'inscription et la déconnexion.
 * C'est ici que réside la logique métier principale d'authentification.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final VerificationTokenRepository tokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.mq.exchange:auth.events}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered:auth.user-registered}")
    private String userRegisteredRoutingKey;

    public AuthService(UserService userService, UserRepository userRepository, RoleRepository roleRepository,
            JwtUtil jwtUtil, VerificationTokenRepository tokenRepository, RabbitTemplate rabbitTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.tokenRepository = tokenRepository;
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

        Role userRole = getOrCreateRole("ROLE_USER", "Standard user");

        // Création d'une nouvelle entité utilisateur
        logger.info("[SERVICE-USER] Creating user: {}", email);
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(userService.hashPassword(password)); // Hashage du mot de passe avant de le stocker
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

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

        // Publication de l'événement
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId, user.getId(), user.getEmail(), tokenId, LocalDateTime.now());

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
                        "roles", user.getRoles().stream().map(Role::getName).toArray()));
        java.util.Date expiresAt = jwtUtil.extractExpiration(token);
        logger.info("[SERVICE-AUTH] JWT generated, expires at: {}", expiresAt);
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
                        "roles", user.getRoles().stream().map(Role::getName).toArray()));
        // Le log de succès de connexion devrait être dans AuthController
        return token;
    }

    // Déconnecte l'utilisateur 
    public void logout(String token) {
        logger.info("[SERVICE-AUTH] Logout: nothing to do server-side with JWT");
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

    // Méthode utilitaire pour obtenir un rôle existant ou le créer s'il n'existe pas
    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    logger.debug("[SERVICE-AUTH] Creating role: {}", name);
                    Role newRole = new Role(name);
                    newRole.setDescription(description);
                    return roleRepository.save(newRole);
                });
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
