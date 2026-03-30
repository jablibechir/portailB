package com.soprarh.portail.application.controller;

import com.soprarh.portail.application.dto.*;
import com.soprarh.portail.application.service.CandidatureService;
import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des candidatures.
 * Implemente les endpoints pour US-CAND-01 a US-CAND-11.
 *
 * Endpoints Candidat :
 *   POST /api/candidatures              -> deposer une candidature (avec CV optionnel)
 *   GET  /api/candidatures/mes          -> voir ses candidatures
 *
 * Endpoints RH :
 *   POST  /api/candidatures/{id}/transmettre  -> transmettre au manager
 *   POST  /api/candidatures/{id}/rejeter-rh   -> rejeter (RH)
 *   POST  /api/candidatures/{id}/evaluer      -> passer en evaluation RH
 *   PATCH /api/candidatures/{id}/statut       -> changer le statut (generique)
 *   GET   /api/candidatures                   -> lister toutes les candidatures
 *   GET   /api/candidatures/filtrer           -> filtrer par statut, score
 *   GET   /api/candidatures/{id}              -> voir le detail
 *
 * Endpoints Manager :
 *   POST /api/candidatures/{id}/valider-manager  -> valider (Manager)
 *   POST /api/candidatures/{id}/rejeter-manager  -> rejeter (Manager)
 *   GET  /api/candidatures/manager               -> candidatures transmises au Manager
 *
 * Endpoint CV :
 *   GET  /api/candidatures/cv/{id}  -> telecharger un CV
 */
@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    // ==================== Endpoints Candidat ====================

    /**
     * US-CAND-01 & US-CAND-02: Deposer une candidature avec CV optionnel.
     * POST /api/candidatures
     * Accessible par: Candidat (permission APPLY_OFFERS)
     * Accepte: multipart/form-data avec champs "offreId", "lettreMotivation" (optionnel), "cv" (optionnel)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('APPLY_OFFERS')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> deposerCandidature(
            @RequestParam("offreId") UUID offreId,
            @RequestParam(value = "lettreMotivation", required = false) String lettreMotivation,
            @RequestParam(value = "cv", required = false) MultipartFile cvFile,
            @AuthenticationPrincipal Utilisateur currentUser) {

        CreateCandidatureRequest request = new CreateCandidatureRequest(offreId, lettreMotivation);
        CandidatureResponse candidature = candidatureService.deposerCandidature(
                request, cvFile, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(candidature, "Candidature deposee avec succes"));
    }

    /**
     * Voir ses candidatures.
     * GET /api/candidatures/mes
     * Accessible par: Candidat (permission APPLY_OFFERS)
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
     * US-CAND-07: Transmettre une candidature au Manager.
     * POST /api/candidatures/{id}/transmettre
     * Accessible par: RH (permission TRANSMIT_TO_MANAGER ou EVALUATE_CANDIDATES)
     */
    @PostMapping("/{id}/transmettre")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> transmettreAuManager(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.transmettreAuManager(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature transmise au manager avec succes"));
    }

    /**
     * US-CAND-10: Rejeter une candidature (RH).
     * POST /api/candidatures/{id}/rejeter-rh
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     */
    @PostMapping("/{id}/rejeter-rh")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> rejeterParRh(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.rejeterParRh(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature rejetee par RH"));
    }

    /**
     * Passer une candidature en evaluation RH.
     * POST /api/candidatures/{id}/evaluer
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
     */
    @PostMapping("/{id}/evaluer")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> passerEnEvaluationRh(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.passerEnEvaluationRh(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature passee en evaluation RH"));
    }

    /**
     * Changer le statut d'une candidature (generique).
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
     * Lister toutes les candidatures.
     * GET /api/candidatures?tri=date|score
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
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
     * Filtrer les candidatures.
     * GET /api/candidatures/filtrer?statut=...&scoreMin=...&scoreMax=...
     * Accessible par: RH (permission EVALUATE_CANDIDATES)
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

    // ==================== Endpoints Manager ====================

    /**
     * US-CAND-08: Valider une candidature (Manager).
     * POST /api/candidatures/{id}/valider-manager
     * Accessible par: Manager (permission VALIDATE_CANDIDATES)
     */
    @PostMapping("/{id}/valider-manager")
    @PreAuthorize("hasAuthority('VALIDATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> validerParManager(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.validerParManager(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature validee par le manager"));
    }

    /**
     * US-CAND-09: Rejeter une candidature (Manager).
     * POST /api/candidatures/{id}/rejeter-manager
     * Accessible par: Manager (permission VALIDATE_CANDIDATES)
     */
    @PostMapping("/{id}/rejeter-manager")
    @PreAuthorize("hasAuthority('VALIDATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> rejeterParManager(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.rejeterParManager(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature rejetee par le manager"));
    }

    /**
     * US-CAND-11: Consulter les candidatures transmises au Manager.
     * GET /api/candidatures/manager
     * Accessible par: Manager (permission VALIDATE_CANDIDATES)
     */
    @GetMapping("/manager")
    @PreAuthorize("hasAuthority('VALIDATE_CANDIDATES')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> getCandidaturesManager() {

        List<CandidatureResponse> candidatures = candidatureService.getCandidaturesManager();

        return ResponseEntity.ok(
                ApiResponse.success(candidatures, "Candidatures transmises au manager: " + candidatures.size()));
    }

    // ==================== Endpoint CV ====================

    /**
     * Telecharger un CV.
     * GET /api/candidatures/cv/{id}
     * Accessible par: RH et Manager
     */
    @GetMapping("/cv/{id}")
    @PreAuthorize("hasAnyAuthority('EVALUATE_CANDIDATES', 'VALIDATE_CANDIDATES')")
    public ResponseEntity<Resource> downloadCv(@PathVariable UUID id) {
        try {
            Path filePath = candidatureService.getCvPath(id);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

