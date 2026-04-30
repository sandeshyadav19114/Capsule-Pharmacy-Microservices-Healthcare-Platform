package com.pharmacy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for Pharmacy Service.
 *
 * RBAC Rules:
 *   - GET /api/medicines/**          → PUBLIC (anyone can browse catalog)
 *   - POST/PUT/DELETE /api/medicines → ADMIN only
 *   - /api/prescriptions/**          → PATIENT or ADMIN
 *   - /api/cart/**                   → PATIENT only
 *   - /api/orders/**                 → PATIENT or ADMIN
 *   - /api/admin/**                  → ADMIN only
 *   - Swagger, Actuator health       → PUBLIC
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health", "/actuator/info"
                ).permitAll()

                // Medicine catalog — GET is public, mutations are admin-only
                .requestMatchers(HttpMethod.GET, "/api/medicines/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/medicines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/medicines/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/medicines/**").hasRole("ADMIN")

                // Prescription upload/view — patient or admin
                .requestMatchers("/api/prescriptions/**").hasAnyRole("PATIENT", "ADMIN")

                // Cart — patient only
                .requestMatchers("/api/cart/**").hasRole("PATIENT")

                // Orders — patient (own orders) or admin (all orders)
                .requestMatchers("/api/orders/**").hasAnyRole("PATIENT", "ADMIN")

                // Admin panel
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
