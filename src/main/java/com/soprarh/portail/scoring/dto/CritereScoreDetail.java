package com.soprarh.portail.scoring.dto;

import com.soprarh.portail.scoring.entity.TypeCritere;

import java.util.UUID;

/**
 * DTO pour le detail du score obtenu sur un critere specifique.
 * Utilise comme element de la liste "details" dans ScoringResultResponse.
 *
 * @param critereId   identifiant du critere evalue
 * @param critereNom  nom du critere (ex: "Maitrise Java")
 * @param type        type du critere (COMPETENCES, EXPERIENCE, etc.)
 * @param poids       poids du critere dans le scoring total
 * @param scoreObtenu score obtenu par le candidat sur ce critere
 */
public record CritereScoreDetail(
        UUID critereId,
        String critereNom,
        TypeCritere type,
        Double poids,
        Double scoreObtenu
) {}

