package com.soprarh.portail.evaluation.repository;

import com.soprarh.portail.evaluation.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la table "evaluations".
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    /**
     * Trouve toutes les evaluations d'une candidature.
     */
    List<Evaluation> findByCandidatureIdOrderByDateEvaluationDesc(UUID candidatureId);

    /**
     * Trouve les evaluations faites par un evaluateur.
     */
    List<Evaluation> findByEvaluateurIdOrderByDateEvaluationDesc(UUID evaluateurId);
}

