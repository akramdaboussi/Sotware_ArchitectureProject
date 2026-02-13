package com.softwarearchi.archi.models;

import jakarta.persistence.*;

/**
 * Role entity for RBAC (Role-Based Access Control).
 * Defines user permissions: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR.
 */
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Role name (e.g., ROLE_ADMIN) - must be unique */
    @Column(unique = true, nullable = false)
    private String name;
    
    /** Optional role description */
    private String description;

    /** Default constructor for JPA */
    public Role() {}

    /** Create role with name only */
    public Role(String name) {
        this.name = name;
    }

    /** Create role with all attributes */
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
