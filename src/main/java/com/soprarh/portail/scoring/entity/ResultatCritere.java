package com.soprarh.portail.scoring.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entite mappant exactement la table "resultat_criteres".
 *
 * CREATE TABLE public.resultat_criteres (
 *   resultat_id   uuid NOT NULL -> FK resultats_scoring(id) ON DELETE CASCADE,
 *   critere_id    uuid NOT NULL -> FK criteres_scoring(id) ON DELETE CASCADE,
 *   score_obtenu  double precision NOT NULL,
 *   PRIMARY KEY (resultat_id, critere_id)
 * );
 */
@Entity
@Table(name = "resultat_criteres", schema = "public")
@IdClass(ResultatCritereId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatCritere {

    /**
     * Resultat de scoring parent.
     * FK: resultat_id -> resultats_scoring(id) ON DELETE CASCADE
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultat_id", nullable = false)
    private ResultatScoring resultat;

    /**
     * Critere de scoring evalue.
     * FK: critere_id -> criteres_scoring(id) ON DELETE CASCADE
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereScoring critere;

    /**
     * Score obtenu pour ce critere specifique.
     * Colonne: score_obtenu double precision NOT NULL
     */
    @Column(name = "score_obtenu", nullable = false)
    private Double scoreObtenu;
}

