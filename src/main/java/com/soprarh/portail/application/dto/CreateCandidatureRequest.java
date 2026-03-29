package com.soprarh.portail.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO pour deposer une candidature.
 * US-CAND-01: Le candidat soumet sa candidature pour une offre.
 * Le candidat_id est extrait du token JWT, pas du body.
 */
public record CreateCandidatureRequest(

        @NotNull(message = "L'ID de l'offre est obligatoire")
        UUID offreId,

        /**
         * Lettre de motivation (optionnelle).
         */
        String lettreMotivation
) {}

