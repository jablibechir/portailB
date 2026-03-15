package com.soprarh.portail.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requete pour la connexion.
 * Contient uniquement les credentials — ne jamais logger ces valeurs.
 */
public record LoginRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String motDePasse
) {}

