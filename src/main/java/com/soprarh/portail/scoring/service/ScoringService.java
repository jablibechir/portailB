package com.soprarh.portail.scoring.service;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.scoring.dto.*;
import com.soprarh.portail.scoring.entity.*;
import com.soprarh.portail.scoring.repository.CritereScoringRepository;
import com.soprarh.portail.scoring.repository.ResultatScoringRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de scoring — gère les critères et appelle le microservice Python IA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final CritereScoringRepository critereRepository;
    private final ResultatScoringRepository resultatRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WebClient aiWebClient;

    // ===== CRITERES CRUD =====

    /**
     * Récupérer tous les critères d'une offre.
     */
    public List<CritereResponse> getCriteresByOffre(UUID offreId) {
        return critereRepository.findByOffreId(offreId).stream()
                .map(this::toCritereResponse)
                .collect(Collectors.toList());
    }

    /**
     * Créer un nouveau critère pour une offre.
     * Règles métier :
     * - Maximum 6 critères par offre (un par type)
     * - Chaque type ne peut apparaître qu'une seule fois par offre
     */
    @Transactional
    public CritereResponse createCritere(UUID offreId, CritereRequest request) {
        Utilisateur currentUser = getCurrentUser();

        // Vérifier max 6 critères par offre
        long count = critereRepository.countByOffreId(offreId);
        if (count >= 6) {
            throw new BusinessException(
                    "Cette offre a déjà 6 critères (maximum atteint). Chaque offre ne peut avoir qu'un critère par type.",
                    HttpStatus.BAD_REQUEST);
        }

        // Vérifier unicité du type par offre
        critereRepository.findByOffreIdAndType(offreId, request.type()).ifPresent(existing -> {
            throw new BusinessException(
                    "Un critère de type " + request.type() + " existe déjà pour cette offre.",
                    HttpStatus.CONFLICT);
        });

        CritereScoring critere = CritereScoring.builder()
                .nom(request.nom())
                .poids(request.poids())
                .type(request.type())
                .creePar(currentUser)
                .build();

        OffreEmploi offre = new OffreEmploi();
        offre.setId(offreId);
        critere.setOffre(offre);

        critere = critereRepository.save(critere);
        log.info("Critere de scoring cree: {} pour offre {}", critere.getNom(), offreId);
        return toCritereResponse(critere);
    }

    /**
     * Modifier un critère existant.
     * Règle métier : si le type change, vérifier l'unicité du nouveau type pour l'offre.
     */
    @Transactional
    public CritereResponse updateCritere(UUID critereId, CritereRequest request) {
        CritereScoring critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new BusinessException("Critere non trouve: " + critereId, HttpStatus.NOT_FOUND));

        // Si le type change, vérifier unicité
        if (request.type() != critere.getType()) {
            UUID offreId = critere.getOffre().getId();
            critereRepository.findByOffreIdAndType(offreId, request.type()).ifPresent(existing -> {
                throw new BusinessException(
                        "Un critère de type " + request.type() + " existe déjà pour cette offre.",
                        HttpStatus.CONFLICT);
            });
        }

        critere.setNom(request.nom());
        critere.setPoids(request.poids());
        critere.setType(request.type());

        critere = critereRepository.save(critere);
        return toCritereResponse(critere);
    }

    /**
     * Supprimer un critère.
     */
    @Transactional
    public void deleteCritere(UUID critereId) {
        if (!critereRepository.existsById(critereId)) {
            throw new BusinessException("Critere non trouve: " + critereId, HttpStatus.NOT_FOUND);
        }
        critereRepository.deleteById(critereId);
    }

    /**
     * Vérifier si les critères d'une offre sont valides (total poids = 100).
     * Retourne true si valide, false sinon.
     */
    @Transactional(readOnly = true)
    public boolean isCriteresValid(UUID offreId) {
        List<CritereScoring> criteres = critereRepository.findByOffreId(offreId);
        if (criteres.isEmpty()) return false;
        double total = criteres.stream().mapToDouble(CritereScoring::getPoids).sum();
        return Math.abs(total - 100.0) < 0.01;
    }

    // ===== SCORING IA =====

    /**
     * Lancer le scoring d'une candidature.
     * 1. Récupère le texte du CV (depuis donnees_cv)
     * 2. Récupère les critères de l'offre
     * 3. Appelle le microservice Python pour calculer les scores
     * 4. Sauvegarde le résultat en base
     */
    @Transactional
    public ScoringResultResponse scoreCandidature(UUID candidatureId) {
        // 1. Récupérer la candidature avec offre et candidat
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new BusinessException("Candidature non trouvee: " + candidatureId, HttpStatus.NOT_FOUND));

        OffreEmploi offre = candidature.getOffre();
        if (offre == null) {
            throw new BusinessException("Aucune offre associee a cette candidature", HttpStatus.BAD_REQUEST);
        }

        // 2. Récupérer le texte du CV
        String cvText = extractCvText(candidature);
        if (cvText == null || cvText.isBlank()) {
            throw new BusinessException("Aucun texte de CV disponible pour cette candidature", HttpStatus.BAD_REQUEST);
        }

        // 3. Récupérer les critères de l'offre
        List<CritereScoring> criteres = critereRepository.findByOffreId(offre.getId());

        ScoringResultResponse result;

        if (criteres.isEmpty()) {
            // Pas de critères définis → scoring global simple
            result = scoreGlobal(candidature, cvText, offre);
        } else {
            // Critères définis → scoring détaillé par critère
            result = scoreWithCriteres(candidature, cvText, offre, criteres);
        }

        return result;
    }

    /**
     * Consulter le résultat de scoring d'une candidature.
     * <p>
     * {@code @Transactional(readOnly = true)} : maintient la session Hibernate ouverte
     * afin de pouvoir initialiser la collection lazy {@code resultatCriteres}.
     */
    @Transactional(readOnly = true)
    public ScoringResultResponse getScoreForCandidature(UUID candidatureId) {
        ResultatScoring resultat = resultatRepository
                .findTopByCandidatureIdOrderByDateCalculDesc(candidatureId)
                .orElseThrow(() -> new BusinessException("Aucun resultat de scoring pour cette candidature", HttpStatus.NOT_FOUND));

        Candidature candidature = resultat.getCandidature();

        // Force l'initialisation tant que la session est ouverte
        List<CritereScoreDetail> details = resultat.getResultatCriteres() == null
                ? List.of()
                : resultat.getResultatCriteres().stream()
                .map(rc -> new CritereScoreDetail(
                        rc.getCritere().getId(),
                        rc.getCritere().getNom(),
                        rc.getCritere().getType(),
                        rc.getCritere().getPoids(),
                        rc.getScoreObtenu()
                ))
                .collect(Collectors.toList());

        return new ScoringResultResponse(
                candidatureId,
                candidature.getCandidat().getNom(),
                candidature.getCandidat().getPrenom(),
                resultat.getScoreTotal(),
                details
        );
    }

    // ===== PRIVATE =====

    /**
     * Scoring global sans critères — appelle /api/score du microservice Python.
     */
    @SuppressWarnings("unchecked")
    private ScoringResultResponse scoreGlobal(Candidature candidature, String cvText, OffreEmploi offre) {
        Map<String, String> body = Map.of(
                "cv_text", cvText,
                "offre_titre", offre.getTitre() != null ? offre.getTitre() : "",
                "offre_description", offre.getDescription() != null ? offre.getDescription() : "",
                "offre_competences", offre.getCompetencesRequises() != null ? offre.getCompetencesRequises() : "",
                "offre_experience", offre.getExperienceRequise() != null ? offre.getExperienceRequise() : "",
                "offre_formation", offre.getFormationRequise() != null ? offre.getFormationRequise() : ""
        );

        Map<String, Object> response = aiWebClient.post()
                .uri("/api/score")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Double scoreTotal = response != null ? ((Number) response.get("score_total")).doubleValue() : 0.0;

        // Sauvegarder le résultat
        ResultatScoring resultat = ResultatScoring.builder()
                .candidature(candidature)
                .scoreTotal(scoreTotal)
                .build();
        resultatRepository.save(resultat);

        // Mettre à jour le score dans la candidature
        candidature.setScoreTotal(scoreTotal);
        candidatureRepository.save(candidature);

        log.info("Scoring global termine pour candidature {}: score = {}", candidature.getId(), scoreTotal);

        return new ScoringResultResponse(
                candidature.getId(),
                candidature.getCandidat().getNom(),
                candidature.getCandidat().getPrenom(),
                scoreTotal,
                List.of()
        );
    }

    /**
     * Scoring détaillé avec critères — appelle /api/score-fields (field-to-field) du microservice Python.
     * Chaque champ CV est comparé individuellement au champ offre correspondant via le type du critère.
     */
    @SuppressWarnings("unchecked")
    private ScoringResultResponse scoreWithCriteres(
            Candidature candidature, String cvText, OffreEmploi offre, List<CritereScoring> criteres) {

        // Construire les champs CV séparés depuis donnees_cv
        Map<String, String> cvFields = extractCvFields(candidature);

        // Log CV fields for debugging
        log.info("Scoring candidature {}: CV fields extracted = {}", 
                candidature.getId(), 
                cvFields.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        e -> e.getValue() != null && !e.getValue().isEmpty() ? e.getValue().length() + " chars" : "EMPTY"
                    )));

        // Construire les champs offre séparés
        Map<String, String> offreFields = new HashMap<>();
        offreFields.put("competences_requises", offre.getCompetencesRequises() != null ? offre.getCompetencesRequises() : "");
        offreFields.put("experience_requise", offre.getExperienceRequise() != null ? offre.getExperienceRequise() : "");
        offreFields.put("formation_requise", offre.getFormationRequise() != null ? offre.getFormationRequise() : "");
        offreFields.put("langues_requises", offre.getLanguesRequises() != null ? offre.getLanguesRequises() : "");
        offreFields.put("certifications_requises", offre.getCertificationsRequises() != null ? offre.getCertificationsRequises() : "");
        offreFields.put("soft_skills_requis", offre.getSoftSkillsRequis() != null ? offre.getSoftSkillsRequis() : "");

        // Log offre fields for debugging
        log.info("Scoring offre {}: Offre fields = {}", 
                offre.getId(),
                offreFields.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        e -> e.getValue() != null && !e.getValue().isEmpty() ? e.getValue().length() + " chars" : "EMPTY"
                    )));

        // Construire la liste des critères
        List<Map<String, Object>> criteresPayload = criteres.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("critere_id", c.getId().toString());
            m.put("critere_nom", c.getNom());
            m.put("poids", c.getPoids());
            m.put("type", c.getType() != null ? c.getType().name() : "COMPETENCES");
            return m;
        }).collect(Collectors.toList());

        log.info("Scoring with {} criteres: {}", criteres.size(), 
                criteres.stream().map(c -> c.getType() + "(" + c.getPoids() + "%)").collect(Collectors.joining(", ")));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cv_fields", cvFields);
        requestBody.put("offre_fields", offreFields);
        requestBody.put("criteres", criteresPayload);

        Map<String, Object> response;
        try {
            response = aiWebClient.post()
                    .uri("/api/score-fields")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.warn("Field-to-field scoring failed, falling back to legacy endpoint: {}", e.getMessage());
            return scoreWithCriteresLegacy(candidature, cvText, offre, criteres);
        }

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            log.warn("AI service returned failure, falling back to legacy endpoint");
            return scoreWithCriteresLegacy(candidature, cvText, offre, criteres);
        }

        Double scoreTotal = ((Number) response.get("score_total")).doubleValue();
        List<Map<String, Object>> details = (List<Map<String, Object>>) response.get("details");
        Boolean needsReview = (Boolean) response.get("needs_review");

        // Log the result
        log.info("Scoring result for candidature {}: score_total={}, needs_review={}", 
                candidature.getId(), scoreTotal, needsReview);

        // Sauvegarder le résultat
        ResultatScoring resultat = ResultatScoring.builder()
                .candidature(candidature)
                .scoreTotal(scoreTotal)
                .build();
        resultat = resultatRepository.save(resultat);

        // Sauvegarder les détails par critère
        List<CritereScoreDetail> detailDtos = new ArrayList<>();
        if (details != null) {
            for (Map<String, Object> d : details) {
                UUID critereId = UUID.fromString((String) d.get("critere_id"));
                Double scoreObtenu = ((Number) d.get("score_obtenu")).doubleValue();
                String critereNom = (String) d.get("critere_nom");
                Double poids = ((Number) d.get("poids")).doubleValue();

                CritereScoring critere = critereRepository.findById(critereId).orElse(null);
                if (critere != null) {
                    ResultatCritere rc = ResultatCritere.builder()
                            .resultat(resultat)
                            .critere(critere)
                            .scoreObtenu(scoreObtenu)
                            .build();
                    resultat.getResultatCriteres().add(rc);

                    detailDtos.add(new CritereScoreDetail(
                            critereId, critereNom, critere.getType(), poids, scoreObtenu
                    ));
                }
            }
        }
        resultatRepository.save(resultat);

        // Mettre à jour le score dans la candidature
        candidature.setScoreTotal(scoreTotal);
        candidatureRepository.save(candidature);

        if (Boolean.TRUE.equals(needsReview)) {
            log.info("Scoring pour candidature {} marque pour revision manuelle (score={})", candidature.getId(), scoreTotal);
        }
        log.info("Scoring field-to-field termine pour candidature {}: score = {}", candidature.getId(), scoreTotal);

        return new ScoringResultResponse(
                candidature.getId(),
                candidature.getCandidat().getNom(),
                candidature.getCandidat().getPrenom(),
                scoreTotal,
                detailDtos
        );
    }

    /**
     * Fallback : scoring par critères via l'ancien endpoint /api/score-criteres.
     * Utilisé si le nouveau endpoint échoue.
     */
    @SuppressWarnings("unchecked")
    private ScoringResultResponse scoreWithCriteresLegacy(
            Candidature candidature, String cvText, OffreEmploi offre, List<CritereScoring> criteres) {

        List<Map<String, Object>> criteresPayload = criteres.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("critere_id", c.getId().toString());
            m.put("critere_nom", c.getNom());
            m.put("poids", c.getPoids());
            m.put("texte_reference", buildTextForCritere(c, offre));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> requestBody = Map.of(
                "cv_text", cvText,
                "criteres", criteresPayload
        );

        Map<String, Object> response = aiWebClient.post()
                .uri("/api/score-criteres")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Double scoreTotal = response != null ? ((Number) response.get("score_total")).doubleValue() : 0.0;
        List<Map<String, Object>> details = response != null
                ? (List<Map<String, Object>>) response.get("details")
                : List.of();

        // Sauvegarder le résultat
        ResultatScoring resultat = ResultatScoring.builder()
                .candidature(candidature)
                .scoreTotal(scoreTotal)
                .build();
        resultat = resultatRepository.save(resultat);

        List<CritereScoreDetail> detailDtos = new ArrayList<>();
        for (Map<String, Object> d : details) {
            UUID critereId = UUID.fromString((String) d.get("critere_id"));
            Double scoreObtenu = ((Number) d.get("score_obtenu")).doubleValue();
            String critereNom = (String) d.get("critere_nom");
            Double poids = ((Number) d.get("poids")).doubleValue();

            CritereScoring critere = critereRepository.findById(critereId).orElse(null);
            if (critere != null) {
                ResultatCritere rc = ResultatCritere.builder()
                        .resultat(resultat)
                        .critere(critere)
                        .scoreObtenu(scoreObtenu)
                        .build();
                resultat.getResultatCriteres().add(rc);

                detailDtos.add(new CritereScoreDetail(
                        critereId, critereNom, critere.getType(), poids, scoreObtenu
                ));
            }
        }
        resultatRepository.save(resultat);

        candidature.setScoreTotal(scoreTotal);
        candidatureRepository.save(candidature);

        log.info("Scoring legacy termine pour candidature {}: score = {}", candidature.getId(), scoreTotal);

        return new ScoringResultResponse(
                candidature.getId(),
                candidature.getCandidat().getNom(),
                candidature.getCandidat().getPrenom(),
                scoreTotal,
                detailDtos
        );
    }

    /**
     * Construit le texte de référence d'un critère en fonction de son type.
     * Utilisé pour le fallback legacy endpoint.
     */
    private String buildTextForCritere(CritereScoring critere, OffreEmploi offre) {
        if (critere.getType() == null) return critere.getNom();

        return switch (critere.getType()) {
            case COMPETENCES -> offre.getCompetencesRequises() != null
                    ? offre.getCompetencesRequises() : critere.getNom();
            case EXPERIENCE -> offre.getExperienceRequise() != null
                    ? offre.getExperienceRequise() : critere.getNom();
            case FORMATION -> offre.getFormationRequise() != null
                    ? offre.getFormationRequise() : critere.getNom();
            case LANGUES -> offre.getLanguesRequises() != null
                    ? offre.getLanguesRequises() : critere.getNom();
            case CERTIFICATIONS -> offre.getCertificationsRequises() != null
                    ? offre.getCertificationsRequises() : critere.getNom();
            case SOFT_SKILLS -> offre.getSoftSkillsRequis() != null
                    ? offre.getSoftSkillsRequis() : critere.getNom();
        };
    }

    /**
     * Extrait les champs CV individuels depuis les données parsées (donnees_cv).
     * Retourne un Map compatible avec le format attendu par /api/score-fields.
     */
    private Map<String, String> extractCvFields(Candidature candidature) {
        Map<String, String> fields = new HashMap<>();
        fields.put("competences", "");
        fields.put("experiences", "");
        fields.put("formations", "");
        fields.put("langues", "");
        fields.put("certifications", "");
        fields.put("soft_skills", "");

        if (candidature.getCv() == null) {
            log.warn("extractCvFields: CV is null for candidature {}", candidature.getId());
            return fields;
        }

        try {
            var donneesCv = candidature.getCv().getDonneesCv();
            if (donneesCv == null) {
                log.warn("extractCvFields: DonneesCv is null for candidature {} (CV id={})", 
                        candidature.getId(), candidature.getCv().getId());
                return fields;
            }

            log.debug("extractCvFields: Found donneesCv id={} for candidature {}", 
                    donneesCv.getId(), candidature.getId());

            if (donneesCv.getCompetences() != null) fields.put("competences", donneesCv.getCompetences());
            if (donneesCv.getExperiences() != null) fields.put("experiences", donneesCv.getExperiences());
            if (donneesCv.getFormations() != null) fields.put("formations", donneesCv.getFormations());
            
            // Langues, certifications, softSkills sont stockées en JSON - les convertir en texte
            if (donneesCv.getLangues() != null) {
                fields.put("langues", parseJsonArrayToText(donneesCv.getLangues(), "langue", "niveau"));
            }
            if (donneesCv.getCertifications() != null) {
                fields.put("certifications", parseJsonArrayToText(donneesCv.getCertifications(), "certification", null));
            }
            if (donneesCv.getSoftSkills() != null) {
                fields.put("soft_skills", parseJsonArrayToTextSimple(donneesCv.getSoftSkills()));
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire les champs CV pour candidature {}: {}", 
                    candidature.getId(), e.getMessage(), e);
        }
        return fields;
    }

    /**
     * Parse un JSON array de type [{"key": "value", "key2": "value2"}] et retourne une chaîne de texte.
     * Si secondKey est fourni, combine les valeurs: "value1 value2, value1 value2, ..."
     */
    @SuppressWarnings("unchecked")
    private String parseJsonArrayToText(String json, String primaryKey, String secondaryKey) {
        if (json == null || json.isBlank()) return "";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(json, List.class);
            return list.stream()
                    .map(m -> {
                        String primary = m.get(primaryKey) != null ? m.get(primaryKey).toString() : "";
                        if (secondaryKey != null && m.get(secondaryKey) != null) {
                            return primary + " " + m.get(secondaryKey).toString();
                        }
                        return primary;
                    })
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.debug("Could not parse JSON array: {}, returning raw value", e.getMessage());
            return json; // Retourne la valeur brute si parsing échoue
        }
    }

    /**
     * Parse un JSON array de strings ["value1", "value2"] et retourne une chaîne de texte.
     */
    @SuppressWarnings("unchecked")
    private String parseJsonArrayToTextSimple(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> list = mapper.readValue(json, List.class);
            return list.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.debug("Could not parse JSON string array: {}, returning raw value", e.getMessage());
            return json; // Retourne la valeur brute si parsing échoue
        }
    }

    /**
     * Extrait le texte du CV depuis les données parsées (donnees_cv).
     */
    private String extractCvText(Candidature candidature) {
        if (candidature.getCv() == null) return null;

        var cv = candidature.getCv();
        // Accéder aux données CV parsées
        try {
            // Utilisation d'une requête native ou accès via le repository
            // Pour simplifier, on construit le texte depuis les champs disponibles
            var donneesCv = cv.getDonneesCv();
            if (donneesCv == null) return null;

            StringBuilder sb = new StringBuilder();
            if (donneesCv.getCompetences() != null) sb.append(donneesCv.getCompetences()).append(" ");
            if (donneesCv.getExperiences() != null) sb.append(donneesCv.getExperiences()).append(" ");
            if (donneesCv.getFormations() != null) sb.append(donneesCv.getFormations()).append(" ");
            if (donneesCv.getLangues() != null) sb.append(donneesCv.getLangues()).append(" ");
            if (donneesCv.getCertifications() != null) sb.append(donneesCv.getCertifications()).append(" ");
            if (donneesCv.getSoftSkills() != null) sb.append(donneesCv.getSoftSkills()).append(" ");
            if (donneesCv.getResume() != null) sb.append(donneesCv.getResume());
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Impossible d'extraire le texte du CV pour candidature {}", candidature.getId(), e);
            return null;
        }
    }

    private CritereResponse toCritereResponse(CritereScoring critere) {
        return new CritereResponse(
                critere.getId(),
                critere.getNom(),
                critere.getPoids(),
                critere.getType(),
                critere.getOffre() != null ? critere.getOffre().getId() : null
        );
    }

    private Utilisateur getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouve", HttpStatus.UNAUTHORIZED));
    }
}

