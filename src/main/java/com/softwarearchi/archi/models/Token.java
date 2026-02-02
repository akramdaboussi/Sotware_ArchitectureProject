package com.softwarearchi.archi.models;

import java.time.LocalDateTime;

public class Token {

    private Long id;
    private String token; // Valeur du token
    private TokenType tokenType; // Type de token : ACCESS, REFRESH, etc.
    private boolean revoked = false; // Indique si le token a été révoqué
    private boolean expired = false; // Indique si le token a expiré
    private LocalDateTime createdAt; // Date de création du token
    private LocalDateTime expiresAt; // Date d'expiration du token
    private Long userId; // Propriétaire du token

    // Constructeurs
    public Token() {}

    // Création d'un token avec les attributs essentiels
    public Token(String token, TokenType tokenType, Long userId, LocalDateTime expiresAt) {
        this.token = token;
        this.tokenType = tokenType;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    // Vérifie si le token est valide (non révoqué et non expiré)
    public boolean isValid() {
        return !revoked && !expired && expiresAt.isAfter(LocalDateTime.now());
    }

    /** @return Identifiant unique du token */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** @return Valeur du token */ 
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    /** @return Type de token : ACCESS, REFRESH, etc. */ 
    public TokenType getTokenType() { return tokenType; }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    /** @return Indique si le token a été révoqué */ 
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    /** @return Indique si le token a expiré */ 
    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    /** @return Date de création du token */
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return Date d'expiration du token */
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /** @return ID du propriétaire du token */
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
