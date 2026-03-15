package com.soprarh.portail.user.repository;

import com.soprarh.portail.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la table "roles".
 * Utilise lors de l'inscription pour assigner le role par defaut.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Recherche un role par son nom (ex: "CANDIDAT", "MANAGER", "RH").
     * Utilise lors de l'inscription pour assigner le bon role.
     */
    Optional<Role> findByNom(String nom);
}

