package com.soprarh.portail.user.dto;

import java.util.Set;
import java.util.UUID;

/**
 * DTO de reponse pour un utilisateur.
 * Exclut les informations sensibles (mot de passe).
 */
public record UserResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String etat,
        String typeUtilisateur,
        Set<String> roles
) {}

