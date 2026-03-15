package com.soprarh.portail.auth.security;

import com.soprarh.portail.user.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du JwtProvider.
 * Pas de Spring context — instanciation directe avec une cle de test.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private Utilisateur utilisateur;

    // Cle de test >= 32 chars (256 bits pour HS256)
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-chars!!";
    private static final long ACCESS_EXPIRY  = 900_000L;   // 15 min
    private static final long REFRESH_EXPIRY = 604_800_000L; // 7 jours

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(TEST_SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);

        Permission perm = Permission.builder()
                .id(UUID.randomUUID())
                .code("VIEW_OFFERS")
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .nom("CANDIDAT")
                .permissions(Set.of(perm))
                .build();

        utilisateur = Utilisateur.builder()
                .id(UUID.randomUUID())
                .nom("Jean Dupont")
                .email("jean@test.com")
                .motDePasse("$2a$12$hash")
                .typeUtilisateur(TypeUtilisateur.candidat)
                .etat(EtatUtilisateur.actif)
                .roles(Set.of(role))
                .build();
    }

    @Test
    @DisplayName("Access token genere contient le bon email en subject")
    void generateAccessToken_containsCorrectSubject() {
        String token = jwtProvider.generateAccessToken(utilisateur);

        assertThat(jwtProvider.extractEmail(token)).isEqualTo("jean@test.com");
    }

    @Test
    @DisplayName("Access token est de type 'access'")
    void generateAccessToken_typeIsAccess() {
        String token = jwtProvider.generateAccessToken(utilisateur);

        assertThat(jwtProvider.extractTokenType(token)).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh token est de type 'refresh'")
    void generateRefreshToken_typeIsRefresh() {
        String token = jwtProvider.generateRefreshToken(utilisateur);

        assertThat(jwtProvider.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid retourne true pour un token valide")
    void isTokenValid_validToken() {
        String token = jwtProvider.generateAccessToken(utilisateur);

        assertThat(jwtProvider.isTokenValid(token, utilisateur)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid retourne false pour un token avec mauvais subject")
    void isTokenValid_wrongSubject() {
        String token = jwtProvider.generateAccessToken(utilisateur);

        Utilisateur autreUser = Utilisateur.builder()
                .id(UUID.randomUUID())
                .nom("Autre")
                .email("autre@test.com")
                .motDePasse("hash")
                .typeUtilisateur(TypeUtilisateur.manager)
                .etat(EtatUtilisateur.actif)
                .roles(Set.of())
                .build();

        assertThat(jwtProvider.isTokenValid(token, autreUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid retourne false pour un token expire")
    void isTokenValid_expiredToken() {
        // Token avec expiration de -1ms (deja expire)
        JwtProvider expiredProvider = new JwtProvider(TEST_SECRET, -1L, -1L);
        String token = expiredProvider.generateAccessToken(utilisateur);

        assertThat(jwtProvider.isTokenValid(token, utilisateur)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid(String) retourne false pour token expire")
    void isTokenValidString_expiredToken() {
        JwtProvider expiredProvider = new JwtProvider(TEST_SECRET, -1L, -1L);
        String token = expiredProvider.generateRefreshToken(utilisateur);

        assertThat(jwtProvider.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Token signe avec une autre cle est rejete")
    void isTokenValid_wrongSignature() {
        JwtProvider otherProvider = new JwtProvider(
                "another-secret-key-totally-different-32chars!!", ACCESS_EXPIRY, REFRESH_EXPIRY
        );
        String tokenFromOther = otherProvider.generateAccessToken(utilisateur);

        assertThat(jwtProvider.isTokenValid(tokenFromOther, utilisateur)).isFalse();
    }

    @Test
    @DisplayName("getAccessTokenExpirationMs retourne la valeur configuree")
    void getAccessTokenExpirationMs_returnsConfiguredValue() {
        assertThat(jwtProvider.getAccessTokenExpirationMs()).isEqualTo(ACCESS_EXPIRY);
    }

    @Test
    @DisplayName("Deux tokens generes successivement ont des jti differents")
    void generateTokens_uniqueJti() {
        String token1 = jwtProvider.generateAccessToken(utilisateur);
        String token2 = jwtProvider.generateAccessToken(utilisateur);

        // Les tokens ne doivent pas etre identiques (jti = UUID unique)
        assertThat(token1).isNotEqualTo(token2);
    }
}

