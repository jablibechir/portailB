package com.soprarh.portail.offer.entity;

import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "offres_emploi".
 *
 * CREATE TABLE public.offres_emploi (
 *   id                   uuid DEFAULT gen_random_uuid() NOT NULL,
 *   titre                varchar(200) NOT NULL,
 *   description          text,
 *   competences_requises text,
 *   experience_requise   varchar(100),
 *   formation_requise    varchar(200),
 *   date_publication     date DEFAULT CURRENT_DATE,
 *   statut               varchar(20) DEFAULT 'brouillon',
 *   cree_par             uuid -> FK utilisateurs(id),
 *   date_creation        timestamp DEFAULT CURRENT_TIMESTAMP,
 *   date_expiration      date
 * );
 */
@Entity
@Table(name = "offres_emploi", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Titre de l'offre d'emploi.
     * Colonne: titre varchar(200) NOT NULL
     */
    @Column(name = "titre", length = 200, nullable = false)
    private String titre;

    /**
     * Description detaillee du poste.
     * Colonne: description text
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Competences requises pour le poste.
     * Colonne: competences_requises text
     */
    @Column(name = "competences_requises", columnDefinition = "text")
    private String competencesRequises;

    /**
     * Experience requise (ex: "3-5 ans", "Junior", "Senior").
     * Colonne: experience_requise varchar(100)
     */
    @Column(name = "experience_requise", length = 100)
    private String experienceRequise;

    /**
     * Formation requise (ex: "Bac+5 Informatique").
     * Colonne: formation_requise varchar(200)
     */
    @Column(name = "formation_requise", length = 200)
    private String formationRequise;

    /**
     * Date de publication de l'offre.
     * Colonne: date_publication date DEFAULT CURRENT_DATE
     */
    @Column(name = "date_publication")
    @Builder.Default
    private LocalDate datePublication = LocalDate.now();

    /**
     * Statut de l'offre.
     * Colonne: statut varchar(20) DEFAULT 'brouillon'
     * CHECK (statut IN ('brouillon', 'publiee', 'archivee'))
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    @Builder.Default
    private StatutOffre statut = StatutOffre.brouillon;

    /**
     * Utilisateur qui a cree l'offre.
     * FK: cree_par -> utilisateurs(id) ON DELETE SET NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    /**
     * Date de creation de l'enregistrement.
     * Colonne: date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    /**
     * Date d'expiration de l'offre.
     * Colonne: date_expiration date (nullable)
     */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
}

