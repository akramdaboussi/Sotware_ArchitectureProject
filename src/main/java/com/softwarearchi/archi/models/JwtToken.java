package com.softwarearchi.archi.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un token JWT stocké en base de données.
 * Permet la révocation des tokens (vrai logout côté serveur) et le suivi des sessions actives.
 */
@Entity
@Table(name = "jwt_tokens")
public class JwtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le token JWT complet (ou son hash pour plus de sécurité)
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    // Email de l'utilisateur associé
    @Column(nullable = false)
    private String email;

    // ID de l'utilisateur associé
    @Column(nullable = false)
    private Long userId;

    // Date de création du token
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Date d'expiration du token
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Indique si le token a été révoqué (logout)
    @Column(nullable = false)
    private boolean revoked = false;

    // Constructeur par défaut requis par JPA
    public JwtToken() {
    }

    // Constructeur avec paramètres
    public JwtToken(String token, String email, Long userId, LocalDateTime expiresAt) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    // Vérifie si le token est valide (non révoqué et non expiré)
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }
}
