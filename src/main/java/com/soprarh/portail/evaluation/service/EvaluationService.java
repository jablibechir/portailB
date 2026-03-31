package com.soprarh.portail.evaluation.service;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.entity.StatutCandidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.evaluation.dto.CreateEvaluationRequest;
import com.soprarh.portail.evaluation.dto.EvaluationResponse;
import com.soprarh.portail.evaluation.entity.DecisionEvaluation;
import com.soprarh.portail.evaluation.entity.Evaluation;
import com.soprarh.portail.evaluation.mapper.EvaluationMapper;
import com.soprarh.portail.evaluation.repository.EvaluationRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.shared.service.NotificationService;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service pour la gestion des evaluations.
 * Implemente les user stories US-EVAL-01 a US-EVAL-03.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EvaluationMapper evaluationMapper;
    private final NotificationService notificationService;

    /**
     * US-EVAL-01: Creer une evaluation (RH).
     * Le RH peut evaluer une candidature avec un commentaire et une decision.
     * La candidature doit etre en statut "soumise" ou "en_evaluation_rh".
     *
     * @param request      contient candidatureId, commentaire, decision
     * @param evaluateurId ID de l'evaluateur (RH)
     * @return l'evaluation creee
     */
    @Transactional
    public EvaluationResponse evaluerParRh(CreateEvaluationRequest request, UUID evaluateurId) {
        // 1. Verifier que la candidature existe
        Candidature candidature = findCandidatureOrThrow(request.candidatureId());

        // 2. Verifier que le statut permet l'evaluation par RH
        if (candidature.getStatut() != StatutCandidature.soumise && 
            candidature.getStatut() != StatutCandidature.en_evaluation_rh) {
            throw new BusinessException(
                    "Impossible d'evaluer: la candidature doit etre en statut 'soumise' ou 'en_evaluation_rh'. Statut actuel: " 
                    + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Recuperer l'evaluateur
        Utilisateur evaluateur = utilisateurRepository.findById(evaluateurId)
                .orElseThrow(() -> new BusinessException(
                        "Evaluateur non trouve.", HttpStatus.NOT_FOUND));

        // 4. Parser la decision
        DecisionEvaluation decision = parseDecision(request.decision());

        // 5. Creer l'evaluation
        Evaluation evaluation = Evaluation.builder()
                .candidature(candidature)
                .evaluateur(evaluateur)
                .commentaire(request.commentaire())
                .decision(decision)
                .build();

        Evaluation saved = evaluationRepository.save(evaluation);
        log.info("Evaluation creee par RH: evaluationId={}, candidatureId={}, decision={}", 
                saved.getId(), candidature.getId(), decision);

        // 6. Passer la candidature en evaluation RH si elle etait soumise
        if (candidature.getStatut() == StatutCandidature.soumise) {
            candidature.setStatut(StatutCandidature.en_evaluation_rh);
            candidatureRepository.save(candidature);
            log.info("Candidature passee en evaluation RH: {}", candidature.getId());
        }

        return evaluationMapper.toResponse(saved);
    }

    /**
     * US-EVAL-02: Creer une evaluation (Manager).
     * Le Manager peut evaluer une candidature qui lui a ete transmise.
     * La candidature doit etre en statut "envoyee_manager".
     *
     * @param request      contient candidatureId, commentaire, decision
     * @param evaluateurId ID de l'evaluateur (Manager)
     * @return l'evaluation creee
     */
    @Transactional
    public EvaluationResponse evaluerParManager(CreateEvaluationRequest request, UUID evaluateurId) {
        // 1. Verifier que la candidature existe
        Candidature candidature = findCandidatureOrThrow(request.candidatureId());

        // 2. Verifier que le statut permet l'evaluation par Manager
        if (candidature.getStatut() != StatutCandidature.envoyee_manager) {
            throw new BusinessException(
                    "Impossible d'evaluer: la candidature doit etre en statut 'envoyee_manager'. Statut actuel: " 
                    + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Recuperer l'evaluateur
        Utilisateur evaluateur = utilisateurRepository.findById(evaluateurId)
                .orElseThrow(() -> new BusinessException(
                        "Evaluateur non trouve.", HttpStatus.NOT_FOUND));

        // 4. Parser la decision
        DecisionEvaluation decision = parseDecision(request.decision());

        // 5. Creer l'evaluation
        Evaluation evaluation = Evaluation.builder()
                .candidature(candidature)
                .evaluateur(evaluateur)
                .commentaire(request.commentaire())
                .decision(decision)
                .build();

        Evaluation saved = evaluationRepository.save(evaluation);
        log.info("Evaluation creee par Manager: evaluationId={}, candidatureId={}, decision={}", 
                saved.getId(), candidature.getId(), decision);

        // Notification RH -> evaluation manager recue
        notificationService.notifierRhEvaluationManager(candidature, evaluateur, decision.name());

        return evaluationMapper.toResponse(saved);
    }

    /**
     * US-EVAL-03: Consulter toutes les evaluations d'une candidature.
     * Le RH (ou Manager) peut voir toutes les evaluations d'une candidature.
     *
     * @param candidatureId ID de la candidature
     * @return liste des evaluations
     */
    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationsByCandidature(UUID candidatureId) {
        // Verifier que la candidature existe
        findCandidatureOrThrow(candidatureId);

        List<Evaluation> evaluations = evaluationRepository
                .findByCandidatureIdOrderByDateEvaluationDesc(candidatureId);
        
        return evaluationMapper.toResponseList(evaluations);
    }

    /**
     * Consulter les evaluations faites par un evaluateur.
     *
     * @param evaluateurId ID de l'evaluateur
     * @return liste des evaluations
     */
    @Transactional(readOnly = true)
    public List<EvaluationResponse> getMesEvaluations(UUID evaluateurId) {
        List<Evaluation> evaluations = evaluationRepository
                .findByEvaluateurIdOrderByDateEvaluationDesc(evaluateurId);
        
        return evaluationMapper.toResponseList(evaluations);
    }

    /**
     * Recuperer une evaluation par son ID.
     *
     * @param id ID de l'evaluation
     * @return l'evaluation
     */
    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluationById(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Evaluation non trouvee avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));

        return evaluationMapper.toResponse(evaluation);
    }

    // ==================== Methodes privees ====================

    private Candidature findCandidatureOrThrow(UUID id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Candidature non trouvee avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));
    }

    private DecisionEvaluation parseDecision(String decision) {
        try {
            return DecisionEvaluation.valueOf(decision.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Decision invalide: " + decision + ". Valeurs acceptees: pour_suivre, a_revoir, rejeter",
                    HttpStatus.BAD_REQUEST);
        }
    }
}

