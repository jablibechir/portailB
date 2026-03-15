package com.soprarh.portail.auth.dto;

import com.soprarh.portail.user.entity.EtatUtilisateur;
import com.soprarh.portail.user.entity.TypeUtilisateur;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de reponse representant le profil d'un utilisateur connecte.
 * Ne contient JAMAIS le mot de passe ni aucun token.
 */
public record UserProfileResponse(

        UUID id,
        String nom,
        String prenom,
        String email,
        EtatUtilisateur etat,
        TypeUtilisateur typeUtilisateur,
        LocalDateTime dateCreation,

        /**
         * Noms des roles assignes. Ex: ["CANDIDAT"]
         * Les permissions granulaires ne sont pas exposees ici (usage interne Spring Security).
         */
        Set<String> roles
) {}

