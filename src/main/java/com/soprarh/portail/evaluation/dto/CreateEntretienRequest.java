package com.soprarh.portail.evaluation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour creer un nouvel entretien.
 */
public record CreateEntretienRequest(

        @NotNull(message = "L'ID de la candidature est obligatoire")
        UUID candidatureId,

        @NotNull(message = "La date de l'entretien est obligatoire")
        LocalDateTime dateEntretien,

        String lieu,

        String type, // presentiel, visio, telephonique

        String notes,

        UUID interviewerId // optional: ID du manager/interviewer
) {
}

