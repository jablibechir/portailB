package com.soprarh.portail.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Enveloppe de reponse API standard.
 * Toutes les reponses (succes et erreurs) utilisent ce format.
 *
 * Format JSON :
 * {
 *   "success": true,
 *   "message": "...",
 *   "data": { ... },       // null si erreur
 *   "timestamp": "..."
 * }
 *
 * @JsonInclude(NON_NULL) : les champs null ne sont pas serialises.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation reussie");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}

