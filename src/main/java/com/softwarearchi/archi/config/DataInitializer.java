package com.softwarearchi.archi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.softwarearchi.archi.models.Permission;
import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.PermissionRepository;
import com.softwarearchi.archi.repository.UserRepository;
import com.softwarearchi.archi.services.UserService;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * Initialise la base de données avec les permissions par défaut et un super admin au démarrage.
 * Implémente CommandLineRunner pour s'exécuter après le chargement du contexte Spring.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // Identifiants du super admin (configurés via variables d'environnement ou application.properties)
    @Value("${app.admin.email:admin@admin.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    public DataInitializer(PermissionRepository permissionRepository, UserRepository userRepository, UserService userService) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // Crée les permissions par défaut au démarrage
    @Override
    public void run(String... args) {
        logger.info("[INIT] Initializing default permissions...");

        // Permissions de base (attribuées automatiquement à l'inscription)
        createPermissionIfNotExists("READ_PROFILE", "Lire son propre profil");
        createPermissionIfNotExists("EDIT_PROFILE", "Modifier son propre profil");
        createPermissionIfNotExists("DELETE_ACCOUNT", "Supprimer son propre compte");

        // Permissions de gestion (attribuées par un admin)
        createPermissionIfNotExists("READ_USERS", "Lire la liste des utilisateurs");
        createPermissionIfNotExists("MANAGE_USERS", "Modifier des utilisateurs");
        createPermissionIfNotExists("DELETE_USERS", "Supprimer des utilisateurs");
        createPermissionIfNotExists("MANAGE_PERMISSIONS", "Attribuer des permissions");

        // Permission administrateur (toutes les permissions)
        createPermissionIfNotExists("ADMIN", "Accès administrateur complet");

        logger.info("[INIT] Default permissions initialized successfully");

        // Création du super admin s'il n'existe pas
        createSuperAdminIfNotExists();
    }

    // Crée un super admin avec toutes les permissions s'il n'existe pas
    private void createSuperAdminIfNotExists() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            logger.info("[INIT] Creating super admin user...");

            User admin = new User();
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(userService.hashPassword(adminPassword));
            admin.setEnabled(true);
            admin.setVerified(true);

            // Attribuer TOUTES les permissions au super admin
            Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
            admin.setPermissions(allPermissions);

            userRepository.save(admin);
            logger.info("[INIT] Super admin created: {}", adminEmail);
            logger.warn("[INIT] ⚠️  CHANGEZ LE MOT DE PASSE ADMIN EN PRODUCTION !");
        } else {
            logger.debug("[INIT] Super admin already exists");
        }
    }

    // Crée une permission seulement si elle n'existe pas (évite les doublons)
    private void createPermissionIfNotExists(String name, String description) {
        if (permissionRepository.findByName(name).isEmpty()) {
            Permission permission = new Permission(name);
            permission.setDescription(description);
            permissionRepository.save(permission);
            logger.info("[INIT] Created permission: {}", name);
        } else {
            logger.debug("[INIT] Permission already exists: {}", name);
        }
    }
}