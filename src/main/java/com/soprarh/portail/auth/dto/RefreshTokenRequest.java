package com.soprarh.portail.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requete pour le renouvellement du token d'acces via refresh token.
 * Stateless : le refresh token est un JWT signe, aucune DB consultee.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Le refresh token est obligatoire")
        String refreshToken
) {}

