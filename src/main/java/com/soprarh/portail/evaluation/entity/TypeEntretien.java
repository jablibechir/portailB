package com.soprarh.portail.evaluation.entity;

/**
 * Enum pour le type d'entretien.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (type IN ('presentiel','visio','telephonique'))
 */
public enum TypeEntretien {
    presentiel,
    visio,
    telephonique
}

