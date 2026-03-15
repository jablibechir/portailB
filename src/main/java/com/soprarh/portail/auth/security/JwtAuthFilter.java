package com.soprarh.portail.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT execute une seule fois par requete (OncePerRequestFilter).
 *
 * Fonctionnement :
 * 1. Extrait le token du header Authorization: Bearer <token>
 * 2. Valide le token via JwtProvider
 * 3. Charge l'utilisateur depuis la DB via UserDetailsService
 * 4. Injecte l'authentification dans le SecurityContext
 *
 * Bonne pratique :
 * - Ne jamais logguer le token en clair.
 * - Si le token est absent ou invalide, on ne bloque pas la requete :
 *   on laisse Spring Security decider (certains endpoints sont publics).
 * - Les refresh tokens ne sont pas acceptes comme access tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Pas de header ou ne commence pas par "Bearer " -> on passe au filtre suivant
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // Refus des refresh tokens utilises comme access tokens
            if (jwtProvider.isRefreshToken(token)) {
                log.warn("Tentative d'utilisation d'un refresh token comme access token");
                filterChain.doFilter(request, response);
                return;
            }

            final String email = jwtProvider.extractEmail(token);

            // Si l'email est present et qu'aucune authentification n'est encore dans le contexte
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtProvider.isTokenValid(token, userDetails)) {
                    // Creation du token d'authentification Spring Security
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                           // credentials null (deja authentifie)
                                    userDetails.getAuthorities()    // permissions issues des roles
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Injection dans le SecurityContext de la requete courante
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token malformed, expire, signature invalide -> on ne bloque pas, Spring Security gere
            log.warn("Echec validation token JWT : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

