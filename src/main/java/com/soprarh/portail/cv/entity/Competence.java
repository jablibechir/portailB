package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entite mappant exactement la table "competences".
 *
 * CREATE TABLE public.competences (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   nom           varchar(100),
 *   niveau        varchar(50)
 * );
 */
@Entity
@Table(name = "competences", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Competence {

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
     * Nom de la competence (ex: "Java", "Spring", "SQL").
     * Colonne: nom varchar(100)
     */
    @Column(name = "nom", length = 100)
    private String nom;

    /**
     * Niveau de maitrise (ex: "expert", "intermediaire", "debutant").
     * Colonne: niveau varchar(50)
     */
    @Column(name = "niveau", length = 50)
    private String niveau;
}

