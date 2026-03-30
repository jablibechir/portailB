package com.soprarh.portail.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour le profil utilisateur.
 * US-PROFIL-01: Consulter son profil.
 */
public record ProfilResponse(
        UUID id,
        UUID utilisateurId,
        String nom,
        String prenom,
        String email,
        String telephone,
        String photoUrl,
        String adresse,
        LocalDateTime dateMiseAJour
) {}

