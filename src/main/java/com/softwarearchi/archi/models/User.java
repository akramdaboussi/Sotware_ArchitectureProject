package com.softwarearchi.archi.models;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.*;

/**
 * Entité utilisateur pour le système d'authentification.
 * Stocke les identifiants et les permissions pour le contrôle d'accès granulaire.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // Numéro de téléphone optionnel 
    @Column(name = "phone_number")
    private String phoneNumber;

    // Email unique utilisé pour la connexion 
    @Column(unique = true, nullable = false)
    private String email;

    // Mot de passe haché en SHA-256 
    @Column(nullable = false)
    private String password;

    // False = compte désactivé 
    @Column(nullable = false)
    private boolean enabled;

    // False = email non vérifié 
    @Column(nullable = false)
    private boolean verified = false;

    // Permissions de l'utilisateur (READ_USERS, MANAGE_USERS, ADMIN, etc.)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeur par défaut pour JPA 
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Créer un utilisateur avec tous les attributs 
    public User(String firstName, String lastName, String phoneNumber, String email, String password, boolean enabled,
            Set<Permission> permissions) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.permissions = permissions;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
