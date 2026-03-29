package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entite mappant exactement la table "langues".
 *
 * CREATE TABLE public.langues (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   langue        varchar(100),
 *   niveau        varchar(50)
 * );
 */
@Entity
@Table(name = "langues", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Langue {

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
     * Nom de la langue (ex: "Francais", "Anglais").
     * Colonne: langue varchar(100)
     */
    @Column(name = "langue", length = 100)
    private String langue;

    /**
     * Niveau de maitrise (ex: "C1", "B2", "natif").
     * Colonne: niveau varchar(50)
     */
    @Column(name = "niveau", length = 50)
    private String niveau;
}

