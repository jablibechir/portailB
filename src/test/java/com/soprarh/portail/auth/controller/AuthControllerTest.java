package com.soprarh.portail.auth.controller;

import com.soprarh.portail.auth.dto.*;
import com.soprarh.portail.auth.service.AuthService;
import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.entity.EtatUtilisateur;
import com.soprarh.portail.user.entity.TypeUtilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du AuthController.
 *
 * Approche : Mockito pur, sans Spring context.
 * On appelle directement les methodes du controller et on verifie les ResponseEntity.
 * C'est l'approche la plus simple et la plus rapide pour tester un controller.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserProfileResponse profileResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        profileResponse = new UserProfileResponse(
                UUID.randomUUID(),
                "Dupont",
                "Jean",
                "jean@test.com",
                EtatUtilisateur.actif,
                TypeUtilisateur.candidat,
                LocalDateTime.now(),
                Set.of("CANDIDAT")
        );

        authResponse = AuthResponse.of("access-token", "refresh-token", 900000L);
    }

    // =========================================================
    // POST /api/auth/register
    // =========================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Retourne 201 CREATED avec le profil")
        void register_returns201() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123");
            when(authService.register(any())).thenReturn(profileResponse);

            ResponseEntity<ApiResponse<UserProfileResponse>> response =
                    authController.register(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().data().email()).isEqualTo("jean@test.com");
            assertThat(response.getBody().data().roles()).contains("CANDIDAT");
        }

        @Test
        @DisplayName("Appelle authService.register() exactement une fois")
        void register_callsService() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123");
            when(authService.register(any())).thenReturn(profileResponse);

            authController.register(request);

            verify(authService, times(1)).register(request);
        }
    }

    // =========================================================
    // POST /api/auth/login
    // =========================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Retourne 200 OK avec les tokens")
        void login_returns200() {
            LoginRequest request = new LoginRequest("jean@test.com", "password123");
            when(authService.login(any())).thenReturn(authResponse);

            ResponseEntity<ApiResponse<AuthResponse>> response =
                    authController.login(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().data().accessToken()).isEqualTo("access-token");
            assertThat(response.getBody().data().refreshToken()).isEqualTo("refresh-token");
            assertThat(response.getBody().data().tokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().data().expiresIn()).isEqualTo(900000L);
        }
    }

    // =========================================================
    // POST /api/auth/refresh
    // =========================================================

    @Nested
    @DisplayName("refresh()")
    class RefreshToken {

        @Test
        @DisplayName("Retourne 200 OK avec les nouveaux tokens")
        void refresh_returns200() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            when(authService.refresh(any())).thenReturn(authResponse);

            ResponseEntity<ApiResponse<AuthResponse>> response =
                    authController.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().accessToken()).isEqualTo("access-token");
        }
    }

    // =========================================================
    // GET /api/auth/me
    // =========================================================

    @Nested
    @DisplayName("me()")
    class Me {

        @Test
        @DisplayName("Retourne 200 OK avec le profil de l'utilisateur connecte")
        void me_returns200() {
            // Mock de UserDetails (injecte par @AuthenticationPrincipal)
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn("jean@test.com");
            when(authService.getProfile("jean@test.com")).thenReturn(profileResponse);

            ResponseEntity<ApiResponse<UserProfileResponse>> response =
                    authController.me(userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().email()).isEqualTo("jean@test.com");
            assertThat(response.getBody().data().nom()).isEqualTo("Dupont");
        }
    }
}
