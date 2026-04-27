package com.soprarh.portail.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resume d'une offre pour le dashboard.
 */
public record OffreResumeDto(
        UUID id,
        String titre,
        String statut,
        String typeEmploi,
        LocalDate datePublication
) {}

