package com.soprarh.portail.evaluation.dto;

import com.soprarh.portail.evaluation.entity.StatutEntretien;
import com.soprarh.portail.evaluation.entity.TypeEntretien;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour les entretiens.
 */
public record EntretienResponse(
        UUID id,
        UUID candidatureId,
        String candidatureOffreTitre,
        String candidatNom,
        String candidatPrenom,
        String candidatEmail,
        UUID planifieParId,
        String planifieParNom,
        LocalDateTime dateEntretien,
        String lieu,
        TypeEntretien type,
        StatutEntretien statut,
        String notes,
        LocalDateTime dateCreation
) {
}

