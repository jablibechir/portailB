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
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
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
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<OffreResponse>> updateOffre(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOffreRequest request) {

        OffreResponse offre = offreService.updateOffre(id, request);

        return ResponseEntity.ok(ApiResponse.success(offre, "Offre mise a jour avec succes"));
    }

    /**
     * US-OFF-03: Supprimer une offre d'emploi.
     * DELETE /api/offres/{id}
     * Accessible par: RH (permission GERER_OFFRES)
     * Note: La confirmation est geree cote frontend.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<Void>> deleteOffre(@PathVariable UUID id) {
        offreService.deleteOffre(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Offre supprimee avec succes"));
    }

    /**
     * US-OFF-04: Changer le statut d'une offre.
     * PATCH /api/offres/{id}/statut
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<OffreResponse>> changeStatut(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatutRequest request) {

        OffreResponse offre = offreService.changeStatut(id, request);

        return ResponseEntity.ok(ApiResponse.success(offre, "Statut de l'offre mis a jour"));
    }

    /**
     * Recuperer toutes les offres (tous statuts).
     * GET /api/offres/admin
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getAllOffres() {
        List<OffreResponse> offres = offreService.getAllOffres();

        return ResponseEntity.ok(ApiResponse.success(offres, "Liste des offres recuperee"));
    }

    /**
     * Recuperer une offre par ID (pour edition).
     * GET /api/offres/admin/{id}
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<OffreResponse>> getOffreById(@PathVariable UUID id) {
        OffreResponse offre = offreService.getOffreById(id);

        return ResponseEntity.ok(ApiResponse.success(offre));
    }

    // ==================== Endpoints Publics (Candidat + Visiteur) ====================

    /**
     * US-OFF-05: Rechercher et filtrer les offres publiees.
     * GET /api/offres?keyword=...&experience=...&formation=...&days=7&typeEmploi=Stage
     * Accessible par: tous les utilisateurs authentifies (permission VIEW_OFFERS).
     * Retourne uniquement les offres publiees.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OffreResponse>>> searchOffresPubliees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String formation,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String typeEmploi) {

        OffreFilterRequest filter = new OffreFilterRequest(keyword, experience, formation, days, typeEmploi);
        List<OffreResponse> offres = offreService.searchOffresPubliees(filter);

        return ResponseEntity.ok(ApiResponse.success(offres, "Offres trouvees: " + offres.size()));
    }

    /**
     * Recuperer une offre publiee par ID.
     * GET /api/offres/{id}
     * Accessible par: tous les utilisateurs authentifies (permission VIEW_OFFERS)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OffreResponse>> getOffrePubliee(@PathVariable UUID id) {
        OffreResponse offre = offreService.getOffreById(id);

        return ResponseEntity.ok(ApiResponse.success(offre));
    }

    // ==================== Endpoints Manager — Recommandations ====================

    /**
     * Manager: Recommander un nouveau poste.
     * POST /api/offres/recommandations
     * Accessible par: Manager (permission RECOMMANDER_OFFRE)
     */
    @PostMapping("/recommandations")
    @PreAuthorize("hasAuthority('RECOMMANDER_OFFRE')")
    public ResponseEntity<ApiResponse<OffreResponse>> recommanderPoste(
            @Valid @RequestBody RecommandationRequest request,
            @AuthenticationPrincipal Utilisateur currentUser) {

        OffreResponse offre = offreService.recommanderPoste(request, currentUser.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(offre, "Recommandation creee avec succes"));
    }

    /**
     * Manager: Voir ses recommandations.
     * GET /api/offres/recommandations/mes
     * Accessible par: Manager (permission RECOMMANDER_OFFRE)
     */
    @GetMapping("/recommandations/mes")
    @PreAuthorize("hasAuthority('RECOMMANDER_OFFRE')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getMesRecommandations(
            @AuthenticationPrincipal Utilisateur currentUser) {

        List<OffreResponse> offres = offreService.getMesRecommandations(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(offres, "Mes recommandations: " + offres.size()));
    }

    /**
     * RH: Voir les recommandations en attente.
     * GET /api/offres/recommandations/en-attente
     * Accessible par: RH (permission GERER_OFFRES)
     */
    @GetMapping("/recommandations/en-attente")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<List<OffreResponse>>> getRecommandationsEnAttente() {

        List<OffreResponse> offres = offreService.getRecommandationsEnAttente();

        return ResponseEntity.ok(ApiResponse.success(offres, "Recommandations en attente: " + offres.size()));
    }

    /**
     * RH: Traiter une recommandation (publier / brouillon / refuser).
     * POST /api/offres/recommandations/{id}/traiter
     * Accessible par: RH (permission GERER_OFFRES)
     * Body: { "action": "publier|brouillon|refuser", "motifRefus": "...", "updates": {...} }
     */
    @PostMapping("/recommandations/{id}/traiter")
    @PreAuthorize("hasAuthority('GERER_OFFRES')")
    public ResponseEntity<ApiResponse<OffreResponse>> traiterRecommandation(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body) {

        String action = (String) body.get("action");
        String motifRefus = (String) body.get("motifRefus");

        // Parse optional updates
        UpdateOffreRequest updates = null;
        if (body.containsKey("titre") || body.containsKey("description")) {
            updates = new UpdateOffreRequest(
                    (String) body.get("titre"),
                    (String) body.get("description"),
                    (String) body.get("competencesRequises"),
                    (String) body.get("experienceRequise"),
                    (String) body.get("formationRequise"),
                    (String) body.get("languesRequises"),
                    (String) body.get("certificationsRequises"),
                    (String) body.get("softSkillsRequis"),
                    null, // dateExpiration
                    (String) body.get("typeEmploi")
            );
        }

        OffreResponse offre = offreService.traiterRecommandation(id, action, updates, motifRefus);

        return ResponseEntity.ok(ApiResponse.success(offre, "Recommandation traitee avec succes"));
    }
}

