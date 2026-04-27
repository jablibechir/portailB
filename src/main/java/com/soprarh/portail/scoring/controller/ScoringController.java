package com.soprarh.portail.scoring.controller;

import com.soprarh.portail.scoring.dto.*;
import com.soprarh.portail.scoring.service.ScoringService;
import com.soprarh.portail.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller du module scoring.
 * Gestion des critères et déclenchement du scoring IA.
 */
@RestController
@RequestMapping("/api/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    // ═══════════════════════════════════════════
    // CRITÈRES DE SCORING (RH)
    // ═══════════════════════════════════════════

    /**
     * GET /api/scoring/offre/{offreId}/criteres
     * Récupérer tous les critères d'une offre.
     */
    @GetMapping("/offre/{offreId}/criteres")
    @PreAuthorize("hasAuthority('MANAGE_SCORING_CRITERIA')")
    public ResponseEntity<ApiResponse<List<CritereResponse>>> getCriteres(@PathVariable UUID offreId) {
        List<CritereResponse> criteres = scoringService.getCriteresByOffre(offreId);
        return ResponseEntity.ok(ApiResponse.success(criteres, "Critères récupérés"));
    }

    /**
     * POST /api/scoring/offre/{offreId}/criteres
     * Créer un critère pour une offre.
     */
    @PostMapping("/offre/{offreId}/criteres")
    @PreAuthorize("hasAuthority('MANAGE_SCORING_CRITERIA')")
    public ResponseEntity<ApiResponse<CritereResponse>> createCritere(
            @PathVariable UUID offreId,
            @Valid @RequestBody CritereRequest request) {
        CritereResponse critere = scoringService.createCritere(offreId, request);
        return ResponseEntity.ok(ApiResponse.success(critere, "Critère créé avec succès"));
    }

    /**
     * PUT /api/scoring/criteres/{critereId}
     * Modifier un critère existant.
     */
    @PutMapping("/criteres/{critereId}")
    @PreAuthorize("hasAuthority('MANAGE_SCORING_CRITERIA')")
    public ResponseEntity<ApiResponse<CritereResponse>> updateCritere(
            @PathVariable UUID critereId,
            @Valid @RequestBody CritereRequest request) {
        CritereResponse critere = scoringService.updateCritere(critereId, request);
        return ResponseEntity.ok(ApiResponse.success(critere, "Critère modifié avec succès"));
    }

    /**
     * DELETE /api/scoring/criteres/{critereId}
     * Supprimer un critère.
     */
    @DeleteMapping("/criteres/{critereId}")
    @PreAuthorize("hasAuthority('MANAGE_SCORING_CRITERIA')")
    public ResponseEntity<ApiResponse<Void>> deleteCritere(@PathVariable UUID critereId) {
        scoringService.deleteCritere(critereId);
        return ResponseEntity.ok(ApiResponse.success(null, "Critère supprimé avec succès"));
    }

    // ═══════════════════════════════════════════
    // SCORING IA
    // ═══════════════════════════════════════════

    /**
     * POST /api/scoring/candidature/{candidatureId}
     * Lancer le scoring d'une candidature via le microservice IA Python.
     */
    @PostMapping("/candidature/{candidatureId}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<ScoringResultResponse>> scoreCandidature(
            @PathVariable UUID candidatureId) {
        ScoringResultResponse result = scoringService.scoreCandidature(candidatureId);
        return ResponseEntity.ok(ApiResponse.success(result, "Scoring terminé"));
    }

    /**
     * POST /api/scoring/calculer/{candidatureId}
     * Alias attendu par le frontend (déclenche le calcul du score).
     */
    @PostMapping("/calculer/{candidatureId}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<ScoringResultResponse>> calculerScore(
            @PathVariable UUID candidatureId) {
        ScoringResultResponse result = scoringService.scoreCandidature(candidatureId);
        return ResponseEntity.ok(ApiResponse.success(result, "Scoring terminé"));
    }

    /**
     * GET /api/scoring/candidature/{candidatureId}
     * Consulter le résultat de scoring d'une candidature.
     */
    @GetMapping("/candidature/{candidatureId}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<ScoringResultResponse>> getScore(
            @PathVariable UUID candidatureId) {
        ScoringResultResponse result = scoringService.getScoreForCandidature(candidatureId);
        return ResponseEntity.ok(ApiResponse.success(result, "Résultat de scoring récupéré"));
    }
}

