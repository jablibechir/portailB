package com.soprarh.portail.cv.repository;

import com.soprarh.portail.cv.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la table "cvs".
 */
@Repository
public interface CvRepository extends JpaRepository<Cv, UUID> {

    /**
     * Trouve un CV par l'ID de la candidature.
     */
    Optional<Cv> findByCandidatureId(UUID candidatureId);

    /**
     * Verifie si un CV existe pour une candidature.
     */
    boolean existsByCandidatureId(UUID candidatureId);
}

