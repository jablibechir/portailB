package com.soprarh.portail.dashboard.service;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.dashboard.dto.*;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import com.soprarh.portail.offer.repository.OffreEmploiRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CandidatureRepository candidatureRepository;
    private final OffreEmploiRepository offreRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Genere le dashboard complet pour le RH.
     */
    public DashboardResponse getDashboard() {

        List<Candidature> allCandidatures = candidatureRepository.findAllOrderByDateDesc();
        List<OffreEmploi> allOffres = offreRepository.findAll();

        // KPIs
        long totalCandidatures = allCandidatures.size();
        long totalOffres = allOffres.size();
        long offresPubliees = allOffres.stream()
                .filter(o -> o.getStatut() == StatutOffre.publiee).count();
        long totalUtilisateurs = utilisateurRepository.count();

        // Candidatures par statut
        Map<String, Long> candidaturesParStatut = allCandidatures.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatut().name(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        // Offres par statut
        Map<String, Long> offresParStatut = allOffres.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatut().name(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        // Taux de selection
        long acceptees = allCandidatures.stream()
                .filter(c -> "acceptee_manager".equals(c.getStatut().name())
                        || "entretien_planifie".equals(c.getStatut().name()))
                .count();
        double tauxSelection = totalCandidatures > 0
                ? Math.round((double) acceptees / totalCandidatures * 10000.0) / 100.0
                : 0.0;

        // Score moyen
        double scoreMoyen = allCandidatures.stream()
                .filter(c -> c.getScoreTotal() != null)
                .mapToDouble(Candidature::getScoreTotal)
                .average()
                .orElse(0.0);
        scoreMoyen = Math.round(scoreMoyen * 100.0) / 100.0;

        // 5 dernieres candidatures
        List<CandidatureResumeDto> candidaturesRecentes = allCandidatures.stream()
                .limit(5)
                .map(c -> new CandidatureResumeDto(
                        c.getId(),
                        c.getCandidat() != null ? c.getCandidat().getNom() : null,
                        c.getCandidat() != null ? c.getCandidat().getPrenom() : null,
                        c.getOffre() != null ? c.getOffre().getTitre() : null,
                        c.getStatut().name(),
                        c.getScoreTotal(),
                        c.getDateSoumission()))
                .collect(Collectors.toList());

        // 5 dernieres offres
        List<OffreResumeDto> offresRecentes = allOffres.stream()
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .limit(5)
                .map(o -> new OffreResumeDto(
                        o.getId(),
                        o.getTitre(),
                        o.getStatut().name(),
                        o.getTypeEmploi(),
                        o.getDatePublication()))
                .collect(Collectors.toList());

        return new DashboardResponse(
                totalCandidatures,
                totalOffres,
                offresPubliees,
                totalUtilisateurs,
                candidaturesParStatut,
                offresParStatut,
                tauxSelection,
                scoreMoyen,
                candidaturesRecentes,
                offresRecentes
        );
    }
}

