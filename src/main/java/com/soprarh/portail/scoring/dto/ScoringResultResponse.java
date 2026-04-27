package com.soprarh.portail.scoring.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de reponse pour le resultat de scoring d'une candidature.
 * Contient le score total et le detail par critere.
 *
 * @param candidatureId  identifiant de la candidature evaluee
 * @param candidatNom    nom du candidat
 * @param candidatPrenom prenom du candidat
 * @param scoreTotal     score total calcule
 * @param details        liste des scores par critere
 */
public record ScoringResultResponse(
        UUID candidatureId,
        String candidatNom,
        String candidatPrenom,
        Double scoreTotal,
        List<CritereScoreDetail> details
) {}

