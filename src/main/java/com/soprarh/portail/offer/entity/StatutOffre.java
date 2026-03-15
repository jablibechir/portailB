package com.soprarh.portail.offer.entity;

/**
 * Enum pour le statut d'une offre d'emploi.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (statut IN ('brouillon', 'publiee', 'archivee'))
 */
public enum StatutOffre {
    brouillon,
    publiee,
    archivee
}

