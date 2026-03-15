package com.soprarh.portail.shared;

import org.springframework.http.HttpStatus;

/**
 * Exception metier generique.
 * Lancee par les services quand une regle metier est violee.
 * Capturee par GlobalExceptionHandler et transformee en reponse HTTP propre.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

