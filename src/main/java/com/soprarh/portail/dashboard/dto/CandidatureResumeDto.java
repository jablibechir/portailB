package com.soprarh.portail.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resume d'une candidature pour le dashboard.
 */
public record CandidatureResumeDto(
        UUID id,
        String candidatNom,
        String candidatPrenom,
        String offreTitre,
        String statut,
        Double scoreTotal,
        LocalDate dateSoumission
) {}

