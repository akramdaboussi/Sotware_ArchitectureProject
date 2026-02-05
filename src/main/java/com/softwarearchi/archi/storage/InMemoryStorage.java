package com.softwarearchi.archi.storage;

import com.softwarearchi.archi.models.Token;
import com.softwarearchi.archi.models.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory storage for users and tokens.
 * Uses thread-safe ConcurrentHashMap for production-ready concurrent access.
 */
@Component
public class InMemoryStorage {

    // Store users by email (email is unique identifier)
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // Store tokens by token string
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

    // Auto-incrementing ID generator for users
    private final AtomicLong userIdGenerator = new AtomicLong(1);

    // User operations
    public void saveUser(User user) {
        if (user.getId() == null) {
            user.setId(userIdGenerator.getAndIncrement());
        }
        users.put(user.getEmail(), user);
    }

    public User findUserByEmail(String email) {
        return users.get(email);
    }

    public User findUserById(Long id) {
        return users.values().stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean existsByEmail(String email) {
        return users.containsKey(email);
    }

    public void deleteUser(String email) {
        users.remove(email);
    }

    // Token operations
    public void saveToken(Token token) {
        tokens.put(token.getToken(), token);
    }

    public Token findTokenByValue(String tokenValue) {
        return tokens.get(tokenValue);
    }

    public void deleteToken(String tokenValue) {
        tokens.remove(tokenValue);
    }

    public void deleteTokensByUserId(Long userId) {
        tokens.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
    }

    // Utility methods
    public int getUserCount() {
        return users.size();
    }

    public int getTokenCount() {
        return tokens.size();
    }
}
