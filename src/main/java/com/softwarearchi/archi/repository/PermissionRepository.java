package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.Permission;

/**
 * Repository JPA pour les opérations CRUD sur l'entité Permission.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // Trouve une permission par son nom (ex: READ_USERS)
    Optional<Permission> findByName(String name);
}
