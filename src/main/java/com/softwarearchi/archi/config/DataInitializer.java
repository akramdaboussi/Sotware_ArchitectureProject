package com.softwarearchi.archi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.softwarearchi.archi.models.Role;
import com.softwarearchi.archi.repository.RoleRepository;

/**
 * Seeds the database with default roles at application startup.
 * Implements CommandLineRunner to execute after Spring context loads.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Creates default roles for RBAC: USER, ADMIN, MODERATOR.
     * Idempotent - safe to run multiple times.
     */
    @Override
    public void run(String... args) {
        logger.info("[INIT] Initializing default roles...");

        createRoleIfNotExists("ROLE_USER", "Standard user");
        createRoleIfNotExists("ROLE_ADMIN", "Administrator with full access");
        createRoleIfNotExists("ROLE_MODERATOR", "Content moderator");

        logger.info("[INIT] Default roles initialized successfully");
    }

    /** Creates role only if it doesn't exist (prevents duplicates). */
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