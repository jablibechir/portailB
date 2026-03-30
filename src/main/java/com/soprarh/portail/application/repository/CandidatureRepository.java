package com.soprarh.portail.application.repository;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.entity.StatutCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la table "candidatures".
 * Fournit les methodes CRUD et les requetes de filtrage.
 */
@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, UUID> {

    /**
     * Verifie si un candidat a deja postule a une offre.
     * Utilise pour la contrainte UNIQUE (candidat_id, offre_id).
     */
    boolean existsByCandidatIdAndOffreId(UUID candidatId, UUID offreId);

    /**
     * Trouve toutes les candidatures d'un candidat.
     * US-CAND-02: Le candidat consulte ses candidatures.
     * Triees par date de soumission DESC (les plus recentes d'abord).
     */
    List<Candidature> findByCandidatIdOrderByDateSoumissionDesc(UUID candidatId);

    /**
     * US-RH-04 & US-RH-05: Filtrer et lister les candidatures (RH).
     * Filtres optionnels: statut, score min, score max.
     * Tri par date de soumission DESC par defaut.
     *
     * @param statut   filtre par statut (nullable = pas de filtre)
     * @param scoreMin score minimum (nullable = pas de filtre)
     * @param scoreMax score maximum (nullable = pas de filtre)
     */
    @Query("""
        SELECT c FROM Candidature c
        LEFT JOIN FETCH c.candidat
        LEFT JOIN FETCH c.offre
        WHERE (:statut IS NULL OR c.statut = :statut)
        AND (:scoreMin IS NULL OR c.scoreTotal >= :scoreMin)
        AND (:scoreMax IS NULL OR c.scoreTotal <= :scoreMax)
        ORDER BY c.dateSoumission DESC
    """)
    List<Candidature> findByFilters(
            @Param("statut") StatutCandidature statut,
            @Param("scoreMin") Double scoreMin,
            @Param("scoreMax") Double scoreMax);

    /**
     * Lister toutes les candidatures triees par score DESC.
     * US-RH-05: Tri par score.
     */
    @Query("""
        SELECT c FROM Candidature c
        LEFT JOIN FETCH c.candidat
        LEFT JOIN FETCH c.offre
        ORDER BY c.scoreTotal DESC NULLS LAST
    """)
    List<Candidature> findAllOrderByScoreDesc();

    /**
     * Lister toutes les candidatures triees par date DESC.
     * US-RH-05: Tri par date.
     */
    @Query("""
        SELECT c FROM Candidature c
        LEFT JOIN FETCH c.candidat
        LEFT JOIN FETCH c.offre
        ORDER BY c.dateSoumission DESC
    """)
    List<Candidature> findAllOrderByDateDesc();

    /**
     * US-CAND-11: Trouve les candidatures transmises au Manager.
     * Inclut les statuts: envoyee_manager, acceptee_manager, rejetee_manager, entretien_planifie
     */
    @Query("""
        SELECT c FROM Candidature c
        LEFT JOIN FETCH c.candidat
        LEFT JOIN FETCH c.offre
        WHERE c.statut IN :statuts
        ORDER BY c.dateSoumission DESC
    """)
    List<Candidature> findByStatutIn(@Param("statuts") List<StatutCandidature> statuts);
}

