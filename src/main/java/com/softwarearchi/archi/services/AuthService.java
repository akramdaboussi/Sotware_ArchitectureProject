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
 * Authentication service - handles login, registration, and logout.
 * This is where the core authentication business logic lives.
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
     * Registers a new user in the system.
     * 
     * @return The generated authentication JWT token.
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

        // Create a new user entity
        logger.info("[SERVICE-USER] Creating user: {}", email);
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(userService.hashPassword(password)); // Will be hashed in UserService
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Save the user with assigned role(s)
        userRepository.save(user);
        logger.info("[SERVICE-USER] User saved with ID: {}", user.getId());

        // Generate Verification Token
        String eventId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        String tokenClear = UUID.randomUUID().toString(); // The secret in the email
        String tokenHash = passwordEncoder.encode(tokenClear);

        VerificationToken verificationToken = new VerificationToken(
                tokenId, tokenHash, user.getId(), LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(verificationToken);

        // Publish Event
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId, user.getId(), user.getEmail(), tokenId, LocalDateTime.now());
        // Temporary hack to pass tokenClear to the Notification service for the email
        // link
        // In a real app, maybe publish a different event or just append it to the token
        // id if safe
        // Or better yet, change the NotificationService to generate the clear token?
        // No, Auth owns it.
        // We'll just pass tokenClear in this single-node simplified setup by adding it
        // to the event locally.
        // But our event doesn't have tokenClear. Let's add it or pass it.
        // The TP says: "tokenClear is included only in the simplest version of the TP".
        // We will just add it as a new property or recreate the event. Let's just
        // create an inline custom map to publish.
        Map<String, Object> eventData = Map.of(
                "eventId", eventId,
                "userId", user.getId(),
                "email", user.getEmail(),
                "tokenId", tokenId,
                "tokenClear", tokenClear,
                "occurredAt", LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, eventData);
        logger.info("[SERVICE-AUTH] Published UserRegisteredEvent for email: {}", email);
        // Generate JWT for the new user
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
     * Authenticates a user with provided credentials.
     * 
     * @return The authentication JWT token if successful.
     */
    public String login(String email, String password) {
        // Login attempt is logged in AuthController
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
        // Login successful log should be in AuthController
        return token;
    }

    /**
     * Logs out the user. (For JWT, nothing to do server-side; just remove token
     * client-side)
     */
    public void logout(String token) {
        logger.info("[SERVICE-AUTH] Logout: nothing to do server-side with JWT");
    }

    /**
     * Retrieves a user by authentication token (JWT).
     */
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

    // Helper method to get an existing role or create it if it does not exist
    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    logger.debug("[SERVICE-AUTH] Creating role: {}", name);
                    Role newRole = new Role(name);
                    newRole.setDescription(description);
                    return roleRepository.save(newRole);
                });
    }

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
