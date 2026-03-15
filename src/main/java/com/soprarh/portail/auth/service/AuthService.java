package com.soprarh.portail.auth.service;

import com.soprarh.portail.auth.dto.*;
import com.soprarh.portail.auth.security.JwtProvider;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.entity.EtatUtilisateur;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.RoleRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service d'authentification.
 *
 * Responsabilites :
 *   - register  : inscription, hash du mot de passe, assignation du role, envoi email verification
 *   - verify    : activation du compte via code de verification
 *   - login     : verification credentials via AuthenticationManager, emission des tokens
 *   - refresh   : validation du refresh token, emission d'un nouvel access token
 *   - me        : retour du profil de l'utilisateur connecte
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * Etapes :
     * 1. Verifier l'unicite de l'email
     * 2. Hasher le mot de passe avec BCrypt
     * 3. Assigner le role correspondant au typeUtilisateur
     * 4. Creer le compte avec etat = inactif et un code de verification
     * 5. Envoyer l'email de verification
     * 6. Retourner le profil (sans mot de passe, sans token)
     */
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        // 1. Unicite de l'email
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Un compte existe deja avec cet email.",
                    HttpStatus.CONFLICT
            );
        }

        // 2. Recherche du role correspondant au type
        String nomRole = request.typeUtilisateur().name().toUpperCase();
        var role = roleRepository.findByNom(nomRole)
                .orElseThrow(() -> new BusinessException(
                        "Role non trouve en base : " + nomRole,
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        // 3. Generer un code de verification unique (UUID)
        String verificationCode = UUID.randomUUID().toString();

        // 4. Construction de l'entite — etat inactif jusqu'a verification email
        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .motDePasse(passwordEncoder.encode(request.motDePasse()))
                .typeUtilisateur(request.typeUtilisateur())
                .etat(EtatUtilisateur.inactif)
                .codeVerification(verificationCode)
                .codeExpiration(LocalDateTime.now().plusHours(24))
                .roles(Set.of(role))
                .build();

        // 5. Persistance
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("Nouvel utilisateur enregistre (inactif) : id={}, type={}", saved.getId(), saved.getTypeUtilisateur());

        // 6. Envoi de l'email de verification
        emailService.sendVerificationEmail(
                saved.getEmail(),
                saved.getPrenom(),
                verificationCode,
                baseUrl
        );

        return toProfileResponse(saved);
    }

    /**
     * Verification du compte via le code envoye par email.
     *
     * @param code code de verification unique
     * @return message de confirmation
     * @throws BusinessException 400 si le code est invalide ou expire
     */
    @Transactional
    public String verifyEmail(String code) {
        Utilisateur utilisateur = utilisateurRepository.findByCodeVerification(code)
                .orElseThrow(() -> new BusinessException(
                        "Code de verification invalide.", HttpStatus.BAD_REQUEST
                ));

        // Verifier que le code n'est pas expire
        if (utilisateur.getCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "Le code de verification a expire. Veuillez vous reinscrire.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Activer le compte
        utilisateur.setEtat(EtatUtilisateur.actif);
        utilisateur.setCodeVerification(null);
        utilisateur.setCodeExpiration(null);
        utilisateurRepository.save(utilisateur);

        log.info("Compte active avec succes : id={}", utilisateur.getId());
        return "Compte active avec succes. Vous pouvez maintenant vous connecter.";
    }

    /**
     * Connexion d'un utilisateur existant.
     * Spring Security verifie que isEnabled() retourne true (etat == actif).
     * Un compte inactif (non verifie) sera rejete avec DisabledException.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.motDePasse())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken  = jwtProvider.generateAccessToken(userDetails);
        String refreshToken = jwtProvider.generateRefreshToken(userDetails);

        log.info("Connexion reussie pour : {}", request.email());

        return AuthResponse.of(accessToken, refreshToken, jwtProvider.getAccessTokenExpirationMs());
    }

    /**
     * Renouvellement de l'access token via refresh token (stateless).
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtProvider.isTokenValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException("Refresh token invalide ou expire.", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtProvider.extractEmail(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        String newAccessToken  = jwtProvider.generateAccessToken(userDetails);
        String newRefreshToken = jwtProvider.generateRefreshToken(userDetails);

        return AuthResponse.of(newAccessToken, newRefreshToken, jwtProvider.getAccessTokenExpirationMs());
    }

    /**
     * Retourne le profil de l'utilisateur actuellement authentifie.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve.", HttpStatus.NOT_FOUND
                ));
        return toProfileResponse(utilisateur);
    }

    // =========================================================
    // Methodes privees
    // =========================================================

    private UserProfileResponse toProfileResponse(Utilisateur u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(r -> r.getNom())
                .collect(Collectors.toSet());

        return new UserProfileResponse(
                u.getId(),
                u.getNom(),
                u.getPrenom(),
                u.getEmail(),
                u.getEtat(),
                u.getTypeUtilisateur(),
                u.getDateCreation(),
                roleNames
        );
    }
}

