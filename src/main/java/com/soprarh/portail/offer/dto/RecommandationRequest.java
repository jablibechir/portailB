package com.soprarh.portail.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la recommandation d'un poste par un Manager.
 */
public record RecommandationRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        String description,

        String competencesRequises,

        @Size(max = 100, message = "L'experience ne peut pas depasser 100 caracteres")
        String experienceRequise,

        @Size(max = 200, message = "La formation ne peut pas depasser 200 caracteres")
        String formationRequise,

        String languesRequises,

        String certificationsRequises,

        String softSkillsRequis,

        /**
         * Type d'emploi: "Emploi a temps plein", "Stage", "Emploi a court terme", "Alternance".
         */
        String typeEmploi,

        @NotBlank(message = "Le commentaire est obligatoire")
        String commentaireManager
) {}
