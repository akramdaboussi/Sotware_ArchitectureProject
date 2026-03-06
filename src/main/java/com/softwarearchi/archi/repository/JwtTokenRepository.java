package com.softwarearchi.archi.repository;

import com.softwarearchi.archi.models.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la gestion des tokens JWT en base de données.
 * Permet de sauvegarder, rechercher et révoquer les tokens.
 */
@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    // Recherche un token par sa valeur
    Optional<JwtToken> findByToken(String token);

    // Recherche tous les tokens valides d'un utilisateur
    List<JwtToken> findByUserIdAndRevokedFalse(Long userId);

    // Recherche tous les tokens d'un email
    List<JwtToken> findByEmail(String email);

    // Vérifie si un token existe et est valide (non révoqué)
    boolean existsByTokenAndRevokedFalse(String token);

    // Révoque tous les tokens d'un utilisateur (logout de toutes les sessions)
    @Modifying
    @Transactional
    @Query("UPDATE JwtToken t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllUserTokens(Long userId);

    // Révoque un token spécifique
    @Modifying
    @Transactional
    @Query("UPDATE JwtToken t SET t.revoked = true WHERE t.token = :token")
    int revokeToken(String token);

    // Supprime les tokens expirés (nettoyage périodique)
    @Modifying
    @Transactional
    @Query("DELETE FROM JwtToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(LocalDateTime now);

    // Compte le nombre de sessions actives pour un utilisateur
    long countByUserIdAndRevokedFalse(Long userId);
}
