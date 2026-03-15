package com.soprarh.portail.user.repository;

import com.soprarh.portail.user.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la table "utilisateurs".
 * Fournit les methodes necessaires au module Auth.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    /**
     * Recherche par email avec chargement complet des roles ET permissions.
     *
     * IMPORTANT : JOIN FETCH force Hibernate a charger toute la chaine
     * Utilisateur -> Roles -> Permissions en une seule requete.
     *
     * Sans cela, meme avec FetchType.EAGER, les permissions peuvent ne pas
     * etre chargees correctement dans certains contextes (hors transaction).
     *
     * LEFT JOIN FETCH : retourne l'utilisateur meme s'il n'a pas de roles.
     */
    @Query("SELECT u FROM Utilisateur u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<Utilisateur> findByEmailWithRolesAndPermissions(@Param("email") String email);

    /**
     * Recherche simple par email (sans chargement des relations).
     * Utiliser pour des verifications rapides.
     */
    Optional<Utilisateur> findByEmail(String email);

    /**
     * Verifie l'unicite de l'email avant inscription.
     * Evite un round-trip inutile (findByEmail + null check).
     */
    boolean existsByEmail(String email);

    /**
     * Recherche un utilisateur par son code de verification email.
     * Utilise lors de l'activation du compte.
     */
    Optional<Utilisateur> findByCodeVerification(String codeVerification);
}

