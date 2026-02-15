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

    public AuthService(UserService userService, UserRepository userRepository, RoleRepository roleRepository, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user in the system.
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
        // Generate JWT for the new user
        logger.info("[SERVICE-AUTH] Generating new JWT for user ID: {}", user.getId());
        String token = jwtUtil.generateToken(
            user.getEmail(),
            Map.of(
                "userId", user.getId(),
                "roles", user.getRoles().stream().map(Role::getName).toArray()
            )
        );
        java.util.Date expiresAt = jwtUtil.extractExpiration(token);
        logger.info("[SERVICE-AUTH] JWT generated, expires at: {}", expiresAt);
        return token;
    }

    /**
     * Authenticates a user with provided credentials.
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
                "roles", user.getRoles().stream().map(Role::getName).toArray()
            )
        );
        // Login successful log should be in AuthController
        return token;
    }

    /**
     * Logs out the user. (For JWT, nothing to do server-side; just remove token client-side)
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
}
