package com.softwarearchi.archi.services;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.storage.InMemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for user management operations.
 * Handles user creation, retrieval, and password hashing.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final InMemoryStorage storage;

    public UserService(InMemoryStorage storage) {
        this.storage = storage;
    }

    /**
     * Create and save a new user
     */
    public User createUser(User user) {
        logger.info("[SERVICE-USER] Creating user: {}", user.getEmail());

        // Hash the password before saving
        logger.debug("[SERVICE-USER] Hashing password for user: {}", user.getEmail());
        user.setPassword(hashPassword(user.getPassword()));

        storage.saveUser(user);
        logger.info("[SERVICE-USER] ✅ User saved with ID: {}", user.getId());
        return user;
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        return storage.findUserByEmail(email);
    }

    /**
     * Check if email already exists
     */
    public boolean existsByEmail(String email) {
        return storage.existsByEmail(email);
    }

    /**
     * Hash password using SHA-256
     */
    public String hashPassword(String password) {
        logger.debug("[SERVICE-USER] Hashing password (SHA-256)");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("[SERVICE-USER] ❌ Error hashing password: {}", e.getMessage());
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verify password matches the stored hash
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        logger.debug("[SERVICE-USER] Verifying password");
        String hashedInput = hashPassword(rawPassword);
        boolean matches = hashedInput.equals(hashedPassword);
        if (matches) {
            logger.debug("[SERVICE-USER] ✅ Password verification successful");
        } else {
            logger.debug("[SERVICE-USER] ❌ Password verification failed");
        }
        return matches;
    }
}
