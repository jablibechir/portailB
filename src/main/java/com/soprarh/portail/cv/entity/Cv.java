package com.soprarh.portail.cv.entity;

import com.soprarh.portail.application.entity.Candidature;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entite mappant exactement la table "cvs".
 *
 * CREATE TABLE public.cvs (
 *   id              uuid DEFAULT gen_random_uuid() NOT NULL,
 *   candidature_id  uuid UNIQUE -> FK candidatures(id) ON DELETE CASCADE,
 *   fichier         varchar(255) NOT NULL,
 *   date_upload     date DEFAULT CURRENT_DATE
 * );
 */
@Entity
@Table(name = "cvs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Relation OneToOne avec Candidature.
     * Chaque CV est lie a une seule candidature.
     * FK: candidature_id -> candidatures(id) ON DELETE CASCADE
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", unique = true)
    private Candidature candidature;

    /**
     * Chemin vers le fichier CV stocke.
     * Colonne: fichier varchar(255) NOT NULL
     */
    @Column(name = "fichier", length = 255, nullable = false)
    private String fichier;

    /**
     * Date de telechargement du CV.
     * Colonne: date_upload date DEFAULT CURRENT_DATE
     */
    @Column(name = "date_upload")
    @Builder.Default
    private LocalDate dateUpload = LocalDate.now();

    /**
     * Relation OneToOne vers DonneesCv (donnees extraites).
     * mappedBy indique que DonneesCv possede la FK.
     */
    @OneToOne(mappedBy = "cv", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DonneesCv donneesCv;
}

