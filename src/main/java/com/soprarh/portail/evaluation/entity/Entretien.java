package com.soprarh.portail.evaluation.entity;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "entretiens".
 *
 * CREATE TABLE public.entretiens (
 *   id              uuid DEFAULT gen_random_uuid() NOT NULL,
 *   candidature_id  uuid -> FK candidatures(id),
 *   planifie_par    uuid -> FK utilisateurs(id),
 *   date_entretien  timestamp NOT NULL,
 *   lieu            varchar(255),
 *   type            varchar(50),
 *   statut          varchar(50) DEFAULT 'planifie',
 *   notes           text,
 *   date_creation   timestamp DEFAULT CURRENT_TIMESTAMP,
 *   CONSTRAINT entretiens_type_check CHECK (type IN ('presentiel','visio','telephonique')),
 *   CONSTRAINT entretiens_statut_check CHECK (statut IN ('planifie','confirme','annule','termine'))
 * );
 */
@Entity
@Table(name = "entretiens", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Candidature concernee par l'entretien.
     * FK: candidature_id -> candidatures(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;

    /**
     * Utilisateur (RH/Manager) qui a planifie l'entretien.
     * FK: planifie_par -> utilisateurs(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planifie_par")
    private Utilisateur planifiePar;

    /**
     * Date et heure de l'entretien.
     * Colonne: date_entretien timestamp NOT NULL
     */
    @Column(name = "date_entretien", nullable = false)
    private LocalDateTime dateEntretien;

    /**
     * Lieu de l'entretien (adresse physique ou lien visio).
     * Colonne: lieu varchar(255)
     */
    @Column(name = "lieu", length = 255)
    private String lieu;

    /**
     * Type d'entretien.
     * Colonne: type varchar(50)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private TypeEntretien type;

    /**
     * Statut de l'entretien.
     * Colonne: statut varchar(50) DEFAULT 'planifie'
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 50)
    @Builder.Default
    private StatutEntretien statut = StatutEntretien.planifie;

    /**
     * Notes et commentaires sur l'entretien.
     * Colonne: notes text
     */
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /**
     * Date de creation de l'enregistrement.
     * Colonne: date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}

