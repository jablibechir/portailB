package com.soprarh.portail.user.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO pour modifier le profil utilisateur.
 * US-PROFIL-02: Modifier telephone et adresse.
 * Tous les champs sont optionnels.
 */
public record UpdateProfilRequest(

        @Size(max = 20, message = "Le telephone ne peut pas depasser 20 caracteres")
        String telephone,

        @Size(max = 255, message = "L'adresse ne peut pas depasser 255 caracteres")
        String adresse
) {}

