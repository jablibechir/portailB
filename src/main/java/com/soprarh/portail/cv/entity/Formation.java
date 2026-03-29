package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entite mappant exactement la table "formations".
 *
 * CREATE TABLE public.formations (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   diplome       varchar(150),
 *   etablissement varchar(150),
 *   date_debut    date,
 *   date_fin      date
 * );
 */
@Entity
@Table(name = "formations", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Relation ManyToOne vers DonneesCv.
     * FK: donnees_cv_id -> donnees_cv(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donnees_cv_id")
    private DonneesCv donneesCv;

    /**
     * Nom du diplome obtenu.
     * Colonne: diplome varchar(150)
     */
    @Column(name = "diplome", length = 150)
    private String diplome;

    /**
     * Nom de l'etablissement.
     * Colonne: etablissement varchar(150)
     */
    @Column(name = "etablissement", length = 150)
    private String etablissement;

    /**
     * Date de debut de la formation.
     * Colonne: date_debut date
     */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    /**
     * Date de fin de la formation.
     * Colonne: date_fin date
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;
}

