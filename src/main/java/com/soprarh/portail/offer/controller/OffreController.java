package com.soprarh.portail.offer.controller;

import com.soprarh.portail.offer.dto.*;
import com.soprarh.portail.offer.service.OffreService;
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
 * Controller REST pour la gestion des offres d'emploi.
 * Implemente les endpoints pour US-OFF-01 a US-OFF-05.
 */
@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreController {

    private final OffreService offreService;

    // ==================== Endpoints Admin/RH ====================

    /**
     * US-OFF-01: Creer une offre d'emploi.
     * POST /api/offres
     * Accessible par: RH (permission MANAGE_OFFERS)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<OffreResponse>> createOffre(
            @Valid @RequestBody CreateOffreRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        OffreResponse offre = offreService.createOffre(request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(offre, "Offre creee avec succes"));
    }

    /**
     * US-OFF-02: Modifier une offre d'emploi.
     * PUT /api/offres/{id}
     * Accessible par: RH (permission MANAGE_OFFERS)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<OffreResponse>> updateOffre(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOffreRequest request) {

        OffreResponse offre = offreService.updateOffre(id, request);

        return ResponseEntity.ok(ApiResponse.success(offre, "Offre mise a jour avec succes"));
    }

    /**
     * US-OFF-03: Supprimer une offre d'emploi.
     * DELETE /api/offres/{id}
     * Accessible par: RH (permission MANAGE_OFFERS)
     * Note: La confirmation est geree cote frontend.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<Void>> deleteOffre(@PathVariable UUID id) {
        offreService.deleteOffre(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Offre supprimee avec succes"));
    }

    /**
     * US-OFF-04: Changer le statut d'une offre.
     * PATCH /api/offres/{id}/statut
     * Accessible par: RH (permission MANAGE_OFFERS)
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<OffreResponse>> changeStatut(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatutRequest request) {

        OffreResponse offre = offreService.changeStatut(id, request);

        return ResponseEntity.ok(ApiResponse.success(offre, "Statut de l'offre mis a jour"));
    }

    /**
     * Recuperer toutes les offres (tous statuts).
     * GET /api/offres/admin
     * Accessible par: RH (permission MANAGE_OFFERS)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getAllOffres() {
        List<OffreResponse> offres = offreService.getAllOffres();

        return ResponseEntity.ok(ApiResponse.success(offres, "Liste des offres recuperee"));
    }

    /**
     * Recuperer une offre par ID (pour edition).
     * GET /api/offres/admin/{id}
     * Accessible par: RH (permission MANAGE_OFFERS)
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('MANAGE_OFFERS')")
    public ResponseEntity<ApiResponse<OffreResponse>> getOffreById(@PathVariable UUID id) {
        OffreResponse offre = offreService.getOffreById(id);

        return ResponseEntity.ok(ApiResponse.success(offre));
    }

    // ==================== Endpoints Candidat ====================

    /**
     * US-OFF-05: Filtrer les offres publiees.
     * GET /api/offres?keyword=...&experience=...&formation=...
     * Accessible par: tous les utilisateurs authentifies (permission VIEW_OFFERS)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_OFFERS')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> searchOffres(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String formation) {

        OffreFilterRequest filter = new OffreFilterRequest(keyword, experience, formation);
        List<OffreResponse> offres = offreService.searchOffresPubliees(filter);

        return ResponseEntity.ok(ApiResponse.success(offres, "Offres trouvees: " + offres.size()));
    }

    /**
     * Recuperer une offre publiee par ID.
     * GET /api/offres/{id}
     * Accessible par: tous les utilisateurs authentifies (permission VIEW_OFFERS)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_OFFERS')")
    public ResponseEntity<ApiResponse<OffreResponse>> getOffrePubliee(@PathVariable UUID id) {
        OffreResponse offre = offreService.getOffreById(id);

        return ResponseEntity.ok(ApiResponse.success(offre));
    }
}

