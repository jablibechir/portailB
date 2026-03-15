package com.soprarh.portail.application.dto;

/**
 * DTO pour les filtres de recherche des candidatures (RH).
 * US-RH-04: Filtrer par statut, score min, score max.
 * Tous les champs sont optionnels.
 */
public record CandidatureFilterRequest(
        String statut,
        Double scoreMin,
        Double scoreMax
) {}

