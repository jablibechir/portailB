package com.soprarh.portail.offer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour une offre d'emploi.
 * Utilise pour toutes les reponses API concernant les offres.
 */
public record OffreResponse(
        UUID id,
        String titre,
        String description,
        String competencesRequises,
        String experienceRequise,
        String formationRequise,
        LocalDate datePublication,
        String statut,
        String creeParNom,
        UUID creeParId,
        LocalDateTime dateCreation,
        LocalDate dateExpiration,
        String typeEmploi
) {}

