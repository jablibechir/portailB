package com.soprarh.portail.application.entity;

import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "candidatures".
 *
 * CREATE TABLE public.candidatures (
 *   id               uuid DEFAULT gen_random_uuid() NOT NULL,
 *   candidat_id      uuid -> FK utilisateurs(id) ON DELETE CASCADE,
 *   offre_id         uuid -> FK offres_emploi(id) ON DELETE CASCADE,
 *   date_soumission  date DEFAULT CURRENT_DATE,
 *   statut           varchar(20) DEFAULT 'soumise',
 *   score_total      double precision,
 *   date_creation    timestamp DEFAULT CURRENT_TIMESTAMP,
 *   lettre_motivation text,
 *   UNIQUE (candidat_id, offre_id),
 *   CONSTRAINT candidatures_statut_check CHECK (statut IN (
 *       'soumise','en_evaluation_rh','envoyee_manager',
 *       'acceptee_manager','rejetee_rh','rejetee_manager','entretien_planifie'
 *   ))
 * );
 */
@Entity
@Table(name = "candidatures", schema = "public",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidat_id", "offre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Candidat qui a soumis la candidature.
     * FK: candidat_id -> utilisateurs(id) ON DELETE CASCADE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidat_id")
    private Utilisateur candidat;

    /**
     * Offre a laquelle le candidat postule.
     * FK: offre_id -> offres_emploi(id) ON DELETE CASCADE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id")
    private OffreEmploi offre;

    /**
     * Date de soumission de la candidature.
     * Colonne: date_soumission date DEFAULT CURRENT_DATE
     */
    @Column(name = "date_soumission")
    @Builder.Default
    private LocalDate dateSoumission = LocalDate.now();

    /**
     * Statut de la candidature.
     * Colonne: statut varchar(20) DEFAULT 'soumise'
     * CHECK (statut IN ('soumise', 'en_evaluation', 'acceptee', 'rejetee'))
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    @Builder.Default
    private StatutCandidature statut = StatutCandidature.soumise;

    /**
     * Score total de la candidature (calcule par le module scoring).
     * Colonne: score_total double precision (nullable)
     */
    @Column(name = "score_total")
    private Double scoreTotal;

    /**
     * Lettre de motivation du candidat.
     * Colonne: lettre_motivation text (nullable)
     */
    @Column(name = "lettre_motivation", columnDefinition = "text")
    private String lettreMotivation;

    /**
     * Date de creation de l'enregistrement.
     * Colonne: date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    /**
     * CV associe a cette candidature (relation inverse).
     * mappedBy indique que Cv possede la FK.
     */
    @OneToOne(mappedBy = "candidature", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private com.soprarh.portail.cv.entity.Cv cv;

    /**
     * Manager a qui la candidature a ete transmise.
     * FK: manager_id -> utilisateurs(id) ON DELETE SET NULL
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Utilisateur manager;
}

