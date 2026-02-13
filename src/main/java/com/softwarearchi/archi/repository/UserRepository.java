package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.User;

/** JPA repository for User entity CRUD operations */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /** Find user by email (used for login) */
    Optional<User> findByEmail(String email);
    /** Check if email is already registered */
    boolean existsByEmail(String email);
}