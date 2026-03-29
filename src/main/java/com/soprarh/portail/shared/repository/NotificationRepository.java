package com.soprarh.portail.shared.repository;

import com.soprarh.portail.shared.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la table "notifications".
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Trouve toutes les notifications d'un utilisateur.
     * Triees par date de creation DESC (les plus recentes d'abord).
     */
    List<Notification> findByUtilisateurIdOrderByDateCreationDesc(UUID utilisateurId);

    /**
     * Trouve les notifications non lues d'un utilisateur.
     */
    List<Notification> findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(UUID utilisateurId);

    /**
     * Compte les notifications non lues d'un utilisateur.
     */
    long countByUtilisateurIdAndLuFalse(UUID utilisateurId);
}

