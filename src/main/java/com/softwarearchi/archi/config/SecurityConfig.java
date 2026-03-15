package com.softwarearchi.archi.config;

import com.softwarearchi.archi.utils.JwtUtil;
import com.softwarearchi.archi.services.UserService;
import com.softwarearchi.archi.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Classe de configuration Spring Security.
 * Configure la sécurité HTTP, le filtre d'authentification JWT et les règles
 * d'accès aux endpoints.
 * Le filtre vérifie également que le token existe en base de données (non
 * révoqué).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    @Lazy // Évite la dépendance circulaire
    private AuthService authService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/styles.css",
                                "/app.js",
                                "/static/**",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/validate",
                                "/favicon.ico",
                                "/validate_email",
                                "/h2-console/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtUtil, userService, authService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Filtre JWT avec vérification en base de données
    public static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtUtil jwtUtil;
        private final UserService userService;
        private final AuthService authService;

        public JwtAuthFilter(JwtUtil jwtUtil, UserService userService, AuthService authService) {
            this.jwtUtil = jwtUtil;
            this.userService = userService;
            this.authService = authService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain)
                throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email = jwtUtil.extractUsername(token);
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Vérification JWT classique + vérification en base de données
                        if (jwtUtil.validateToken(token, email) && authService.isTokenValid(token)) {
                            UserDetails userDetails = userService.loadUserByUsername(email);
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
