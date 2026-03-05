package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.User;

// Repository JPA pour les opérations CRUD sur l'entité User 
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Trouve un utilisateur par email (utilisé pour la connexion) 
    Optional<User> findByEmail(String email);
    // Vérifie si un email est déjà enregistré 
    boolean existsByEmail(String email);
}