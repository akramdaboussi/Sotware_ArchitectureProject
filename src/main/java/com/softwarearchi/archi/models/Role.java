package com.softwarearchi.archi.models;

import jakarta.persistence.*;

// Définit les permissions utilisateur : ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR. 
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Nom du rôle (ex: ROLE_ADMIN) - doit être unique 
    @Column(unique = true, nullable = false)
    private String name;
    
    // Description optionnelle du rôle 
    private String description;

    // Constructeur par défaut pour JPA 
    public Role() {}

    // Créer un rôle avec le nom uniquement 
    public Role(String name) {
        this.name = name;
    }

    // Créer un rôle avec tous les attributs 
    public Role(Long id, String name, String description) {
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
