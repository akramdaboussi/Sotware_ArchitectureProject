package com.softwarearchi.archi.services;

import com.softwarearchi.archi.models.Token;
import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for token generation and validation.
 * Creates custom tokens without external JWT libraries.
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private final TokenRepository tokenRepository;
    private final SecureRandom random = new SecureRandom();

    // Token expiration time in hours
    private static final int ACCESS_TOKEN_EXPIRY_HOURS = 24;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Generate a new access token for a user
     * Format: userId:timestamp:randomString (Base64 encoded)
     */
    public String generateToken(User user) {
        logger.info("[SERVICE-TOKEN] Generating new token for user ID: {}", user.getId());

        // Create token payload
        String payload = user.getId() + ":" +
                System.currentTimeMillis() + ":" +
                generateRandomString(32);

        // Encode in Base64
        String tokenValue = Base64.getEncoder()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        // Create Token entity
        Token token = new Token(
                tokenValue,
                user.getId(),
                LocalDateTime.now().plusHours(ACCESS_TOKEN_EXPIRY_HOURS));

        // Save token
        tokenRepository.save(token);

        logger.info("[SERVICE-TOKEN] Token generated and saved, expires at: {}", token.getExpiresAt());
        return tokenValue;
    }

    /**
     * Validate token and check if it's still valid
     */
    public boolean validateToken(String tokenValue) {
        logger.debug("[SERVICE-TOKEN] Validating token");
        Token token = tokenRepository.findByToken(tokenValue).orElse(null);
        boolean isValid = token != null && token.isValid();
        logger.debug("[SERVICE-TOKEN] Token validation result: {}", isValid);
        return isValid;
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String tokenValue) {
        logger.debug("[SERVICE-TOKEN] Extracting user ID from token");
        Token token = tokenRepository.findByToken(tokenValue).orElse(null);
        if (token != null && token.isValid()) {
            logger.debug("[SERVICE-TOKEN] User ID extracted: {}", token.getUserId());
            return token.getUserId();
        }
        logger.warn("[SERVICE-TOKEN] Invalid or expired token");
        return null;
    }

    /**
     * Revoke a token (logout)
     */
    @Transactional
    public void revokeToken(String tokenValue) {
        logger.info("[SERVICE-TOKEN] Revoking token");
        Token token = tokenRepository.findByToken(tokenValue).orElse(null);
        if (token != null) {
            token.setRevoked(true);
            tokenRepository.save(token);
            logger.info("[SERVICE-TOKEN] Token revoked successfully");
        } else {
            logger.warn("[SERVICE-TOKEN] Attempted to revoke non-existent token");
        }
    }

    @Transactional
    public void deleteTokensByUserId(Long userId) {
        tokenRepository.deleteByUserId(userId);  
    }

    /**
     * Generate random string for token uniqueness
     */
    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, length);
    }
}
