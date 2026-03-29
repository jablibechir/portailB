package com.soprarh.portail.evaluation.entity;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "evaluations".
 *
 * CREATE TABLE public.evaluations (
 *   id              uuid DEFAULT gen_random_uuid() NOT NULL,
 *   candidature_id  uuid -> FK candidatures(id) ON DELETE CASCADE,
 *   evaluateur_id   uuid -> FK utilisateurs(id) ON DELETE SET NULL,
 *   commentaire     text,
 *   decision        varchar(20),
 *   date_evaluation date DEFAULT CURRENT_DATE,
 *   date_creation   timestamp DEFAULT CURRENT_TIMESTAMP,
 *   CONSTRAINT evaluations_decision_check CHECK (decision IN ('pour_suivre','a_revoir','rejeter'))
 * );
 */
@Entity
@Table(name = "evaluations", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Candidature evaluee.
     * FK: candidature_id -> candidatures(id) ON DELETE CASCADE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;

    /**
     * Utilisateur (RH/Manager) qui a realise l'evaluation.
     * FK: evaluateur_id -> utilisateurs(id) ON DELETE SET NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id")
    private Utilisateur evaluateur;

    /**
     * Commentaire de l'evaluateur.
     * Colonne: commentaire text
     */
    @Column(name = "commentaire", columnDefinition = "text")
    private String commentaire;

    /**
     * Decision de l'evaluateur.
     * Colonne: decision varchar(20)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20)
    private DecisionEvaluation decision;

    /**
     * Date de l'evaluation.
     * Colonne: date_evaluation date DEFAULT CURRENT_DATE
     */
    @Column(name = "date_evaluation")
    @Builder.Default
    private LocalDate dateEvaluation = LocalDate.now();

    /**
     * Date de creation de l'enregistrement.
     * Colonne: date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}

