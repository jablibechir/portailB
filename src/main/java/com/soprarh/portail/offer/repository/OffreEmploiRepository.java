package com.soprarh.portail.offer.repository;

import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Trouve les offres publiees avec filtres optionnels.
     * US-OFF-05: Filtrer par mot-cle, experience, formation.
     *
     * @param keyword   mot-cle recherche dans titre, description, competences (peut etre null)
     * @param experience filtre sur experience_requise (peut etre null)
     * @param formation  filtre sur formation_requise (peut etre null)
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
    ORDER BY o.datePublication DESC
""")
    List<OffreEmploi> findByFilters(
            @Param("statut") StatutOffre statut,
            @Param("keyword") String keyword,
            @Param("experience") String experience,
            @Param("formation") String formation);
    /**
     * Trouve toutes les offres creees par un utilisateur specifique.
     */
    List<OffreEmploi> findByCreeParId(UUID utilisateurId);
}

