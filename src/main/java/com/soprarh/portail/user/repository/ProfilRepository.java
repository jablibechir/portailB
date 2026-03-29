package com.soprarh.portail.user.repository;

import com.soprarh.portail.user.entity.Profil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la table "profils".
 */
@Repository
public interface ProfilRepository extends JpaRepository<Profil, UUID> {

    /**
     * Trouve le profil d'un utilisateur.
     */
    Optional<Profil> findByUtilisateurId(UUID utilisateurId);

    /**
     * Verifie si un profil existe pour un utilisateur.
     */
    boolean existsByUtilisateurId(UUID utilisateurId);
}

