package com.softwarearchi.archi.models;

// Types de tokens utilisés dans l'application
public enum TokenType {
    ACCESS,            // Token d'accès API (courte durée : 15-30 min)
    REFRESH,           // Renouvellement du token d'accès (longue durée : 7-30 jours)
    PASSWORD_RESET,    // Token pour reset le mot de passe (courte durée : 1-2 heures)
    EMAIL_VERIFICATION // Token pour vérifier l'email (1-24 heures)
}
