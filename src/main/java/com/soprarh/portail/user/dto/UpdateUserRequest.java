package com.soprarh.portail.user.dto;

import com.soprarh.portail.user.entity.EtatUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise a jour d'un utilisateur par le RH.
 */
public record UpdateUserRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100)
        String nom,

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(max = 100)
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit etre valide")
        @Size(max = 255)
        String email,

        @NotNull(message = "L'etat est obligatoire")
        EtatUtilisateur etat,

        @NotBlank(message = "Le role est obligatoire")
        String roleName
) {}
