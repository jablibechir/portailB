package com.soprarh.portail.shared.controller;

import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.shared.dto.NotificationResponse;
import com.soprarh.portail.shared.service.NotificationService;
import com.soprarh.portail.user.entity.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des notifications.
 * Implemente les endpoints pour US-NOTIF-07 et US-NOTIF-08.
 *
 * Endpoints :
 *   GET  /api/notifications              -> consulter toutes mes notifications
 *   GET  /api/notifications/non-lues     -> consulter mes notifications non lues
 *   GET  /api/notifications/count        -> compter mes notifications non lues
 *   PUT  /api/notifications/{id}/lire    -> marquer une notification comme lue
 *   PUT  /api/notifications/lire-toutes  -> marquer toutes les notifications comme lues
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * US-NOTIF-07: Consulter toutes mes notifications.
     * GET /api/notifications
     * Accessible par: tout utilisateur authentifie (permission VIEW_NOTIFICATIONS)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMesNotifications(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<NotificationResponse> notifications = notificationService
                .getMesNotifications(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(notifications, "Notifications trouvees: " + notifications.size()));
    }

    /**
     * Consulter mes notifications non lues.
     * GET /api/notifications/non-lues
     * Accessible par: tout utilisateur authentifie (permission VIEW_NOTIFICATIONS)
     */
    @GetMapping("/non-lues")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMesNotificationsNonLues(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<NotificationResponse> notifications = notificationService
                .getMesNotificationsNonLues(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(notifications, "Notifications non lues: " + notifications.size()));
    }

    /**
     * Compter mes notifications non lues.
     * GET /api/notifications/count
     * Accessible par: tout utilisateur authentifie (permission VIEW_NOTIFICATIONS)
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<ApiResponse<Long>> countNonLues(
            @AuthenticationPrincipal Utilisateur currentUser) {

        long count = notificationService.countNonLues(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(count, "Nombre de notifications non lues"));
    }

    /**
     * US-NOTIF-08: Marquer une notification comme lue.
     * PUT /api/notifications/{id}/lire
     * Accessible par: tout utilisateur authentifie (permission VIEW_NOTIFICATIONS)
     */
    @PutMapping("/{id}/lire")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<ApiResponse<NotificationResponse>> marquerCommeLue(
            @PathVariable UUID id,
            @AuthenticationPrincipal Utilisateur currentUser) {

        NotificationResponse notification = notificationService
                .marquerCommeLue(id, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(notification, "Notification marquee comme lue"));
    }

    /**
     * US-NOTIF-08: Marquer toutes les notifications comme lues.
     * PUT /api/notifications/lire-toutes
     * Accessible par: tout utilisateur authentifie (permission VIEW_NOTIFICATIONS)
     */
    @PutMapping("/lire-toutes")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<ApiResponse<Integer>> marquerToutesCommeLues(
            @AuthenticationPrincipal Utilisateur currentUser) {

        int count = notificationService.marquerToutesCommeLues(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(count, count + " notifications marquees comme lues"));
    }
}

