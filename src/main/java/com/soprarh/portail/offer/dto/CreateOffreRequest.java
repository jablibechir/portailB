package com.soprarh.portail.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO pour creer une nouvelle offre d'emploi.
 * US-OFF-01: Saisie du titre, description, experience, formation.
 */
public record CreateOffreRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String titre,

        String description,

        String competencesRequises,

        @Size(max = 100, message = "L'experience ne peut pas depasser 100 caracteres")
        String experienceRequise,

        @Size(max = 200, message = "La formation ne peut pas depasser 200 caracteres")
        String formationRequise,

        /**
         * Statut initial: "brouillon" ou "publiee".
         * Si null, defaut = brouillon.
         */
        String statut,

        /**
         * Date d'expiration de l'offre (optionnelle).
         */
        LocalDate dateExpiration,

        /**
         * Type d'emploi: "Emploi à temps plein", "Stage", "Emploi à court terme", "Alternance".
         * Si null, defaut = "Emploi à temps plein".
         */
        String typeEmploi
) {}

