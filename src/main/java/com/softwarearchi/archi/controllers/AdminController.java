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
 * REST controller for admin-only operations.
 * All endpoints require ROLE_ADMIN - returns 403 otherwise.
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

    /** Validates token and checks ROLE_ADMIN. Throws RuntimeException if unauthorized. */
    private User verifyAdmin(String authHeader) {
        String token = extractToken(authHeader);
        User user = authService.getUserByToken(token);
        
        if (!userService.isAdmin(user)) {
            throw new RuntimeException("Access denied: Admin role required");
        }
        
        return user;
    }

    /** Extracts token from "Bearer <token>" header format. */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        return authHeader.substring(7);
    }

    /**
     * GET /api/admin/users - List all users with their roles.
     * Returns: [{id, email, firstName, lastName, enabled, roles}]
     */
    @GetMapping("/users")
    public ResponseEntity<Object> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        logger.info("[CONTROLLER-ADMIN] Get all users request");

        try {
            verifyAdmin(authHeader);

            List<User> users = userService.findAllUsers();
            
            // Map users to DTOs (exclude sensitive fields like password)
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("email", user.getEmail());
                userMap.put("firstName", user.getFirstName());
                userMap.put("lastName", user.getLastName());
                userMap.put("enabled", user.isEnabled());
                userMap.put("roles", user.getRoles().stream()
                        .map(role -> role.getName())
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
     * POST /api/admin/add-role - Add a role to a user.
     * Body: {email, role} - e.g., {"email": "user@example.com", "role": "ROLE_MODERATOR"}
     */
    @PostMapping("/add-role")
    public ResponseEntity<Map<String, Object>> addRole(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        logger.info("[CONTROLLER-ADMIN] Add role request");

        try {
            verifyAdmin(authHeader);

            String email = request.get("email");
            String roleName = request.get("role");

            if (email == null || roleName == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email and role are required"));
            }

            userService.addRoleToUser(email, roleName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role " + roleName + " added to " + email);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/remove-role - Remove a role from a user.
     * Body: {email, role} - e.g., {"email": "user@example.com", "role": "ROLE_MODERATOR"}
     */
    @PostMapping("/remove-role")
    public ResponseEntity<Map<String, Object>> removeRole(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        logger.info("[CONTROLLER-ADMIN] Remove role request");

        try {
            verifyAdmin(authHeader);

            String email = request.get("email");
            String roleName = request.get("role");

            if (email == null || roleName == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email and role are required"));
            }

            userService.removeRoleFromUser(email, roleName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role " + roleName + " removed from " + email);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("[CONTROLLER-ADMIN] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}