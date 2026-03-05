package com.softwarearchi.archi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.softwarearchi.archi.models.Role;
import com.softwarearchi.archi.repository.RoleRepository;

/**
 * Initialise la base de données avec les rôles par défaut au démarrage de l'application.
 * Implémente CommandLineRunner pour s'exécuter après le chargement du contexte Spring.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    // Crée les rôles par défaut : USER, ADMIN, MODERATOR. 
    @Override
    public void run(String... args) {
        logger.info("[INIT] Initializing default roles...");

        createRoleIfNotExists("ROLE_USER", "Standard user");
        createRoleIfNotExists("ROLE_ADMIN", "Administrator with full access");
        createRoleIfNotExists("ROLE_MODERATOR", "Content moderator");

        logger.info("[INIT] Default roles initialized successfully");
    }

    // Crée un rôle seulement s'il n'existe pas (évite les doublons). 
    private void createRoleIfNotExists(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role(name);
            role.setDescription(description);
            roleRepository.save(role);
            logger.info("[INIT] Created role: {}", name);
        } else {
            logger.debug("[INIT] Role already exists: {}", name);
        }
    }
}