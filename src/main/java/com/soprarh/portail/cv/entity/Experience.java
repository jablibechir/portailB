package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entite mappant exactement la table "experiences".
 *
 * CREATE TABLE public.experiences (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   poste         varchar(150),
 *   entreprise    varchar(150),
 *   date_debut    date,
 *   date_fin      date,
 *   description   text
 * );
 */
@Entity
@Table(name = "experiences", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Experience {

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
     * Intitule du poste occupe.
     * Colonne: poste varchar(150)
     */
    @Column(name = "poste", length = 150)
    private String poste;

    /**
     * Nom de l'entreprise.
     * Colonne: entreprise varchar(150)
     */
    @Column(name = "entreprise", length = 150)
    private String entreprise;

    /**
     * Date de debut de l'experience.
     * Colonne: date_debut date
     */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    /**
     * Date de fin de l'experience (null si toujours en cours).
     * Colonne: date_fin date
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Description des taches et responsabilites.
     * Colonne: description text
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;
}

