package com.soprarh.portail.scoring.entity;

/**
 * Enum pour la colonne "type" de la table criteres_scoring.
 * CHECK (type IN ('COMPETENCES','EXPERIENCE','FORMATION','LANGUES','CERTIFICATIONS','SOFT_SKILLS'))
 */
public enum TypeCritere {
    COMPETENCES,
    EXPERIENCE,
    FORMATION,
    LANGUES,
    CERTIFICATIONS,
    SOFT_SKILLS
}

