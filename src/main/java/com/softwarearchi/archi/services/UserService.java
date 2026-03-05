package com.softwarearchi.archi.services;

import com.softwarearchi.archi.models.Role;
import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.RoleRepository;
import com.softwarearchi.archi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

/**
 * Service de gestion des utilisateurs.
 * Gère la création d'utilisateurs, la récupération, le hachage des mots de passe et la gestion des rôles.
 */
@Service
public class UserService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName())).toList())
                .accountLocked(!user.isEnabled())
                .build();
    }
    //TODO: supprimer le système de rôles, remplacer par le système d'autorités (permissions)
    //intégré nativement dans org.springframework.security.core.userdetails
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    // Crée et sauvegarde un nouvel utilisateur (le mot de passe est haché avant la sauvegarde)
    public User createUser(User user) {
        logger.info("[SERVICE-USER] Creating user: {}", user.getEmail());

        // Hash the password before saving
        user.setPassword(hashPassword(user.getPassword()));
        User savedUser = userRepository.save(user);
        logger.info("[SERVICE-USER] User saved with ID: {}", savedUser.getId());
        return savedUser;
    }

    // Trouve un utilisateur par email. Retourne null si non trouvé.
    public User findByEmail(String email) {
        logger.debug("[SERVICE-USER] Finding user by email: {}", email);
        return userRepository.findByEmail(email).orElse(null);
    }

    // Trouve un utilisateur par ID. Retourne null si non trouvé.
    public User findById(Long id) {
        logger.debug("[SERVICE-USER] Finding user by ID: {}", id);
        return userRepository.findById(id).orElse(null);  
    }

    // Vérifie si un utilisateur existe avec l'email donné.
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Récupère tous les utilisateurs de la base de données.
    public List<User> findAllUsers() {
        logger.debug("[SERVICE-USER] Finding all users");
        return userRepository.findAll();
    }

    // --- Gestion des rôles ---

    /**
     * Ajoute un rôle à un utilisateur par email et nom de rôle.
     * Lève une exception si l'utilisateur ou le rôle n'est pas trouvé.
     */
    public void addRoleToUser(String email, String roleName) {
        logger.info("[SERVICE-USER] Adding role {} to user {}", roleName, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        logger.info("[SERVICE-USER] Role {} added to user {}", roleName, email);
    }

    /**
     * Supprime un rôle d'un utilisateur par email et nom de rôle.
     * Lève une exception si l'utilisateur n'est pas trouvé.
     */
    public void removeRoleFromUser(String email, String roleName) {
        logger.info("[SERVICE-USER] Removing role {} from user {}", roleName, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.getRoles().removeIf(role -> role.getName().equals(roleName));
        userRepository.save(user);

        logger.info("[SERVICE-USER] Role {} removed from user {}", roleName, email);
    }

    // Vérifie si un utilisateur a un rôle spécifique.
    public boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    // Vérifie si un utilisateur a le rôle administrateur.
    public boolean isAdmin(User user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    // Hache un mot de passe en utilisant SHA-256 et l'encodage Base64.
    public String hashPassword(String password) {
        // Hachage du mot de passe (SHA-256)
        logger.debug("[SERVICE-USER] Hashing password (SHA-256)");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Vérifie si un mot de passe en clair correspond au hash stocké.
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        logger.debug("[SERVICE-USER] Verifying password");
        String hashedInput = hashPassword(rawPassword);
        boolean matches = hashedInput.equals(hashedPassword);
        if (matches) {
            logger.debug("[SERVICE-USER] Password verification successful");
        } else {
            logger.debug("[SERVICE-USER] Password verification failed");
        }
        return matches;
    }
}
