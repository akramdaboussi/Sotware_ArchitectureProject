package com.softwarearchi.archi.events;

import java.time.LocalDateTime;

/**
 * Événement publié lors d'une inscription utilisateur.
 * Envoyé via RabbitMQ au NotificationService pour déclencher l'email de vérification.
 */
public class UserRegisteredEvent {
    private String eventId;      // UUID pour traçabilité
    private Long userId;         // ID de l'utilisateur inscrit
    private String email;        // Destination du mail
    private String tokenId;      // Token de vérification associé
    private LocalDateTime occurredAt;  // Horodatage

    // Constructeur vide pour désérialisation JSON (important pour RabbitMQ)
    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(String eventId, Long userId, String email, String tokenId, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.email = email;
        this.tokenId = tokenId;
        this.occurredAt = occurredAt;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
