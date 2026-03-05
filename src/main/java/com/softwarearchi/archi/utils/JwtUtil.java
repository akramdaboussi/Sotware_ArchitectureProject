package com.softwarearchi.archi.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;


/**
 * Classe utilitaire pour générer, valider et analyser les tokens JWT.
 * Gère la signature, l'extraction des claims et les vérifications d'expiration.
 */
@Component
public class JwtUtil {

    // Clé secrète pour signer les JWT
    private final SecretKey key;
    // Durée de validité du token (24 heures)
    private final long jwtExpirationMs = 86400000;


    // Initialise JwtUtil avec la clé secrète depuis la configuration. 
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
    }


    // Génère un token JWT signé pour un utilisateur. 
    public String generateToken(String subject, Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(key, Jwts.SIG.HS256);
        return builder.compact();
    }


    // Valide un token JWT pour un nom d'utilisateur donné. 
    public boolean validateToken(String token, String username) {
        try {
            final String subject = extractUsername(token);
            return (subject.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }


    // Extrait le nom d'utilisateur (subject) d'un token JWT. 
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    // Extrait la date d'expiration d'un token JWT. 
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    // Extrait un claim spécifique d'un token JWT.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    // Analyse et retourne tous les claims d'un token JWT
    private Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(key).build();
        Jws<Claims> jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }


    // Vérifie si le token JWT est expiré
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
