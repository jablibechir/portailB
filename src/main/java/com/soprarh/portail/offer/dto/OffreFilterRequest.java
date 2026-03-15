package com.soprarh.portail.offer.dto;

/**
 * DTO pour les filtres de recherche d'offres.
 * US-OFF-05: Filtre par mot-cle, experience, formation.
 * Tous les champs sont optionnels.
 */
public record OffreFilterRequest(
        String keyword,
        String experience,
        String formation
) {}

