package com.soprarh.portail.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO principal pour le dashboard RH.
 * Contient tous les KPIs et statistiques.
 */
public record DashboardResponse(

        // KPIs generaux
        long totalCandidatures,
        long totalOffres,
        long offresPubliees,
        long totalUtilisateurs,

        // Candidatures par statut
        Map<String, Long> candidaturesParStatut,

        // Offres par statut
        Map<String, Long> offresParStatut,

        // Taux de selection (acceptees / total)
        double tauxSelection,

        // Score moyen des candidatures
        double scoreMoyen,

        // Candidatures recentes (5 dernieres)
        List<CandidatureResumeDto> candidaturesRecentes,

        // Offres recentes (5 dernieres)
        List<OffreResumeDto> offresRecentes
) {}

