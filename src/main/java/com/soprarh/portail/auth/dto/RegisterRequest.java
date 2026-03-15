package com.soprarh.portail.auth.dto;

import com.soprarh.portail.user.entity.TypeUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requete pour l'inscription d'un nouvel utilisateur.
 * Le mot de passe est recu ici mais ne sera JAMAIS retourne dans une reponse.
 *
 * Utilise record Java 16+ pour l'immutabilite et la concision.
 */
public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne peut pas depasser 100 caracteres")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(max = 100, message = "Le prenom ne peut pas depasser 100 caracteres")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format email invalide")
        @Size(max = 255, message = "L'email ne peut pas depasser 255 caracteres")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
        String motDePasse,

        @NotNull(message = "Le type utilisateur est obligatoire")
        TypeUtilisateur typeUtilisateur
) {}

