package com.soprarh.portail.offer.repository;

import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la table "offres_emploi".
 * Fournit les methodes CRUD et les requetes de filtrage.
 */
@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, UUID> {

    /**
     * Trouve toutes les offres avec un statut specifique.
     * Utilise pour afficher uniquement les offres publiees aux candidats.
     */
    List<OffreEmploi> findByStatut(StatutOffre statut);

    /**
     * US-OFF-05: Filtre par mot-cle, experience, formation, date de publication, type d'emploi.
     */
    @Query("""
        SELECT o FROM OffreEmploi o
        WHERE o.statut = :statut
        AND (:keyword IS NULL OR
             LOWER(o.titre) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR
             LOWER(o.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR
             LOWER(o.competencesRequises) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
        AND (:experience IS NULL OR LOWER(o.experienceRequise) LIKE LOWER(CONCAT('%', CAST(:experience AS text), '%')))
        AND (:formation IS NULL OR LOWER(o.formationRequise) LIKE LOWER(CONCAT('%', CAST(:formation AS text), '%')))
        AND (:dateMin IS NULL OR o.datePublication >= :dateMin)
        AND (:typeEmploi IS NULL OR o.typeEmploi = :typeEmploi)
        ORDER BY o.datePublication DESC
    """)
    List<OffreEmploi> findByFilters(
            @Param("statut") StatutOffre statut,
            @Param("keyword") String keyword,
            @Param("experience") String experience,
            @Param("formation") String formation,
            @Param("dateMin") LocalDate dateMin,
            @Param("typeEmploi") String typeEmploi);

    /**
     * Trouve toutes les offres creees par un utilisateur specifique.
     */
    List<OffreEmploi> findByCreeParId(UUID utilisateurId);

    /**
     * Trouve toutes les offres recommandees par un manager.
     */
    List<OffreEmploi> findByRecommandeeParIdOrderByDateRecommandationDesc(UUID managerId);

    /**
     * Trouve toutes les offres avec le statut recommandee.
     */
    List<OffreEmploi> findByStatutOrderByDateRecommandationDesc(StatutOffre statut);
}

