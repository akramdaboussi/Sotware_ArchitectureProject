package com.softwarearchi.archi.models;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Authentication token entity.
 * Stores Base64-encoded tokens with 24h expiration.
 */
@Entity
@Table(name = "tokens")
public class Token {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Base64-encoded token value */
    @Column(unique = true, nullable = false, length = 500)
    private String token;
    
    /** Token type (currently only ACCESS) */
    @Column(name = "token_type", nullable = false)
    private String tokenType = "ACCESS";
    
    /** True if token was manually revoked (logout) */
    @Column(nullable = false)
    private boolean revoked = false;
    
    /** True if token has expired */
    @Column(nullable = false)
    private boolean expired = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /** Owner of this token */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Default constructor for JPA */
    public Token() {}

    /** Create token with essential attributes */
    public Token(String token, Long userId, LocalDateTime expiresAt) {
        this.token = token;
        this.tokenType = "ACCESS";
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /** @return true if token is not revoked, not expired, and within validity period */
    public boolean isValid() {
        return !revoked && !expired && expiresAt.isAfter(LocalDateTime.now());
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
