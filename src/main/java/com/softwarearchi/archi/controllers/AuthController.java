package com.softwarearchi.archi.controllers;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour les endpoints d'authentification.
 * Gère les requêtes HTTP et délègue la logique métier à AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Autorise toutes les origines pour les tests
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Inscrit un nouvel utilisateur
     * Corps de la requête : { firstName, lastName, email, password, phoneNumber }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        logger.info("[CONTROLLER] Registration request received");

        try {
            // Extraction des champs de la requête
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");
            String email = request.get("email");
            String password = request.get("password");
            String phoneNumber = request.get("phoneNumber");

            logger.debug("[CONTROLLER] Registration data: email={}, firstName={}, lastName={}",
                    email, firstName, lastName);

            // Validation des champs requis
            if (firstName == null || lastName == null || email == null || password == null) {
                logger.warn("[CONTROLLER] Registration failed: Missing required fields for email={}", email);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Missing required fields"));
            }

            // Appel du service pour l'inscription
            String token = authService.register(firstName, lastName, email, password, phoneNumber);

            logger.info("[CONTROLLER] Registration successful");

            // Retour de la réponse avec le token et les infos de l'utilisateur
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", email);
            response.put("firstName", firstName);
            response.put("message", "Registration successful");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            logger.error("[CONTROLLER] Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * Connexion avec email et mot de passe
     * Corps de la requête : { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        logger.info("[CONTROLLER] Login request received");

        try {
            String email = request.get("email");
            String password = request.get("password");

            logger.debug("[CONTROLLER] Login attempt for email={}", email);

            // Validate
            if (email == null || password == null) {
                logger.warn("[CONTROLLER] Login failed: Missing email or password");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email and password are required"));
            }

            // Authentification
            String token = authService.login(email, password);

            logger.info("[CONTROLLER] Login successful");

            // Retour du token
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", email);
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER] Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout
     * Déconnexion de l'utilisateur (JWT : simplement supprimer le token côté client)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("[CONTROLLER] Logout request received");

        try {
            String token = extractToken(authHeader);
            logger.debug("[CONTROLLER] Token extracted for logout");

            authService.logout(token);
            logger.info("[CONTROLLER] Logout successful");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/auth/me
     * Obtenir l'utilisateur actuellement authentifié
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        logger.info("[CONTROLLER] Get current user request received");

        try {
            String token = extractToken(authHeader);
            logger.debug("[CONTROLLER] Token extracted, fetching user info");

            User user = authService.getUserByToken(token);
            logger.info("[CONTROLLER] User info retrieved: email={}", user.getEmail());

            // Création de la réponse utilisateur (exclut les champs sensibles comme le mot de passe)
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("enabled", user.isEnabled());
            response.put("verified", user.isVerified());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/auth/verify
     * Vérifier l'email de l'utilisateur
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestParam("tokenId") String tokenId,
            @RequestParam("t") String tokenClear) {

        logger.info("[CONTROLLER] Email verification request received for token");

        try {
            authService.verifyEmail(tokenId, tokenClear);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email successfully verified!");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER] Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // Utilitaire : Extraire le token de l'en-tête Authorization
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        return authHeader.substring(7); // Supprime le préfixe "Bearer "
    }

    // Utilitaire : Créer une réponse d'erreur
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", LocalDateTime.now().toString());
        return error;
    }
}
