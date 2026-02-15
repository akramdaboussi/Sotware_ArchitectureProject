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
 * Utility class for generating, validating, and parsing JWT tokens.
 * Handles signing, claims extraction, and expiration checks.
 */
@Component
public class JwtUtil {

    // Secret key for signing JWTs
    private final SecretKey key;
    // Token validity duration (24 hours)
    private final long jwtExpirationMs = 86400000;


    /**
     * Initialize JwtUtil with the secret key from configuration.
     * @param secret Base64-encoded secret for HS256
     */
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
    }


    /**
     * Generate a signed JWT token for a user.
     * @param subject Username or user identifier
     * @param claims  Additional claims to include
     * @return Signed JWT as String
     */
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


    /**
     * Validate a JWT token for a given username.
     * @param token    JWT token
     * @param username Expected username (subject)
     * @return true if valid and not expired
     */
    public boolean validateToken(String token, String username) {
        try {
            final String subject = extractUsername(token);
            return (subject.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Extract the username (subject) from a JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    /**
     * Extract the expiration date from a JWT token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    /**
     * Extract a specific claim from a JWT token.
     * @param token JWT token
     * @param claimsResolver Function to extract claim
     * @return Extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    // Parse and return all claims from a JWT token
    private Claims extractAllClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(key).build();
        Jws<Claims> jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }


    // Check if the JWT token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
