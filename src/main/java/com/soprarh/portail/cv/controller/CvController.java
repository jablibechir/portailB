package com.soprarh.portail.cv.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soprarh.portail.cv.entity.*;
import com.soprarh.portail.cv.repository.CvRepository;
import com.soprarh.portail.cv.repository.DonneesCvRepository;
import com.soprarh.portail.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * GET /api/cv/{candidatureId}/donnees
     * Retourne les donnees extraites du CV (competences, experiences, formations,
     * langues, certifications, soft skills, resume).
     * Uses the detail entities for properly structured data.
     */
    @GetMapping("/{candidatureId}/donnees")
    @PreAuthorize("hasAuthority('EVALUATE_CANDIDATES') or hasAuthority('APPLY_OFFERS')")
    @Transactional(readOnly = true)
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

        // Use detail entities for structured data, fall back to text columns if empty
        payload.put("competences", buildCompetences(d));
        payload.put("experiences", buildExperiences(d));
        payload.put("formations", buildFormations(d));
        payload.put("langues", buildLangues(d));
        payload.put("certifications", buildCertifications(d));
        payload.put("softSkills", buildSoftSkills(d));
        payload.put("texteComplet", d.getResume() != null ? d.getResume() : "");

        return ResponseEntity.ok(ApiResponse.success(payload, "Donnees CV recuperees"));
    }

    // ── Build structured data from detail entities ──────────────────────────

    private List<?> buildCompetences(DonneesCv d) {
        if (d.getCompetencesDetail() != null && !d.getCompetencesDetail().isEmpty()) {
            return d.getCompetencesDetail().stream()
                    .map(c -> c.getNom() != null ? c.getNom() : "")
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }
        // Fallback: competences text is comma-separated ("Java, Spring Boot, React")
        return splitClean(d.getCompetences(), "[,;\\n]");
    }

    private List<?> buildExperiences(DonneesCv d) {
        if (d.getExperiencesDetail() != null && !d.getExperiencesDetail().isEmpty()) {
            return d.getExperiencesDetail().stream().map(e -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("poste", e.getPoste() != null ? e.getPoste() : "");
                m.put("entreprise", e.getEntreprise() != null ? e.getEntreprise() : "");
                m.put("description", e.getDescription() != null ? e.getDescription() : "");
                return m;
            }).collect(Collectors.toList());
        }
        // Fallback: try JSON array, then split on newlines
        List<Map<String, Object>> json = tryParseJsonListOfMaps(d.getExperiences());
        if (json != null) {
            return json.stream().map(e -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("poste", strVal(e, "poste", "titre"));
                m.put("entreprise", strVal(e, "entreprise"));
                m.put("description", strVal(e, "description"));
                return m;
            }).collect(Collectors.toList());
        }
        return splitClean(d.getExperiences(), "[;\\n]");
    }

    private List<?> buildFormations(DonneesCv d) {
        if (d.getFormationsDetail() != null && !d.getFormationsDetail().isEmpty()) {
            return d.getFormationsDetail().stream().map(f -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("diplome", f.getDiplome() != null ? f.getDiplome() : "");
                m.put("etablissement", f.getEtablissement() != null ? f.getEtablissement() : "");
                return m;
            }).collect(Collectors.toList());
        }
        // Fallback: try JSON array, then split on newlines
        List<Map<String, Object>> json = tryParseJsonListOfMaps(d.getFormations());
        if (json != null) {
            return json.stream().map(f -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("diplome", strVal(f, "diplome", "titre"));
                m.put("etablissement", strVal(f, "etablissement"));
                return m;
            }).collect(Collectors.toList());
        }
        return splitClean(d.getFormations(), "[;\\n]");
    }

    private List<?> buildLangues(DonneesCv d) {
        if (d.getLanguesDetail() != null && !d.getLanguesDetail().isEmpty()) {
            return d.getLanguesDetail().stream().map(l -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("langue", l.getLangue() != null ? l.getLangue() : "");
                m.put("niveau", l.getNiveau() != null ? l.getNiveau() : "");
                return m;
            }).collect(Collectors.toList());
        }
        // Fallback: langues text may contain JSON array or Java toString()
        return parseLanguesFallback(d.getLangues());
    }

    private List<?> buildCertifications(DonneesCv d) {
        if (d.getCertificationsDetail() != null && !d.getCertificationsDetail().isEmpty()) {
            return d.getCertificationsDetail().stream()
                    .map(c -> c.getNom() != null ? c.getNom() : "")
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }
        // Fallback: certifications text may contain JSON array
        return parseCertificationsFallback(d.getCertifications());
    }

    private List<?> buildSoftSkills(DonneesCv d) {
        if (d.getSoftSkillsDetail() != null && !d.getSoftSkillsDetail().isEmpty()) {
            return d.getSoftSkillsDetail().stream()
                    .map(s -> s.getNom() != null ? s.getNom() : "")
                    .filter(v -> !v.isBlank())
                    .collect(Collectors.toList());
        }
        // Fallback: softSkills text may contain JSON array of strings
        return parseSoftSkillsFallback(d.getSoftSkills());
    }

    // ── Fallback parsers for text columns ───────────────────────────────────

    /**
     * Parse langues from text column.
     * Handles: JSON [{"langue":"Français","niveau":"Natif"}],
     *          Java toString [{langue=Français, niveau=Natif}],
     *          or plain text "Français;Anglais".
     */
    private List<?> parseLanguesFallback(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        // Try JSON array of objects
        List<Map<String, Object>> json = tryParseJsonListOfMaps(raw);
        if (json != null) {
            return json.stream().map(l -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("langue", strVal(l, "langue", "language", "nom"));
                m.put("niveau", strVal(l, "niveau", "level"));
                return m;
            }).filter(m -> !m.get("langue").isBlank()).collect(Collectors.toList());
        }

        // Try Java Map.toString() format: {langue=Français, niveau=Natif}
        List<Map<String, String>> fromToString = parseMapToStringEntries(raw, "langue", "niveau");
        if (!fromToString.isEmpty()) return fromToString;

        // Plain text fallback
        return splitClean(raw, "[,;\\n]");
    }

    private List<?> parseCertificationsFallback(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        // Try JSON array of objects
        List<Map<String, Object>> jsonMaps = tryParseJsonListOfMaps(raw);
        if (jsonMaps != null) {
            return jsonMaps.stream()
                    .map(c -> strVal(c, "certification", "nom", "name"))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        // Try JSON array of strings
        List<String> jsonStrings = tryParseJsonListOfStrings(raw);
        if (jsonStrings != null) return jsonStrings;

        // Try Java Map.toString() format
        List<Map<String, String>> fromToString = parseMapToStringEntries(raw, "certification", "nom");
        if (!fromToString.isEmpty()) {
            return fromToString.stream()
                    .map(m -> m.getOrDefault("certification", m.getOrDefault("nom", "")))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        return splitClean(raw, "[,;\\n]");
    }

    private List<?> parseSoftSkillsFallback(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        // Try JSON array of strings ["skill1","skill2"]
        List<String> jsonStrings = tryParseJsonListOfStrings(raw);
        if (jsonStrings != null) return jsonStrings;

        // Try JSON array of objects [{"nom":"skill"}]
        List<Map<String, Object>> jsonMaps = tryParseJsonListOfMaps(raw);
        if (jsonMaps != null) {
            return jsonMaps.stream()
                    .map(m -> strVal(m, "nom", "name", "skill"))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        return splitClean(raw, "[,;\\n]");
    }

    // ── JSON / toString parsing helpers ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryParseJsonListOfMaps(String raw) {
        if (raw == null || !raw.trim().startsWith("[")) return null;
        try {
            List<?> list = MAPPER.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            return (List<Map<String, Object>>) list;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> tryParseJsonListOfStrings(String raw) {
        if (raw == null || !raw.trim().startsWith("[")) return null;
        try {
            List<String> list = MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            return list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse Java Map.toString() format: [{key1=val1, key2=val2}, {key1=val3, key2=val4}]
     * Returns empty list if pattern doesn't match.
     */
    private List<Map<String, String>> parseMapToStringEntries(String raw, String... keys) {
        if (raw == null) return List.of();
        List<Map<String, String>> result = new ArrayList<>();
        // Match {key=value, key=value} patterns
        var matcher = java.util.regex.Pattern.compile("\\{([^}]+)\\}").matcher(raw);
        while (matcher.find()) {
            String inside = matcher.group(1);
            Map<String, String> entry = new LinkedHashMap<>();
            for (String pair : inside.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    entry.put(kv[0].trim(), kv[1].trim());
                }
            }
            if (!entry.isEmpty()) result.add(entry);
        }
        return result;
    }

    private String strVal(Map<String, ?> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return "";
    }

    /**
     * Split text into clean list of trimmed, non-empty strings.
     */
    private List<String> splitClean(String raw, String regex) {
        if (raw == null || raw.isBlank()) return List.of();
        // Strip surrounding brackets if present (leftover from toString)
        String cleaned = raw.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return Arrays.stream(cleaned.split(regex))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
