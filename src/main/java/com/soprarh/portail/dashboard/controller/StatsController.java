package com.soprarh.portail.dashboard.controller;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.dashboard.dto.DashboardResponse;
import com.soprarh.portail.dashboard.service.DashboardService;
import com.soprarh.portail.evaluation.repository.EntretienRepository;
import com.soprarh.portail.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller des statistiques avancees.
 * Endpoints attendus par la page AdminStatistiques (frontend).
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DashboardService dashboardService;
    private final CandidatureRepository candidatureRepository;
    private final EntretienRepository entretienRepository;

    /**
     * GET /api/stats/dashboard
     * Vue analytique : KPIs + nombre d'entretiens + delai moyen.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('VIEW_STATS') or hasAuthority('VIEW_STATISTICS')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        DashboardResponse base = dashboardService.getDashboard();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalCandidatures", base.totalCandidatures());
        payload.put("totalOffres", base.totalOffres());
        payload.put("offresPubliees", base.offresPubliees());
        payload.put("totalUtilisateurs", base.totalUtilisateurs());
        payload.put("candidaturesParStatut", base.candidaturesParStatut());
        payload.put("offresParStatut", base.offresParStatut());
        payload.put("tauxSelection", base.tauxSelection());
        payload.put("scoreMoyen", base.scoreMoyen());
        payload.put("totalEntretiens", entretienRepository.count());

        // Delai moyen (jours entre dateSoumission et aujourd'hui pour les candidatures cloturees)
        List<Candidature> all = candidatureRepository.findAllOrderByDateDesc();
        double delaiMoyen = all.stream()
                .filter(c -> c.getDateSoumission() != null)
                .filter(c -> {
                    String s = c.getStatut() != null ? c.getStatut().name() : "";
                    return s.startsWith("rejetee_") || s.equals("acceptee_manager") || s.equals("entretien_planifie");
                })
                .mapToLong(c -> Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                        c.getDateSoumission(), LocalDate.now())))
                .average()
                .orElse(0.0);
        payload.put("delaiMoyenJours", Math.round(delaiMoyen));
        payload.put("tempsMoyenJours", Math.round(delaiMoyen));

        return ResponseEntity.ok(ApiResponse.success(payload, "Statistiques chargees"));
    }

    /**
     * GET /api/stats/candidatures-par-mois
     * Repartition mensuelle des candidatures sur les 12 derniers mois.
     */
    @GetMapping("/candidatures-par-mois")
    @PreAuthorize("hasAuthority('VIEW_STATS') or hasAuthority('VIEW_STATISTICS')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCandidaturesParMois() {
        List<Candidature> all = candidatureRepository.findAllOrderByDateDesc();

        Map<Integer, Long> parMois = all.stream()
                .filter(c -> c.getDateSoumission() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getDateSoumission().getMonthValue(),
                        Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mois", m);
            row.put("total", parMois.getOrDefault(m, 0L));
            result.add(row);
        }
        return ResponseEntity.ok(ApiResponse.success(result, "Repartition mensuelle"));
    }

    /**
     * GET /api/stats/top-offres
     * Top 10 des offres avec le plus de candidatures.
     */
    @GetMapping("/top-offres")
    @PreAuthorize("hasAuthority('VIEW_STATS') or hasAuthority('VIEW_STATISTICS')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopOffres() {
        List<Candidature> all = candidatureRepository.findAllOrderByDateDesc();

        Map<UUID, Long> counts = all.stream()
                .filter(c -> c.getOffre() != null)
                .collect(Collectors.groupingBy(c -> c.getOffre().getId(), Collectors.counting()));

        Map<UUID, String> titres = all.stream()
                .filter(c -> c.getOffre() != null)
                .collect(Collectors.toMap(
                        c -> c.getOffre().getId(),
                        c -> c.getOffre().getTitre() != null ? c.getOffre().getTitre() : "Offre",
                        (a, b) -> a));

        List<Map<String, Object>> result = counts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("offreId", e.getKey());
                    row.put("titre", titres.get(e.getKey()));
                    row.put("totalCandidatures", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result, "Top offres"));
    }

    /**
     * GET /api/stats/export?format=pdf|excel
     * Export simple (CSV pour 'excel', texte pour 'pdf') sous forme de fichier telechargeable.
     * Implementation minimale sans dependance externe.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS') or hasAuthority('VIEW_STATISTICS')")
    public ResponseEntity<ByteArrayResource> exportRapport(
            @RequestParam(defaultValue = "pdf") String format) {

        DashboardResponse d = dashboardService.getDashboard();
        StringBuilder sb = new StringBuilder();

        boolean excel = "excel".equalsIgnoreCase(format);
        String sep = excel ? "," : " : ";

        if (excel) {
            sb.append("Indicateur,Valeur\n");
            sb.append("Total candidatures,").append(d.totalCandidatures()).append('\n');
            sb.append("Total offres,").append(d.totalOffres()).append('\n');
            sb.append("Offres publiees,").append(d.offresPubliees()).append('\n');
            sb.append("Total utilisateurs,").append(d.totalUtilisateurs()).append('\n');
            sb.append("Taux de selection,").append(d.tauxSelection()).append("%\n");
            sb.append("Score moyen,").append(d.scoreMoyen()).append('\n');
            sb.append("\nStatut,Nombre\n");
            d.candidaturesParStatut().forEach((k, v) -> sb.append(k).append(',').append(v).append('\n'));
        } else {
            sb.append("RAPPORT STATISTIQUES — Portail RH\n");
            sb.append("Genere le ").append(LocalDate.now()).append("\n\n");
            sb.append("Total candidatures").append(sep).append(d.totalCandidatures()).append('\n');
            sb.append("Total offres").append(sep).append(d.totalOffres()).append('\n');
            sb.append("Offres publiees").append(sep).append(d.offresPubliees()).append('\n');
            sb.append("Total utilisateurs").append(sep).append(d.totalUtilisateurs()).append('\n');
            sb.append("Taux de selection").append(sep).append(d.tauxSelection()).append("%\n");
            sb.append("Score moyen").append(sep).append(d.scoreMoyen()).append('\n');
            sb.append("\nCandidatures par statut :\n");
            d.candidaturesParStatut().forEach((k, v) -> sb.append("  - ").append(k).append(sep).append(v).append('\n'));
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "rapport-statistiques-" + LocalDate.now()
                + (excel ? ".csv" : ".txt");
        MediaType mt = excel ? MediaType.parseMediaType("text/csv")
                             : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mt)
                .body(new ByteArrayResource(bytes));
    }
}
