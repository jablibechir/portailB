package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entite mappant exactement la table "soft_skills".
 *
 * CREATE TABLE public.soft_skills (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   nom           varchar(100)
 * );
 */
@Entity
@Table(name = "soft_skills", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SoftSkill {

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
     * Nom du soft skill (ex: "communication", "travail en equipe").
     * Colonne: nom varchar(100)
     */
    @Column(name = "nom", length = 100)
    private String nom;
}

