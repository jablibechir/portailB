package com.soprarh.portail.auth.controller;

import com.soprarh.portail.auth.dto.*;
import com.soprarh.portail.auth.service.AuthService;
import com.soprarh.portail.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur REST du module Auth.
 *
 * Endpoints :
 *   POST /api/auth/register  -> inscription        (public)
 *   GET  /api/auth/verify    -> activation compte   (public)
 *   POST /api/auth/login     -> connexion           (public)
 *   POST /api/auth/refresh   -> renouvellement token (public)
 *   GET  /api/auth/me        -> profil connecte     (protege)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Inscription d'un nouvel utilisateur.
     * Le compte est cree comme inactif. Un email de verification est envoye.
     * HTTP 201 Created + profil sans mot de passe.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserProfileResponse profile = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Compte cree. Verifiez votre email pour activer votre compte."));
    }

    /**
     * Activation du compte via le code envoye par email.
     * GET /api/auth/verify?code=xxx
     * HTTP 200 OK + message de confirmation.
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String code) {
        String message = authService.verifyEmail(code);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Connexion avec email + mot de passe.
     * Le compte doit etre actif (verifie par email) pour se connecter.
     * HTTP 200 OK + access token + refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Connexion reussie."));
    }

    /**
     * Renouvellement de l'access token via refresh token (stateless).
     * HTTP 200 OK + nouveaux tokens.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Token renouvele."));
    }

    /**
     * Retourne le profil de l'utilisateur actuellement authentifie.
     * @AuthenticationPrincipal injecte directement l'objet UserDetails
     * depuis le SecurityContext — pas de re-lecture du header JWT.
     * HTTP 200 OK + profil.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}

