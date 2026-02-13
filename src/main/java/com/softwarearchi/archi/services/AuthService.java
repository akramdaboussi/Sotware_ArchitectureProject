package com.softwarearchi.archi.services;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.UserRepository;
import com.softwarearchi.archi.models.Role;
import com.softwarearchi.archi.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Authentication service - handles login, registration, and logout.
 * This is where the core authentication business logic lives.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthService(UserService userService, TokenService tokenService, UserRepository userRepository, RoleRepository roleRepository) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Register a new user
     * 
     * @return Generated authentication token
     */
    public String register(String firstName, String lastName, String email,
            String password, String phoneNumber) {
        logger.info("[SERVICE-AUTH] Starting registration for email: {}", email);

        // Check if user already exists
        logger.debug("[SERVICE-AUTH] Checking if email exists: {}", email);
        if (userService.existsByEmail(email)) {
            logger.warn("[SERVICE-AUTH] Registration failed: Email {} already exists", email);
            throw new RuntimeException("User with email " + email + " already exists");
        }

        Role userRole = getOrCreateRole("ROLE_USER", "Standard user");

        // Create new user
        logger.debug("[SERVICE-AUTH] Creating new user object");
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

        // Save user (une seule fois, avec le rôle)
        userRepository.save(user);
        logger.info("[SERVICE-AUTH] User created with ID: {}", user.getId());
        logger.info("[SERVICE-AUTH] Welcome aboard, {}!", firstName);
        
        // Generate and return token
        String token = tokenService.generateToken(user);
        logger.info("[SERVICE-AUTH] Registration complete for email: {}", email);
        return token;
    }

    /**
     * Login user with credentials
     * 
     * @return Authentication token if successful
     */
    public String login(String email, String password) {
        logger.info("[SERVICE-AUTH] Login attempt for email: {}", email);

        // Find user
        logger.debug("[SERVICE-AUTH] Looking up user by email");
        User user = userService.findByEmail(email);
        if (user == null) {
            logger.warn("[SERVICE-AUTH] Login failed: User not found for email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }
        logger.debug("[SERVICE-AUTH] User found: ID={}", user.getId());

        // Verify password
        logger.debug("[SERVICE-AUTH] Verifying password");
        if (!userService.verifyPassword(password, user.getPassword())) {
            logger.warn("[SERVICE-AUTH] Login failed: Invalid password for email: {}", email);
            throw new RuntimeException("Invalid email or password");
        }
        logger.debug("[SERVICE-AUTH] Password verified successfully");

        // Check if user is enabled
        if (!user.isEnabled()) {
            logger.warn("[SERVICE-AUTH] Login failed: Account disabled for email: {}", email);
            throw new RuntimeException("Account is disabled");
        }

        // Generate and return token
        logger.info("[SERVICE-AUTH] Generating authentication token");
        String token = tokenService.generateToken(user);
        logger.info("[SERVICE-AUTH] Login successful for email: {}", email);
        return token;
    }

    /**
     * Logout user by revoking token
     */
    public void logout(String token) {
        logger.info("[SERVICE-AUTH] Processing logout request");
        tokenService.revokeToken(token);
        logger.info("[SERVICE-AUTH] Logout successful");
    }

    /**
     * Get user by authentication token
     */
    public User getUserByToken(String token) {
        Long userId = tokenService.getUserIdFromToken(token);
        if (userId == null) {
            logger.warn("[SERVICE-AUTH] Invalid or expired token");
            throw new RuntimeException("Invalid or expired token");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("[SERVICE-AUTH] User not found for ID: {}", userId);
            throw new RuntimeException("User not found");
        }

        logger.info("[SERVICE-AUTH] User info retrieved for: {}", user.getEmail());
        return user;
    }
    
    // Helper method to get existing role or create it if it doesn't exist
    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    logger.debug("[SERVICE-AUTH] Creating role: {}", name);
                    Role newRole = new Role(name);
                    newRole.setDescription(description);
                    return roleRepository.save(newRole);
                });
    }
}
