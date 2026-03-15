package com.soprarh.portail.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de reponse apres login ou refresh.
 * Contient les deux tokens JWT.
 *
 * Bonne pratique : on retourne le type "Bearer" et la duree d'expiration
 * pour que le client n'ait pas a le hardcoder.
 */
public record AuthResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn
) {
    /**
     * Constructeur de commodite avec tokenType = "Bearer" par defaut.
     */
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}

