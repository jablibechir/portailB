package com.soprarh.portail.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour changer le role d'un utilisateur.
 * Le role doit etre un nom valide : "CANDIDAT", "MANAGER", ou "RH".
 */
public record ChangeRoleRequest(
        @NotBlank(message = "Le nom du role est obligatoire")
        String roleName
) {}

