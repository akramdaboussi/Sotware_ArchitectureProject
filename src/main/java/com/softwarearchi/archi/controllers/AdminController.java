package com.softwarearchi.archi.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.services.AuthService;
import com.softwarearchi.archi.services.UserService;

/**
 * Contrôleur REST pour les opérations réservées aux administrateurs.
 * Chaque endpoint vérifie la permission appropriée (ou ADMIN).
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final AuthService authService;

    public AdminController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // Vérifie qu'un utilisateur possède une permission spécifique (ou ADMIN)
    private User verifyPermission(String authHeader, String requiredPermission) {
        String token = extractToken(authHeader);
        User user = authService.getUserByToken(token);
        if (!userService.hasPermission(user, requiredPermission) && !userService.isAdmin(user)) {
            throw new RuntimeException("Access denied: " + requiredPermission + " permission required");
        }
        return user;
    }

    // Extrait le token du format d'en-tête "Bearer <token>". 
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        return authHeader.substring(7);
    }

    /**
     * GET /api/admin/users - Liste tous les utilisateurs avec leurs permissions.
     * Nécessite : READ_USERS ou ADMIN
     */
    @GetMapping("/users")
    public ResponseEntity<Object> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        logger.info("[CONTROLLER-ADMIN] Get all users request");

        try {
            verifyPermission(authHeader, "READ_USERS");

            List<User> users = userService.findAllUsers();
            
            // Conversion des utilisateurs en DTOs (exclut les champs sensibles comme le mot de passe)
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("email", user.getEmail());
                userMap.put("firstName", user.getFirstName());
                userMap.put("lastName", user.getLastName());
                userMap.put("enabled", user.isEnabled());
                userMap.put("permissions", user.getPermissions().stream()
                        .map(permission -> permission.getName())
                        .collect(Collectors.toList()));
                return userMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(userList);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/add-permission - Ajoute une permission à un utilisateur.
     * Nécessite : MANAGE_PERMISSIONS ou ADMIN
     */
    @PostMapping("/add-permission")
    public ResponseEntity<Map<String, Object>> addPermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        logger.info("[CONTROLLER-ADMIN] Add permission request");

        try {
            verifyPermission(authHeader, "MANAGE_PERMISSIONS");

            String email = request.get("email");
            String permissionName = request.get("permission");

            if (email == null || permissionName == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email and permission are required"));
            }

            userService.addPermissionToUser(email, permissionName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permission " + permissionName + " added to " + email);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/remove-permission - Supprime une permission d'un utilisateur.
     * Nécessite : MANAGE_PERMISSIONS ou ADMIN
     */
    @PostMapping("/remove-permission")
    public ResponseEntity<Map<String, Object>> removePermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        logger.info("[CONTROLLER-ADMIN] Remove permission request");

        try {
            verifyPermission(authHeader, "MANAGE_PERMISSIONS");

            String email = request.get("email");
            String permissionName = request.get("permission");

            if (email == null || permissionName == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email and permission are required"));
            }

            userService.removePermissionFromUser(email, permissionName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permission " + permissionName + " removed from " + email);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/users/{id} - Supprime un utilisateur.
     * Nécessite : DELETE_USERS ou ADMIN
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        logger.info("[CONTROLLER-ADMIN] Delete user request for ID: {}", id);

        try {
            verifyPermission(authHeader, "DELETE_USERS");

            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            userService.deleteUser(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User " + user.getEmail() + " deleted successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}