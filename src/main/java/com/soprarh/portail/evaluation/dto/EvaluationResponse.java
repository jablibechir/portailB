package com.soprarh.portail.evaluation.dto;

import com.soprarh.portail.evaluation.entity.DecisionEvaluation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour les evaluations.
 * Inclut les infos candidat et score pour l'historique.
 */
public record EvaluationResponse(
        UUID id,
        UUID candidatureId,
        String candidatureOffreTitre,
        UUID evaluateurId,
        String evaluateurNom,
        String evaluateurPrenom,
        String candidatNom,
        String candidatPrenom,
        Double scoreTotal,
        String commentaire,
        DecisionEvaluation decision,
        LocalDate dateEvaluation,
        LocalDateTime dateCreation
) {
}

