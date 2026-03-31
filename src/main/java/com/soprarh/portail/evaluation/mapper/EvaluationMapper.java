package com.soprarh.portail.evaluation.mapper;

import com.soprarh.portail.evaluation.dto.EvaluationResponse;
import com.soprarh.portail.evaluation.entity.Evaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper pour convertir Evaluation <-> EvaluationResponse.
 */
@Mapper(componentModel = "spring")
public interface EvaluationMapper {

    @Mapping(source = "candidature.id", target = "candidatureId")
    @Mapping(source = "candidature.offre.titre", target = "candidatureOffreTitre")
    @Mapping(source = "evaluateur.id", target = "evaluateurId")
    @Mapping(source = "evaluateur.nom", target = "evaluateurNom")
    @Mapping(source = "evaluateur.prenom", target = "evaluateurPrenom")
    EvaluationResponse toResponse(Evaluation evaluation);

    List<EvaluationResponse> toResponseList(List<Evaluation> evaluations);
}

