package com.soprarh.portail.application.entity;

/**
 * Enum pour le statut d'une candidature.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (statut IN ('soumise','en_evaluation_rh','envoyee_manager',
 *                   'acceptee_manager','rejetee_rh','rejetee_manager','entretien_planifie'))
 */
public enum StatutCandidature {
    soumise,
    en_evaluation_rh,
    envoyee_manager,
    acceptee_manager,
    rejetee_rh,
    rejetee_manager,
    entretien_planifie
}

