package com.soprarh.portail.application.entity;

/**
 * Enum pour le statut d'une candidature.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (statut IN ('soumise', 'en_evaluation', 'acceptee', 'rejetee'))
 */
public enum StatutCandidature {
    soumise,
    en_evaluation,
    acceptee,
    rejetee
}

