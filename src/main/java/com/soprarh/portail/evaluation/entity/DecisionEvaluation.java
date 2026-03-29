package com.soprarh.portail.evaluation.entity;

/**
 * Enum pour la decision d'une evaluation.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (decision IN ('pour_suivre','a_revoir','rejeter'))
 */
public enum DecisionEvaluation {
    pour_suivre,
    a_revoir,
    rejeter
}

