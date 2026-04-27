package com.soprarh.portail.cv.controller;

import com.soprarh.portail.cv.entity.Cv;
import com.soprarh.portail.cv.entity.DonneesCv;
import com.soprarh.portail.cv.repository.CvRepository;
import com.soprarh.portail.cv.repository.DonneesCvRepository;
import com.soprarh.portail.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller du module CV.
 * Expose les donnees extraites du CV d'une candidature.
 */
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {

    private final CvRepository cvRepository;
    private final DonneesCvRepository donneesCvRepository;

    /**
     * GET /api/cv/{candidatureId}/donnees
     * Retourne les donnees extraites du CV (competences, experiences, formations,
     * langues, certifications, soft skills, resume).
     * Si aucune extraction n'existe encore, retourne un objet vide.
     */
    @GetMapping("/{candidatureId}/donnees")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES') or hasAuthority('APPLY_OFFERS')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDonnees(@PathVariable UUID candidatureId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("competences", List.of());
        payload.put("experiences", List.of());
        payload.put("formations", List.of());
        payload.put("langues", List.of());
        payload.put("certifications", List.of());
        payload.put("softSkills", List.of());
        payload.put("texteComplet", "");

        Optional<Cv> cvOpt = cvRepository.findByCandidatureId(candidatureId);
        if (cvOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(payload, "Aucun CV pour cette candidature"));
        }
        Cv cv = cvOpt.get();

        Optional<DonneesCv> donneesOpt = donneesCvRepository.findByCvId(cv.getId());
        if (donneesOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(payload, "CV non encore analyse"));
        }
        DonneesCv d = donneesOpt.get();

        payload.put("competences", split(d.getCompetences()));
        payload.put("experiences", split(d.getExperiences()));
        payload.put("formations", split(d.getFormations()));
        payload.put("langues", split(d.getLangues()));
        payload.put("certifications", split(d.getCertifications()));
        payload.put("softSkills", split(d.getSoftSkills()));
        payload.put("texteComplet", d.getResume() != null ? d.getResume() : "");

        return ResponseEntity.ok(ApiResponse.success(payload, "Donnees CV recuperees"));
    }

    /**
     * Decoupe une chaine separee par des virgules ou retours a la ligne en liste.
     */
    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
