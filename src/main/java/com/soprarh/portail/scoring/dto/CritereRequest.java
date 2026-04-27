package com.soprarh.portail.scoring.dto;

import com.soprarh.portail.scoring.entity.TypeCritere;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO pour creer ou modifier un critere de scoring.
 * Utilise par le RH pour definir les criteres d'une offre.
 *
 * @param nom   nom du critere (ex: "Maitrise Java")
 * @param poids poids du critere (entre 0 exclus et 100 inclus)
 * @param type  type du critere (COMPETENCES, EXPERIENCE, FORMATION, LANGUES, CERTIFICATIONS, SOFT_SKILLS)
 */
public record CritereRequest(

        @NotBlank(message = "Le nom du critere est obligatoire")
        @Size(max = 100, message = "Le nom ne peut pas depasser 100 caracteres")
        String nom,

        @NotNull(message = "Le poids est obligatoire")
        @DecimalMin(value = "0.01", message = "Le poids doit etre superieur a 0")
        @DecimalMax(value = "100", message = "Le poids ne peut pas depasser 100")
        Double poids,

        @NotNull(message = "Le type de critere est obligatoire")
        TypeCritere type
) {}

