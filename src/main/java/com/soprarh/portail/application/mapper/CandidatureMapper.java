package com.soprarh.portail.application.mapper;

import com.soprarh.portail.application.dto.CandidatureResponse;
import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.cv.entity.Cv;
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
                c.getLettreMotivation(),
                c.getDateCreation(),
                mapCvInfo(c.getCv()),
                c.getManager() != null ? c.getManager().getId() : null,
                c.getManager() != null ? (c.getManager().getPrenom() + " " + c.getManager().getNom()) : null
        );
    }

    /**
     * Convertit un CV en CvInfo DTO.
     */
    private CandidatureResponse.CvInfo mapCvInfo(Cv cv) {
        if (cv == null) {
            return null;
        }
        return new CandidatureResponse.CvInfo(
                cv.getId(),
                cv.getFichier(),
                "/api/candidatures/cv/" + cv.getId(),
                cv.getDateUpload()
        );
    }
}

