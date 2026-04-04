package com.soprarh.portail.offer.dto;

/**
 * DTO pour les filtres de recherche d'offres.
 * US-OFF-05: Filtre par mot-cle, experience, formation, date de publication, type d'emploi.
 * Tous les champs sont optionnels.
 *
 * @param keyword    mot-cle recherche dans titre, description, competences
 * @param experience filtre sur experience requise
 * @param formation  filtre sur formation requise
 * @param days       nombre de jours depuis la publication (7, 30, null = tous)
 * @param typeEmploi filtre sur type d'emploi (ex: "Stage", "Alternance", null = tous)
 */
public record OffreFilterRequest(
        String keyword,
        String experience,
        String formation,
        Integer days,
        String typeEmploi
) {}

