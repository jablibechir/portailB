package com.soprarh.portail.auth.service;

import com.soprarh.portail.auth.dto.*;
import com.soprarh.portail.auth.security.JwtProvider;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.entity.*;
import com.soprarh.portail.user.repository.RoleRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du AuthService.
 * Toutes les dependances sont mockees — aucune DB, aucun contexte Spring.
 *
 * Structure Nested : un bloc par methode du service.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // =========================================================
    // Fixtures
    // =========================================================

    private Role roleCandidat;
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        Permission perm = Permission.builder()
                .id(UUID.randomUUID())
                .code("VIEW_OFFERS")
                .build();

        roleCandidat = Role.builder()
                .id(UUID.randomUUID())
                .nom("CANDIDAT")
                .permissions(Set.of(perm))
                .build();

        utilisateur = Utilisateur.builder()
                .id(UUID.randomUUID())
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .motDePasse("$2a$12$hashedpassword")
                .typeUtilisateur(TypeUtilisateur.candidat)
                .etat(EtatUtilisateur.actif)
                .roles(Set.of(roleCandidat))
                .build();
    }

    // =========================================================
    // register()
    // =========================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Succes : nouvel utilisateur cree et profil retourne")
        void register_success() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123", TypeUtilisateur.candidat
            );

            when(utilisateurRepository.existsByEmail("jean@test.com")).thenReturn(false);
            when(roleRepository.findByNom("CANDIDAT")).thenReturn(Optional.of(roleCandidat));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
            when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

            UserProfileResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("jean@test.com");
            assertThat(response.nom()).isEqualTo("Dupont");
            assertThat(response.prenom()).isEqualTo("Jean");
            assertThat(response.roles()).contains("CANDIDAT");

            verify(passwordEncoder).encode("password123");
            verify(utilisateurRepository).save(any(Utilisateur.class));
        }

        @Test
        @DisplayName("Echec : email deja utilise -> 409 CONFLICT")
        void register_emailAlreadyExists() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123", TypeUtilisateur.candidat
            );

            when(utilisateurRepository.existsByEmail("jean@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });

            // S'assurer que rien n'est sauvegarde
            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("Echec : role introuvable en base -> 500")
        void register_roleNotFound() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123", TypeUtilisateur.candidat
            );

            when(utilisateurRepository.existsByEmail("jean@test.com")).thenReturn(false);
            when(roleRepository.findByNom("CANDIDAT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Le mot de passe n'est jamais retourne dans la reponse")
        void register_passwordNeverExposed() {
            RegisterRequest request = new RegisterRequest(
                    "Dupont", "Jean", "jean@test.com", "password123", TypeUtilisateur.candidat
            );

            when(utilisateurRepository.existsByEmail(any())).thenReturn(false);
            when(roleRepository.findByNom(any())).thenReturn(Optional.of(roleCandidat));
            when(passwordEncoder.encode(any())).thenReturn("$2a$12$hash");
            when(utilisateurRepository.save(any())).thenReturn(utilisateur);

            UserProfileResponse response = authService.register(request);

            // UserProfileResponse ne contient pas de champ motDePasse
            // On verifie que la reponse est bien un record sans getter password
            assertThat(response).isNotNull();
            assertThat(response.getClass().getDeclaredFields())
                    .extracting("name")
                    .doesNotContain("motDePasse", "password", "mot_de_passe");
        }
    }

    // =========================================================
    // login()
    // =========================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Succes : tokens emis apres authentification")
        void login_success() {
            LoginRequest request = new LoginRequest("jean@test.com", "password123");

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(userDetailsService.loadUserByUsername("jean@test.com")).thenReturn(utilisateur);
            when(jwtProvider.generateAccessToken(utilisateur)).thenReturn("access-token-mock");
            when(jwtProvider.generateRefreshToken(utilisateur)).thenReturn("refresh-token-mock");
            when(jwtProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token-mock");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-mock");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900000L);
        }

        @Test
        @DisplayName("Echec : mauvais credentials -> BadCredentialsException propagee")
        void login_badCredentials() {
            LoginRequest request = new LoginRequest("jean@test.com", "wrongpassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    // =========================================================
    // refresh()
    // =========================================================

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("Succes : nouvel access token emis")
        void refresh_success() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

            when(jwtProvider.isTokenValid("valid-refresh-token")).thenReturn(true);
            when(jwtProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
            when(jwtProvider.extractEmail("valid-refresh-token")).thenReturn("jean@test.com");
            when(userDetailsService.loadUserByUsername("jean@test.com")).thenReturn(utilisateur);
            when(jwtProvider.generateAccessToken(utilisateur)).thenReturn("new-access-token");
            when(jwtProvider.generateRefreshToken(utilisateur)).thenReturn("new-refresh-token");
            when(jwtProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

            AuthResponse response = authService.refresh(request);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("Echec : refresh token expire -> 401")
        void refresh_expiredToken() {
            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

            when(jwtProvider.isTokenValid("expired-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        assertThat(((BusinessException) ex).getStatus())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("Echec : access token utilise comme refresh token -> 401")
        void refresh_accessTokenUsedAsRefresh() {
            RefreshTokenRequest request = new RefreshTokenRequest("access-token-used-as-refresh");

            when(jwtProvider.isTokenValid("access-token-used-as-refresh")).thenReturn(true);
            when(jwtProvider.isRefreshToken("access-token-used-as-refresh")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        assertThat(((BusinessException) ex).getStatus())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }
    }

    // =========================================================
    // getProfile()
    // =========================================================

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("Succes : profil retourne sans mot de passe")
        void getProfile_success() {
            when(utilisateurRepository.findByEmail("jean@test.com"))
                    .thenReturn(Optional.of(utilisateur));

            UserProfileResponse response = authService.getProfile("jean@test.com");

            assertThat(response.email()).isEqualTo("jean@test.com");
            assertThat(response.nom()).isEqualTo("Dupont");
            assertThat(response.prenom()).isEqualTo("Jean");
            assertThat(response.etat()).isEqualTo(EtatUtilisateur.actif);
        }

        @Test
        @DisplayName("Echec : utilisateur introuvable -> 404")
        void getProfile_notFound() {
            when(utilisateurRepository.findByEmail("unknown@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile("unknown@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        assertThat(((BusinessException) ex).getStatus())
                                .isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}

