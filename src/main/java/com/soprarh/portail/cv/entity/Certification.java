package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entite mappant exactement la table "certifications".
 *
 * CREATE TABLE public.certifications (
 *   id            uuid DEFAULT gen_random_uuid() NOT NULL,
 *   donnees_cv_id uuid -> FK donnees_cv(id),
 *   nom           varchar(150),
 *   organisme     varchar(150),
 *   date_obtention date
 * );
 */
@Entity
@Table(name = "certifications", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Certification {

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
     * Nom de la certification (ex: "Oracle Java Certified").
     * Colonne: nom varchar(150)
     */
    @Column(name = "nom", length = 150)
    private String nom;

    /**
     * Organisme delivrant la certification.
     * Colonne: organisme varchar(150)
     */
    @Column(name = "organisme", length = 150)
    private String organisme;

    /**
     * Date d'obtention de la certification.
     * Colonne: date_obtention date
     */
    @Column(name = "date_obtention")
    private LocalDate dateObtention;
}

