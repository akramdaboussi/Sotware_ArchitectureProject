package com.softwarearchi.archi.models;

import jakarta.persistence.*;

/**
 * Entité Permission pour le contrôle d'accès granulaire.
 * Remplace le système de rôles par des permissions fines.
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom de la permission (ex: READ_USERS, MANAGE_USERS)
    @Column(unique = true, nullable = false)
    private String name;

    // Description optionnelle de la permission
    private String description;

    // Constructeur par défaut pour JPA
    public Permission() {}

    // Créer une permission avec le nom uniquement
    public Permission(String name) {
        this.name = name;
    }

    // Créer une permission avec tous les attributs
    public Permission(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
