package com.soprarh.portail.offer.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO pour modifier une offre d'emploi existante.
 * US-OFF-02: Modification du titre, description, competences.
 * Tous les champs sont optionnels - seuls les champs non-null seront mis a jour.
 */
public record UpdateOffreRequest(

        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String titre,

        String description,

        String competencesRequises,

        @Size(max = 100, message = "L'experience ne peut pas depasser 100 caracteres")
        String experienceRequise,

        @Size(max = 200, message = "La formation ne peut pas depasser 200 caracteres")
        String formationRequise
) {}

