package com.soprarh.portail.evaluation.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO pour creer une nouvelle evaluation.
 * Utilise par le RH et le Manager.
 */
public record CreateEvaluationRequest(

        @NotNull(message = "L'ID de la candidature est obligatoire")
        java.util.UUID candidatureId,

        String commentaire,

        @NotNull(message = "La decision est obligatoire")
        String decision // pour_suivre, a_revoir, rejeter
) {
}

