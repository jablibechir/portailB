package com.soprarh.portail.evaluation.controller;

import com.soprarh.portail.evaluation.dto.CreateEntretienRequest;
import com.soprarh.portail.evaluation.dto.EntretienResponse;
import com.soprarh.portail.evaluation.service.EntretienService;
import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des entretiens.
 *
 * Endpoints RH :
 *   POST  /api/entretiens                    -> planifier un entretien (US-ENT-01)
 *   GET   /api/entretiens/candidature/{id}   -> entretiens d'une candidature
 *   GET   /api/entretiens/mes-planifies      -> entretiens que j'ai planifies
 *   GET   /api/entretiens/{id}               -> detail d'un entretien
 *   PATCH /api/entretiens/{id}/statut        -> confirmer/annuler un entretien (US-ENT-03)
 *   GET   /api/entretiens/calendrier/rh      -> calendrier global RH (US-ENT-05)
 *
 * Endpoints Manager :
 *   POST  /api/entretiens                    -> planifier un entretien (US-ENT-02, meme endpoint que RH)
 *   GET   /api/entretiens/calendrier/manager -> calendrier Manager (US-ENT-05)
 *
 * Endpoints Candidat :
 *   GET   /api/entretiens/mes-a-venir        -> mes entretiens a venir (US-ENT-04)
 */
@RestController
@RequestMapping("/api/entretiens")
@RequiredArgsConstructor
public class EntretienController {

    private final EntretienService entretienService;

    // ==================== Endpoints RH ====================

    /**
     * Planifier un entretien pour une candidature.
     * POST /api/entretiens
     * Accessible par: RH (permission PLAN_INTERVIEWS)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PLAN_INTERVIEWS')")
    public ResponseEntity<ApiResponse<EntretienResponse>> planifierEntretien(
            @Valid @RequestBody CreateEntretienRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        EntretienResponse entretien = entretienService.planifierEntretien(request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(entretien, "Entretien planifie avec succes"));
    }

    /**
     * Consulter les entretiens d'une candidature.
     * GET /api/entretiens/candidature/{candidatureId}
     * Accessible par: RH et Manager (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/candidature/{candidatureId}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> getEntretiensByCandidature(
            @PathVariable UUID candidatureId) {

        List<EntretienResponse> entretiens = entretienService.getEntretiensByCandidature(candidatureId);

        return ResponseEntity.ok(
                ApiResponse.success(entretiens, "Entretiens trouves: " + entretiens.size()));
    }

    /**
     * Consulter les entretiens que j'ai planifies.
     * GET /api/entretiens/mes-planifies
     * Accessible par: RH (permission PLAN_INTERVIEWS)
     */
    @GetMapping("/mes-planifies")
    @PreAuthorize("hasAuthority('PLAN_INTERVIEWS')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> getMesEntretiensPlanifies(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<EntretienResponse> entretiens = entretienService.getMesEntretiensPlanifies(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(entretiens, "Mes entretiens planifies: " + entretiens.size()));
    }

    /**
     * Recuperer un entretien par son ID.
     * GET /api/entretiens/{id}
     * Accessible par: RH et Manager (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<EntretienResponse>> getEntretienById(@PathVariable UUID id) {

        EntretienResponse entretien = entretienService.getEntretienById(id);

        return ResponseEntity.ok(
                ApiResponse.success(entretien, "Entretien trouve"));
    }

    /**
     * Mettre a jour le statut d'un entretien.
     * PATCH /api/entretiens/{id}/statut?statut=...
     * Accessible par: RH (permission PLAN_INTERVIEWS)
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('PLAN_INTERVIEWS')")
    public ResponseEntity<ApiResponse<EntretienResponse>> updateStatut(
            @PathVariable UUID id,
            @RequestParam String statut) {

        EntretienResponse entretien = entretienService.updateStatut(id, statut);

        return ResponseEntity.ok(
                ApiResponse.success(entretien, "Statut de l'entretien mis a jour"));
    }

    // ==================== Endpoints Candidat ====================

    /**
     * US-ENT-04: Consulter mes entretiens a venir (calendrier candidat).
     * GET /api/entretiens/mes-a-venir
     * Accessible par: Candidat (permission VIEW_CALENDAR)
     */
    @GetMapping("/mes-a-venir")
    @PreAuthorize("hasAuthority('VIEW_CALENDAR')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> getMesEntretiensAVenir(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<EntretienResponse> entretiens = entretienService.getMesEntretiensAVenir(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(entretiens, "Mes entretiens a venir: " + entretiens.size()));
    }

    // ==================== Endpoints Calendrier RH/Manager ====================

    /**
     * US-ENT-05: Calendrier RH — tous les entretiens a venir.
     * GET /api/entretiens/calendrier/rh
     * Accessible par: RH (permission PLAN_INTERVIEWS)
     */
    @GetMapping("/calendrier/rh")
    @PreAuthorize("hasAuthority('PLAN_INTERVIEWS')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> getCalendrierRh() {

        List<EntretienResponse> entretiens = entretienService.getCalendrierRh();

        return ResponseEntity.ok(
                ApiResponse.success(entretiens, "Calendrier RH — entretiens a venir: " + entretiens.size()));
    }

    /**
     * US-ENT-05: Calendrier Manager — entretiens a venir des candidatures Manager.
     * GET /api/entretiens/calendrier/manager
     * Accessible par: Manager (permission VIEW_CALENDAR)
     */
    @GetMapping("/calendrier/manager")
    @PreAuthorize("hasAuthority('VIEW_CALENDAR')")
    public ResponseEntity<ApiResponse<List<EntretienResponse>>> getCalendrierManager() {

        List<EntretienResponse> entretiens = entretienService.getCalendrierManager();

        return ResponseEntity.ok(
                ApiResponse.success(entretiens, "Calendrier Manager — entretiens a venir: " + entretiens.size()));
    }
}

