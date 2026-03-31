package com.soprarh.portail.evaluation.mapper;

import com.soprarh.portail.evaluation.dto.EntretienResponse;
import com.soprarh.portail.evaluation.entity.Entretien;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper pour convertir Entretien <-> EntretienResponse.
 */
@Mapper(componentModel = "spring")
public interface EntretienMapper {

    @Mapping(source = "candidature.id", target = "candidatureId")
    @Mapping(source = "candidature.offre.titre", target = "candidatureOffreTitre")
    @Mapping(source = "candidature.candidat.nom", target = "candidatNom")
    @Mapping(source = "candidature.candidat.prenom", target = "candidatPrenom")
    @Mapping(source = "candidature.candidat.email", target = "candidatEmail")
    @Mapping(source = "planifiePar.id", target = "planifieParId")
    @Mapping(source = "planifiePar.nom", target = "planifieParNom")
    EntretienResponse toResponse(Entretien entretien);

    List<EntretienResponse> toResponseList(List<Entretien> entretiens);
}

