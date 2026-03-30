package com.soprarh.portail.user.mapper;

import com.soprarh.portail.user.dto.ProfilResponse;
import com.soprarh.portail.user.entity.Profil;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel pour convertir entre Profil et ses DTOs.
 */
@Component
public class ProfilMapper {

    /**
     * Convertit une entite Profil en DTO ProfilResponse.
     * Inclut les informations de l'utilisateur lie.
     */
    public ProfilResponse toResponse(Profil profil) {
        if (profil == null) {
            return null;
        }

        return new ProfilResponse(
                profil.getId(),
                profil.getUtilisateur() != null ? profil.getUtilisateur().getId() : null,
                profil.getUtilisateur() != null ? profil.getUtilisateur().getNom() : null,
                profil.getUtilisateur() != null ? profil.getUtilisateur().getPrenom() : null,
                profil.getUtilisateur() != null ? profil.getUtilisateur().getEmail() : null,
                profil.getTelephone(),
                profil.getPhotoUrl(),
                profil.getAdresse(),
                profil.getDateMiseAJour()
        );
    }
}

