package com.soprarh.portail.application.controller;

import com.soprarh.portail.application.dto.*;
import com.soprarh.portail.application.service.CandidatureService;
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
 * Controller REST pour la gestion des candidatures.
 * Implemente les endpoints pour US-CAND-01 a US-RH-05.
 *
 * Endpoints Candidat :
 *   POST /api/candidatures              -> deposer une candidature
 *   GET  /api/candidatures/mes          -> voir ses candidatures
 *
 * Endpoints RH :
 *   PATCH /api/candidatures/{id}/statut -> changer le statut
 *   GET   /api/candidatures             -> lister toutes les candidatures (tri par date/score)
 *   GET   /api/candidatures/filtrer     -> filtrer par statut, score min, score max
 *   GET   /api/candidatures/{id}        -> voir le detail d'une candidature
 */
@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    // ==================== Endpoints Candidat ====================

    /**
     * US-CAND-01: Deposer une candidature.
     * POST /api/candidatures
     * Accessible par: Candidat (permission APPLY_OFFERS)
     * Le candidat_id est extrait du token JWT via @AuthenticationPrincipal.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('APPLY_OFFERS')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> deposerCandidature(
            @Valid @RequestBody CreateCandidatureRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        CandidatureResponse candidature = candidatureService.deposerCandidature(
                request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(candidature, "Candidature deposee avec succes"));
    }

    /**
     * US-CAND-02: Voir ses candidatures.
     * GET /api/candidatures/mes
     * Accessible par: Candidat (permission APPLY_OFFERS)
     * Retourne uniquement les candidatures du candidat connecte.
     */
    @GetMapping("/mes")
    @PreAuthorize("hasAuthority('APPLY_OFFERS')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> mesCandidatures(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<CandidatureResponse> candidatures = candidatureService.mesCandidatures(
                currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(candidatures, "Candidatures trouvees: " + candidatures.size()));
    }

    // ==================== Endpoints RH ====================

    /**
     * US-CAND-03: Changer le statut d'une candidature.
     * PATCH /api/candidatures/{id}/statut
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> changerStatut(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatutCandidatureRequest request) {

        CandidatureResponse candidature = candidatureService.changerStatut(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Statut de la candidature mis a jour"));
    }

    /**
     * US-RH-05: Lister toutes les candidatures.
     * GET /api/candidatures?tri=date|score
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     * Par defaut tri par date. Passer ?tri=score pour trier par score.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> listerCandidatures(
            @RequestParam(required = false, defaultValue = "date") String tri) {

        List<CandidatureResponse> candidatures = candidatureService.listerCandidatures(tri);

        return ResponseEntity.ok(
                ApiResponse.success(candidatures, "Candidatures trouvees: " + candidatures.size()));
    }

    /**
     * US-RH-04: Filtrer les candidatures.
     * GET /api/candidatures/filtrer?statut=...&scoreMin=...&scoreMax=...
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     * Tous les parametres sont optionnels.
     */
    @GetMapping("/filtrer")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> filtrerCandidatures(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Double scoreMin,
            @RequestParam(required = false) Double scoreMax) {

        CandidatureFilterRequest filter = new CandidatureFilterRequest(statut, scoreMin, scoreMax);
        List<CandidatureResponse> candidatures = candidatureService.filtrerCandidatures(filter);

        return ResponseEntity.ok(
                ApiResponse.success(candidatures, "Candidatures filtrees: " + candidatures.size()));
    }

    /**
     * Voir le detail d'une candidature.
     * GET /api/candidatures/{id}
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> getCandidature(@PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.getCandidatureById(id);

        return ResponseEntity.ok(ApiResponse.success(candidature));
    }
}

