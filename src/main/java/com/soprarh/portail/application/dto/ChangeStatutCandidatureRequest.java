package com.soprarh.portail.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour modifier le statut d'une candidature.
 * US-CAND-03: Le RH change le statut (soumise, en_evaluation, acceptee, rejetee).
 */
public record ChangeStatutCandidatureRequest(

        @NotBlank(message = "Le statut est obligatoire")
        String statut
) {}

