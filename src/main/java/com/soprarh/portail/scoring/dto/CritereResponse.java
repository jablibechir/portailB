package com.soprarh.portail.scoring.dto;

import com.soprarh.portail.scoring.entity.TypeCritere;

import java.util.UUID;

/**
 * DTO de reponse pour un critere de scoring.
 * Retourne au frontend les informations d'un critere sans exposer les relations JPA.
 *
 * @param id      identifiant du critere
 * @param nom     nom du critere (ex: "Maitrise Java")
 * @param poids   poids du critere dans le scoring
 * @param type    type du critere (COMPETENCES, EXPERIENCE, etc.)
 * @param offreId identifiant de l'offre a laquelle le critere est associe
 */
public record CritereResponse(
        UUID id,
        String nom,
        Double poids,
        TypeCritere type,
        UUID offreId
) {}

