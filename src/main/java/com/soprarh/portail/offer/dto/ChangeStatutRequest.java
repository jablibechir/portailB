package com.soprarh.portail.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO pour changer le statut d'une offre.
 * US-OFF-04: Changement de statut (Brouillon/Publie/Archive).
 */
public record ChangeStatutRequest(

        @NotBlank(message = "Le statut est obligatoire")
        @Pattern(regexp = "^(brouillon|publiee|archivee)$",
                 message = "Statut invalide. Valeurs acceptees: brouillon, publiee, archivee")
        String statut
) {}

