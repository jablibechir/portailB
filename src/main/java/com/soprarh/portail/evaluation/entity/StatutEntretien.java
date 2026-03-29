package com.soprarh.portail.evaluation.entity;

/**
 * Enum pour le statut d'un entretien.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (statut IN ('planifie','confirme','annule','termine'))
 */
public enum StatutEntretien {
    planifie,
    confirme,
    annule,
    termine
}

