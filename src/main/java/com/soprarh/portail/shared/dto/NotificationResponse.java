package com.soprarh.portail.shared.dto;

import com.soprarh.portail.shared.entity.TypeNotification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de reponse pour les notifications.
 */
public record NotificationResponse(
        UUID id,
        String titre,
        String message,
        TypeNotification type,
        Boolean lu,
        LocalDateTime dateCreation
) {
}

