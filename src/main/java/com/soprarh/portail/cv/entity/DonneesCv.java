package com.soprarh.portail.cv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entite mappant exactement la table "donnees_cv".
 *
 * CREATE TABLE public.donnees_cv (
 *   id              uuid DEFAULT gen_random_uuid() NOT NULL,
 *   cv_id           uuid UNIQUE -> FK cvs(id) ON DELETE CASCADE,
 *   competences     text,
 *   experiences     text,
 *   formations      text,
 *   langues         text,
 *   date_extraction timestamp DEFAULT CURRENT_TIMESTAMP,
 *   certifications  text,
 *   soft_skills     text,
 *   resume          text
 * );
 */
@Entity
@Table(name = "donnees_cv", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DonneesCv {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Relation OneToOne avec Cv.
     * FK: cv_id -> cvs(id) ON DELETE CASCADE
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", unique = true)
    private Cv cv;

    /**
     * Competences extraites (texte brut ou JSON).
     * Colonne: competences text
     */
    @Column(name = "competences", columnDefinition = "text")
    private String competences;

    /**
     * Experiences extraites (texte brut ou JSON).
     * Colonne: experiences text
     */
    @Column(name = "experiences", columnDefinition = "text")
    private String experiences;

    /**
     * Formations extraites (texte brut ou JSON).
     * Colonne: formations text
     */
    @Column(name = "formations", columnDefinition = "text")
    private String formations;

    /**
     * Langues extraites (texte brut ou JSON).
     * Colonne: langues text
     */
    @Column(name = "langues", columnDefinition = "text")
    private String langues;

    /**
     * Date d'extraction des donnees.
     * Colonne: date_extraction timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_extraction")
    @Builder.Default
    private LocalDateTime dateExtraction = LocalDateTime.now();

    /**
     * Certifications extraites (texte brut ou JSON).
     * Colonne: certifications text
     */
    @Column(name = "certifications", columnDefinition = "text")
    private String certifications;

    /**
     * Soft skills extraites (texte brut ou JSON).
     * Colonne: soft_skills text
     */
    @Column(name = "soft_skills", columnDefinition = "text")
    private String softSkills;

    /**
     * Resume genere par l'IA.
     * Colonne: resume text
     */
    @Column(name = "resume", columnDefinition = "text")
    private String resume;

    // ============== Relations vers les tables de detail ==============

    /**
     * Liste des competences detaillees.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Competence> competencesDetail = new ArrayList<>();

    /**
     * Liste des experiences detaillees.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Experience> experiencesDetail = new ArrayList<>();

    /**
     * Liste des formations detaillees.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Formation> formationsDetail = new ArrayList<>();

    /**
     * Liste des langues detaillees.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Langue> languesDetail = new ArrayList<>();

    /**
     * Liste des certifications detaillees.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Certification> certificationsDetail = new ArrayList<>();

    /**
     * Liste des soft skills detailles.
     */
    @OneToMany(mappedBy = "donneesCv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SoftSkill> softSkillsDetail = new ArrayList<>();
}

