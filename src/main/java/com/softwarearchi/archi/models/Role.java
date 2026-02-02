package com.softwarearchi.archi.models;

public class Role {

    private Long id;
    private String name; // ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
    private String description; // Description du rôle

    // Constructeurs
    public Role() {}

    // Création d'un rôle avec seulement le nom
    public Role(String name) {
        this.name = name;
    }

    // Création d'un rôle avec tous les attributs
    public Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /** @return Identifiant du rôle */ 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** @return Nom du rôle */ 
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** @return Description du rôle */ 
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
