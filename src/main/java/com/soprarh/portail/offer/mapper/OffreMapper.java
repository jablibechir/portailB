package com.soprarh.portail.offer.mapper;

import com.soprarh.portail.offer.dto.OffreResponse;
import com.soprarh.portail.offer.entity.OffreEmploi;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel pour convertir entre OffreEmploi et ses DTOs.
 * Simple et comprehensible pour un debutant.
 */
@Component
public class OffreMapper {

    /**
     * Convertit une entite OffreEmploi en DTO OffreResponse.
     */
    public OffreResponse toResponse(OffreEmploi offre) {
        if (offre == null) {
            return null;
        }

        return new OffreResponse(
                offre.getId(),
                offre.getTitre(),
                offre.getDescription(),
                offre.getCompetencesRequises(),
                offre.getExperienceRequise(),
                offre.getFormationRequise(),
                offre.getLanguesRequises(),
                offre.getCertificationsRequises(),
                offre.getSoftSkillsRequis(),
                offre.getDatePublication(),
                offre.getStatut() != null ? offre.getStatut().name() : null,
                offre.getCreePar() != null ? (offre.getCreePar().getPrenom() + " " + offre.getCreePar().getNom()) : null,
                offre.getCreePar() != null ? offre.getCreePar().getId() : null,
                offre.getDateCreation(),
                offre.getDateExpiration(),
                offre.getTypeEmploi(),
                offre.getRecommandeePar() != null ? offre.getRecommandeePar().getId() : null,
                offre.getRecommandeePar() != null ? (offre.getRecommandeePar().getPrenom() + " " + offre.getRecommandeePar().getNom()) : null,
                offre.getCommentaireManager(),
                offre.getDateRecommandation()
        );
    }
}

