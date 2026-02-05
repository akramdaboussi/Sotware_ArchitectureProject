package com.softwarearchi.archi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration to disable Spring Security's default behavior.
 * We're implementing our own authentication logic.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Allow all requests (we handle auth manually)
                );

        return http.build();
    }
}
