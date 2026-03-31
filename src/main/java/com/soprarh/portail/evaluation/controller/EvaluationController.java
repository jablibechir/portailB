package com.soprarh.portail.evaluation.controller;

import com.soprarh.portail.evaluation.dto.CreateEvaluationRequest;
import com.soprarh.portail.evaluation.dto.EvaluationResponse;
import com.soprarh.portail.evaluation.service.EvaluationService;
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
 * Controller REST pour la gestion des evaluations.
 * Implemente les endpoints pour US-EVAL-01 a US-EVAL-03.
 *
 * Endpoints RH :
 *   POST /api/evaluations/rh                       -> creer une evaluation (RH)
 *   GET  /api/evaluations/candidature/{id}         -> consulter les evaluations d'une candidature
 *   GET  /api/evaluations/mes                      -> consulter mes evaluations
 *   GET  /api/evaluations/{id}                     -> consulter une evaluation
 *
 * Endpoints Manager :
 *   POST /api/evaluations/manager                  -> creer une evaluation (Manager)
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ==================== Endpoints RH ====================

    /**
     * US-EVAL-01: Creer une evaluation (RH).
     * POST /api/evaluations/rh
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     */
    @PostMapping("/rh")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluerParRh(
            @Valid @RequestBody CreateEvaluationRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        EvaluationResponse evaluation = evaluationService.evaluerParRh(request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(evaluation, "Evaluation creee avec succes"));
    }

    /**
     * US-EVAL-03: Consulter toutes les evaluations d'une candidature.
     * GET /api/evaluations/candidature/{candidatureId}
     * Accessible par: RH et Manager (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/candidature/{candidatureId}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationsByCandidature(
            @PathVariable UUID candidatureId) {

        List<EvaluationResponse> evaluations = evaluationService
                .getEvaluationsByCandidature(candidatureId);

        return ResponseEntity.ok(
                ApiResponse.success(evaluations, "Evaluations trouvees: " + evaluations.size()));
    }

    /**
     * Consulter mes evaluations (celles que j'ai faites).
     * GET /api/evaluations/mes
     * Accessible par: RH et Manager (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/mes")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getMesEvaluations(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<EvaluationResponse> evaluations = evaluationService
                .getMesEvaluations(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(evaluations, "Mes evaluations: " + evaluations.size()));
    }

    /**
     * Consulter une evaluation par son ID.
     * GET /api/evaluations/{id}
     * Accessible par: RH et Manager (permission EVALUATE_CANDIDATES)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluationById(
            @PathVariable UUID id) {

        EvaluationResponse evaluation = evaluationService.getEvaluationById(id);

        return ResponseEntity.ok(
                ApiResponse.success(evaluation, "Evaluation trouvee"));
    }

    // ==================== Endpoints Manager ====================

    /**
     * US-EVAL-02: Creer une evaluation (Manager).
     * POST /api/evaluations/manager
     * Accessible par: Manager (permission VALIDATE_CANDIDATES)
     */
    @PostMapping("/manager")
    @PreAuthorize("hasAuthority('VALIDATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluerParManager(
            @Valid @RequestBody CreateEvaluationRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        EvaluationResponse evaluation = evaluationService.evaluerParManager(request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(evaluation, "Evaluation creee avec succes"));
    }
}

