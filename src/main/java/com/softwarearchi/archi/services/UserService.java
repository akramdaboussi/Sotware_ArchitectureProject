package com.softwarearchi.archi.services;

import com.softwarearchi.archi.models.Permission;
import com.softwarearchi.archi.models.User;
import com.softwarearchi.archi.repository.PermissionRepository;
import com.softwarearchi.archi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;

/**
 * Service de gestion des utilisateurs.
 * Gère la création d'utilisateurs, la récupération, le hachage des mots de
 * passe et la gestion des permissions.
 */
@Service
public class UserService implements UserDetailsService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getPermissions().stream().map(p -> new SimpleGrantedAuthority(p.getName())).toList())
                .accountLocked(!user.isEnabled())
                .build();
    }

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public UserService(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    // Crée et sauvegarde un nouvel utilisateur (le mot de passe est haché avant la
    // sauvegarde)
    public User createUser(User user) {
        logger.info("[SERVICE-USER] Creating user: {}", user.getEmail());

        // Hachage du mot de passe avant sauvegarde
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

    // Supprime un utilisateur par son ID
    public void deleteUser(Long id) {
        logger.info("[SERVICE-USER] Deleting user with ID: {}", id);
        userRepository.deleteById(id);
        logger.info("[SERVICE-USER] User deleted successfully");
    }

    // --- Gestion des permissions ---

    /**
     * Ajoute une permission à un utilisateur par email et nom de permission.
     * Lève une exception si l'utilisateur ou la permission n'est pas trouvée.
     */
    public void addPermissionToUser(String email, String permissionName) {
        logger.info("[SERVICE-USER] Adding permission {} to user {}", permissionName, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionName));

        user.getPermissions().add(permission);

        
        userRepository.save(user);

        logger.info("[SERVICE-USER] Permission {} added to user {}", permissionName, email);
    }

    /**
     * Supprime une permission d'un utilisateur par email et nom de permission.
     * Lève une exception si l'utilisateur n'est pas trouvé.
     */
    public void removePermissionFromUser(String email, String permissionName) {
        logger.info("[SERVICE-USER] Removing permission {} from user {}", permissionName, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.getPermissions().removeIf(permission -> permission.getName().equals(permissionName));
        userRepository.save(user);

        logger.info("[SERVICE-USER] Permission {} removed from user {}", permissionName, email);
    }

    // Vérifie si un utilisateur a une permission spécifique.
    public boolean hasPermission(User user, String permissionName) {
        return user.getPermissions().stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    // Vérifie si un utilisateur a la permission administrateur.
    public boolean isAdmin(User user) {
        return hasPermission(user, "ADMIN");
    }

    // Hache un mot de passe en utilisant BCrypt.
    public String hashPassword(String password) {
        logger.debug("[SERVICE-USER] Hashing password (BCrypt)");
        return passwordEncoder.encode(password);
    }

    // Vérifie si un mot de passe en clair correspond au hash BCrypt stocké.
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        logger.debug("[SERVICE-USER] Verifying password");
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
        if (matches) {
            logger.debug("[SERVICE-USER] Password verification successful");
        } else {
            logger.debug("[SERVICE-USER] Password verification failed");
        }
        return matches;
    }
}
