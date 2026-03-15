package com.soprarh.portail.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour une candidature.
 * Utilise pour toutes les reponses API concernant les candidatures.
 * Ne contient jamais de donnees sensibles (mot de passe, etc.).
 */
public record CandidatureResponse(
        UUID id,
        UUID candidatId,
        String candidatNom,
        String candidatPrenom,
        String candidatEmail,
        UUID offreId,
        String offreTitre,
        LocalDate dateSoumission,
        String statut,
        Double scoreTotal,
        LocalDateTime dateCreation
) {}

