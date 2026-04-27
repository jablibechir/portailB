package com.soprarh.portail.scoring.entity;

import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "criteres_scoring".
 *
 * CREATE TABLE public.criteres_scoring (
 *   id             uuid DEFAULT gen_random_uuid() NOT NULL,
 *   nom            varchar(100) NOT NULL,
 *   poids          double precision NOT NULL,
 *   cree_par       uuid -> FK utilisateurs(id) ON DELETE SET NULL,
 *   date_creation  timestamp DEFAULT CURRENT_TIMESTAMP,
 *   offre_id       uuid -> FK offres_emploi(id),
 *   type           varchar(50) CHECK (type IN ('COMPETENCES','EXPERIENCE','FORMATION',
 *                    'LANGUES','CERTIFICATIONS','SOFT_SKILLS'))
 * );
 * CONSTRAINT criteres_scoring_poids_check CHECK (poids > 0 AND poids <= 100)
 */
@Entity
@Table(name = "criteres_scoring", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CritereScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Nom du critere (ex: "Maitrise Java", "Niveau Anglais").
     * Colonne: nom varchar(100) NOT NULL
     */
    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    /**
     * Poids du critere dans le scoring (entre 0 exclus et 100 inclus).
     * Colonne: poids double precision NOT NULL
     * CONSTRAINT: poids > 0 AND poids <= 100
     */
    @Column(name = "poids", nullable = false)
    private Double poids;

    /**
     * RH qui a cree ce critere.
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
     * Offre d'emploi a laquelle ce critere est associe.
     * FK: offre_id -> offres_emploi(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id")
    private OffreEmploi offre;

    /**
     * Type de critere de scoring.
     * Colonne: type varchar(50)
     * CHECK (type IN ('COMPETENCES','EXPERIENCE','FORMATION','LANGUES','CERTIFICATIONS','SOFT_SKILLS'))
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private TypeCritere type;
}

