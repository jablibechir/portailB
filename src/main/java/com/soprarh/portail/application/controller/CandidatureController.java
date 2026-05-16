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
     * Accessible par: Candidat (permission POSTULER_OFFRE)
     * Accepte: multipart/form-data avec champs "offreId", "lettreMotivation" (optionnel), "cv" (optionnel)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('POSTULER_OFFRE')")
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
     * Accessible par: Candidat (permission POSTULER_OFFRE)
     */
    @GetMapping("/mes")
    @PreAuthorize("hasAuthority('POSTULER_OFFRE')")
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
     * POST /api/candidatures/{id}/transmettre?managerId=...
     * Accessible par: RH (permission TRANSMIT_TO_MANAGER ou EVALUER_CANDIDATURE)
     */
    @PostMapping("/{id}/transmettre")
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> transmettreAuManager(
            @PathVariable UUID id,
            @RequestParam UUID managerId) {

        CandidatureResponse candidature = candidatureService.transmettreAuManager(id, managerId);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature transmise au manager avec succes"));
    }

    /**
     * US-CAND-10: Rejeter une candidature (RH).
     * POST /api/candidatures/{id}/rejeter-rh
     * Accessible par: RH (permission EVALUER_CANDIDATURE)
     */
    @PostMapping("/{id}/rejeter-rh")
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> rejeterParRh(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.rejeterParRh(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature rejetee par RH"));
    }

    /**
     * Passer une candidature en evaluation RH.
     * POST /api/candidatures/{id}/evaluer
     * Accessible par: RH (permission EVALUER_CANDIDATURE)
     */
    @PostMapping("/{id}/evaluer")
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> passerEnEvaluationRh(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.passerEnEvaluationRh(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature passee en evaluation RH"));
    }

    /**
     * Changer le statut d'une candidature (generique).
     * PATCH /api/candidatures/{id}/statut
     * Accessible par: RH (permission EVALUER_CANDIDATURE)
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
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
     * Accessible par: RH (permission EVALUER_CANDIDATURE)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> listerCandidatures(
            @RequestParam(required = false, defaultValue = "date") String tri) {

        List<CandidatureResponse> candidatures = candidatureService.listerCandidatures(tri);

        return ResponseEntity.ok(
                ApiResponse.success(candidatures, "Candidatures trouvees: " + candidatures.size()));
    }

    /**
     * Filtrer les candidatures.
     * GET /api/candidatures/filtrer?statut=...&scoreMin=...&scoreMax=...
     * Accessible par: RH (permission EVALUER_CANDIDATURE)
     */
    @GetMapping("/filtrer")
    @PreAuthorize("hasAuthority('EVALUER_CANDIDATURE')")
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
     * Accessible par: RH, Manager, ou le candidat proprietaire.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CandidatureResponse>> getCandidature(
            @PathVariable UUID id,
            @AuthenticationPrincipal Utilisateur currentUser) {

        CandidatureResponse candidature = candidatureService.getCandidatureById(id);

        // Verifier les droits: RH (EVALUER_CANDIDATURE), Manager (VOIR_CANDIDATURES_ASSIGNEES), ou proprietaire
        boolean isRh = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("EVALUER_CANDIDATURE"));
        boolean isManager = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("VOIR_CANDIDATURES_ASSIGNEES"));
        boolean isOwner = currentUser.getId().equals(candidature.candidatId());

        if (!isRh && !isManager && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Acces refuse a cette candidature"));
        }

        return ResponseEntity.ok(ApiResponse.success(candidature));
    }

    // ==================== Endpoints Manager ====================

    /**
     * US-CAND-08: Valider une candidature (Manager).
     * POST /api/candidatures/{id}/valider-manager
     * Accessible par: Manager (permission NOTER_CANDIDATURE)
     */
    @PostMapping("/{id}/valider-manager")
    @PreAuthorize("hasAuthority('VALIDER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> validerParManager(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.validerParManager(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature validee par le manager"));
    }

    /**
     * US-CAND-09: Rejeter une candidature (Manager).
     * POST /api/candidatures/{id}/rejeter-manager
     * Accessible par: Manager (permission NOTER_CANDIDATURE)
     */
    @PostMapping("/{id}/rejeter-manager")
    @PreAuthorize("hasAuthority('VALIDER_CANDIDATURE')")
    public ResponseEntity<ApiResponse<CandidatureResponse>> rejeterParManager(
            @PathVariable UUID id) {

        CandidatureResponse candidature = candidatureService.rejeterParManager(id);

        return ResponseEntity.ok(
                ApiResponse.success(candidature, "Candidature rejetee par le manager"));
    }

    /**
     * US-CAND-11: Consulter les candidatures transmises au Manager.
     * GET /api/candidatures/manager
     * Accessible par: Manager (permission VOIR_CANDIDATURES_ASSIGNEES)
     */
    @GetMapping("/manager")
    @PreAuthorize("hasAuthority('VOIR_CANDIDATURES_ASSIGNEES')")
    public ResponseEntity<ApiResponse<List<CandidatureResponse>>> getCandidaturesManager(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<CandidatureResponse> candidatures = candidatureService.getCandidaturesManager(currentUser.getId());

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
    @PreAuthorize("hasAnyAuthority('EVALUER_CANDIDATURE', 'NOTER_CANDIDATURE')")
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

