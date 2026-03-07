package com.softwarearchi.archi.controllers;

import com.softwarearchi.archi.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Nginx auth_request validation endpoint.
 * Called as an internal subrequest by Nginx before proxying to Service A or B.
 * Returns 200 if JWT is valid, 401 otherwise.
 */
@RestController
@CrossOrigin(origins = "*")
public class ValidateController {

    private static final Logger logger = LoggerFactory.getLogger(ValidateController.class);
    private final JwtUtil jwtUtil;

    public ValidateController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * GET /validate
     * Called by Nginx auth_request subrequest.
     * Validates the Bearer JWT from the Authorization header.
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Target-Service", required = false) String targetService) {

        logger.info("[VALIDATE] Nginx auth_request for service={}", targetService);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("[VALIDATE] Missing or invalid Authorization header");
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractUsername(token);
            if (email == null || !jwtUtil.validateToken(token, email)) {
                logger.warn("[VALIDATE] Invalid or expired token for service={}", targetService);
                return ResponseEntity.status(401).build();
            }
            logger.info("[VALIDATE] Token valid for user={}, service={}", email, targetService);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("[VALIDATE] Token validation error: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * GET /validate_email
     * Public endpoint exposed via Nginx (no auth needed).
     * Can be used for public email confirmation redirects.
     */
    @GetMapping("/validate_email")
    public ResponseEntity<String> validateEmail() {
        return ResponseEntity.ok("Email validation endpoint");
    }
}
