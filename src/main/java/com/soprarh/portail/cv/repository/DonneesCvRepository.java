package com.soprarh.portail.cv.repository;

import com.soprarh.portail.cv.entity.DonneesCv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la table "donnees_cv".
 */
@Repository
public interface DonneesCvRepository extends JpaRepository<DonneesCv, UUID> {

    /**
     * Trouve les donnees extraites par l'ID du CV.
     */
    Optional<DonneesCv> findByCvId(UUID cvId);
}

