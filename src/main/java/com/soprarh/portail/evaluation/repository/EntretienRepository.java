package com.soprarh.portail.evaluation.repository;

import com.soprarh.portail.evaluation.entity.Entretien;
import com.soprarh.portail.evaluation.entity.StatutEntretien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la table "entretiens".
 */
@Repository
public interface EntretienRepository extends JpaRepository<Entretien, UUID> {

    /**
     * Trouve tous les entretiens d'une candidature.
     */
    List<Entretien> findByCandidatureIdOrderByDateEntretienAsc(UUID candidatureId);

    /**
     * Trouve les entretiens planifies par un utilisateur.
     */
    List<Entretien> findByPlanifieParIdOrderByDateEntretienAsc(UUID planifieParId);

    /**
     * Trouve les entretiens a venir pour un candidat.
     */
    @Query("""
        SELECT e FROM Entretien e
        JOIN e.candidature c
        WHERE c.candidat.id = :candidatId
        AND e.dateEntretien > :now
        AND e.statut IN (:statuts)
        ORDER BY e.dateEntretien ASC
    """)
    List<Entretien> findUpcomingEntretiensByCandidatId(
            @Param("candidatId") UUID candidatId,
            @Param("now") LocalDateTime now,
            @Param("statuts") List<StatutEntretien> statuts);

    /**
     * Trouve les entretiens par statut.
     */
    List<Entretien> findByStatutOrderByDateEntretienAsc(StatutEntretien statut);
}

