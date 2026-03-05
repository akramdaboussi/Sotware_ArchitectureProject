package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.Role;

// Repository JPA pour les opérations CRUD sur l'entité Role
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Trouve un rôle par son nom (ex: ROLE_ADMIN)
    Optional<Role> findByName(String name);
}