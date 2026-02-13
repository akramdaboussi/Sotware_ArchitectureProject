package com.softwarearchi.archi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softwarearchi.archi.models.Token;

/** JPA repository for Token entity CRUD operations */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    /** Find token by its Base64 value */
    Optional<Token> findByToken(String token);
    /** Delete all tokens for a specific user */
    void deleteByUserId(Long userId);
}