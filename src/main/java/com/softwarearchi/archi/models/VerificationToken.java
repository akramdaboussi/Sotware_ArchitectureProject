package com.softwarearchi.archi.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Token de vérification d'email.
 * Sécurité : seul le hash du token est stocké, jamais le token en clair.
 * Le tokenId (public) est dans l'URL, le tokenClear (secret) est envoyé par email.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant public (dans l'URL) */
    @Column(nullable = false, unique = true)
    private String tokenId;

    /** Hash BCrypt du token secret */
    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Long userId;

    /** Expiration (15 min par défaut) */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public VerificationToken() {
    }

    public VerificationToken(String tokenId, String tokenHash, Long userId, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    // Getters (pas de setter pour tokenHash pour éviter les modifications après création)
    public Long getId() {
        return id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
