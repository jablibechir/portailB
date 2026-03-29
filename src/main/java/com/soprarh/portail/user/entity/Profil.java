package com.soprarh.portail.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "profils".
 *
 * CREATE TABLE public.profils (
 *   id               uuid DEFAULT gen_random_uuid() NOT NULL,
 *   utilisateur_id   uuid UNIQUE -> FK utilisateurs(id),
 *   telephone        varchar(20),
 *   photo_url        varchar(255),
 *   adresse          varchar(255),
 *   date_mise_a_jour timestamp DEFAULT CURRENT_TIMESTAMP
 * );
 */
@Entity
@Table(name = "profils", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Profil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Relation OneToOne avec Utilisateur.
     * FK: utilisateur_id -> utilisateurs(id) UNIQUE
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true)
    private Utilisateur utilisateur;

    /**
     * Numero de telephone.
     * Colonne: telephone varchar(20)
     */
    @Column(name = "telephone", length = 20)
    private String telephone;

    /**
     * URL de la photo de profil.
     * Colonne: photo_url varchar(255)
     */
    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    /**
     * Adresse postale.
     * Colonne: adresse varchar(255)
     */
    @Column(name = "adresse", length = 255)
    private String adresse;

    /**
     * Date de derniere mise a jour du profil.
     * Colonne: date_mise_a_jour timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_mise_a_jour")
    @Builder.Default
    private LocalDateTime dateMiseAJour = LocalDateTime.now();
}

