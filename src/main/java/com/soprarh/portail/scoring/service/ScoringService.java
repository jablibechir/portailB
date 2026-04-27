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
     */
    @Transactional
    public CritereResponse createCritere(UUID offreId, CritereRequest request) {
        Utilisateur currentUser = getCurrentUser();

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
     */
    @Transactional
    public CritereResponse updateCritere(UUID critereId, CritereRequest request) {
        CritereScoring critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new BusinessException("Critere non trouve: " + critereId, HttpStatus.NOT_FOUND));

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
     * Scoring détaillé avec critères — appelle /api/score-criteres du microservice Python.
     */
    @SuppressWarnings("unchecked")
    private ScoringResultResponse scoreWithCriteres(
            Candidature candidature, String cvText, OffreEmploi offre, List<CritereScoring> criteres) {

        // Construire le texte de référence pour chaque critère en fonction de son type
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

        // Sauvegarder les détails par critère
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

        // Mettre à jour le score dans la candidature
        candidature.setScoreTotal(scoreTotal);
        candidatureRepository.save(candidature);

        log.info("Scoring detaille termine pour candidature {}: score = {}", candidature.getId(), scoreTotal);

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
            case LANGUES -> critere.getNom();
            case CERTIFICATIONS -> critere.getNom();
            case SOFT_SKILLS -> critere.getNom();
        };
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

