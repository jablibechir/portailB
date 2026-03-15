package com.soprarh.portail.shared;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions.
 * Intercepte toutes les exceptions et retourne une ApiResponse structuree.
 *
 * Ordre de priorite : du plus specifique au plus generique.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erreurs de validation @Valid sur les DTOs (champs manquants, format incorrect).
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * Erreurs de validation sur les parametres de methode (@RequestParam, @PathVariable).
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Exceptions metier explicitement lancees par les services.
     * Le status HTTP est porte par l'exception elle-meme.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Mauvais credentials (email/mot de passe incorrects).
     * HTTP 401 Unauthorized.
     * Bonne pratique : message generique, ne pas indiquer si c'est l'email ou le mot de passe.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email ou mot de passe incorrect"));
    }

    /**
     * Compte desactive (etat != actif).
     * HTTP 403 Forbidden.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Compte inactif. Veuillez verifier votre email pour activer votre compte."));
    }

    /**
     * Compte verrouille (etat == suspendu).
     * HTTP 403 Forbidden.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Compte suspendu. Contactez l'administrateur."));
    }

    /**
     * Acces refuse — token valide mais permissions insuffisantes.
     * HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Acces refuse. Permissions insuffisantes."));
    }

    /**
     * Catch-all — toute exception non capturee par les handlers precedents.
     * HTTP 500 Internal Server Error.
     * Bonne pratique : ne pas exposer les details techniques au client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne est survenue."));
    }
}

