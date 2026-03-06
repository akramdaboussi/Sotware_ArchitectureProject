package com.softwarearchi.archi.repository;

import com.softwarearchi.archi.models.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Repository pour les tokens de vérification d'email (CRUD + recherche par tokenId) */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    /** Recherche un token par son identifiant public (utilisé lors de la validation email) */
    Optional<VerificationToken> findByTokenId(String tokenId);

    /** Supprime tous les tokens de vérification d'un utilisateur (utilisé lors de la suppression de compte) */
    void deleteByUserId(Long userId);
}
