package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.Role;

/** JPA repository for Role entity CRUD operations */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    /** Find role by name (e.g., ROLE_ADMIN) */
    Optional<Role> findByName(String name);
}