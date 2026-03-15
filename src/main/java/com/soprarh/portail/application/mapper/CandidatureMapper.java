package com.soprarh.portail.application.mapper;

import com.soprarh.portail.application.dto.CandidatureResponse;
import com.soprarh.portail.application.entity.Candidature;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel pour convertir entre Candidature et ses DTOs.
 * Simple et comprehensible — meme pattern que OffreMapper.
 */
@Component
public class CandidatureMapper {

    /**
     * Convertit une entite Candidature en DTO CandidatureResponse.
     */
    public CandidatureResponse toResponse(Candidature c) {
        if (c == null) {
            return null;
        }

        return new CandidatureResponse(
                c.getId(),
                c.getCandidat() != null ? c.getCandidat().getId() : null,
                c.getCandidat() != null ? c.getCandidat().getNom() : null,
                c.getCandidat() != null ? c.getCandidat().getPrenom() : null,
                c.getCandidat() != null ? c.getCandidat().getEmail() : null,
                c.getOffre() != null ? c.getOffre().getId() : null,
                c.getOffre() != null ? c.getOffre().getTitre() : null,
                c.getDateSoumission(),
                c.getStatut() != null ? c.getStatut().name() : null,
                c.getScoreTotal(),
                c.getDateCreation()
        );
    }
}

