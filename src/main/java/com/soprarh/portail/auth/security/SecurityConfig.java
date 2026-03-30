package com.soprarh.portail.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security 6 pour l'application.
 *
 * Decisions d'architecture :
 *
 * 1. STATELESS : aucune session HTTP (JWT est auto-porteur).
 *    SessionCreationPolicy.STATELESS -> pas de HttpSession creee.
 *
 * 2. CSRF desactive : inutile en API REST stateless
 *    (CSRF exploite les sessions, pas les tokens Bearer).
 *
 * 3. DaoAuthenticationProvider : delegue a UserDetailsService + BCrypt.
 *    C'est ce provider qu'AuthenticationManager utilisera.
 *
 * 4. @EnableMethodSecurity : active @PreAuthorize sur les controllers.
 *    Exemple : @PreAuthorize("hasAuthority('MANAGE_USERS')")
 *
 * 5. JwtAuthFilter est insere AVANT UsernamePasswordAuthenticationFilter
 *    pour que le SecurityContext soit rempli avant le traitement.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Chaine de filtres de securite principale.
     *
     * Regles d'acces :
     * - /api/auth/**     : public (register, login, refresh)
     * - Tout le reste    : authentification requise
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactivation CSRF — API REST stateless
            .csrf(AbstractHttpConfigurer::disable)

            // Politique de session — STATELESS (JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Regles d'autorisation
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/profil/photo/**").permitAll()
                .anyRequest().authenticated()
            )

            // Provider d'authentification
            .authenticationProvider(authenticationProvider())

            // Filtre JWT insere avant le filtre de login Spring Security standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * PasswordEncoder — BCrypt avec strength 12.
     * Strength 12 = bon compromis securite/performance pour 2026.
     * Bonne pratique : ne jamais utiliser strength < 10 en production.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationProvider — utilise UserDetailsService + BCryptPasswordEncoder.
     * DaoAuthenticationProvider est le provider standard pour les DB.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — expose comme Bean pour injection dans AuthService.
     * Necessaire car AuthService.login() appelle authenticationManager.authenticate().
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

