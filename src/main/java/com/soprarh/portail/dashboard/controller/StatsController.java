package com.soprarh.portail.dashboard.controller;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.dashboard.dto.DashboardResponse;
import com.soprarh.portail.dashboard.service.DashboardService;
import com.soprarh.portail.evaluation.repository.EntretienRepository;
import com.soprarh.portail.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    @PreAuthorize("hasAuthority('VOIR_STATISTIQUES')")
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
    @PreAuthorize("hasAuthority('VOIR_STATISTIQUES')")
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
    @PreAuthorize("hasAuthority('VOIR_STATISTIQUES')")
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
     * Export des statistiques en PDF (Apache PDFBox) ou Excel XLSX (Apache POI).
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('EXPORTER_RAPPORTS')")
    public ResponseEntity<ByteArrayResource> exportRapport(
            @RequestParam(defaultValue = "pdf") String format) {

        DashboardResponse d = dashboardService.getDashboard();

        try {
            if ("excel".equalsIgnoreCase(format)) {
                return exportExcel(d);
            } else {
                return exportPdf(d);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la generation du rapport: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<ByteArrayResource> exportPdf(DashboardResponse d) throws Exception {
        // Gather extra data for enriched report
        List<Candidature> allCandidatures = candidatureRepository.findAllOrderByDateDesc();
        long totalEntretiens = entretienRepository.count();

        // Candidatures par mois (12 mois)
        Map<Integer, Long> parMois = allCandidatures.stream()
                .filter(c -> c.getDateSoumission() != null)
                .collect(Collectors.groupingBy(c -> c.getDateSoumission().getMonthValue(), Collectors.counting()));

        // Top offres
        Map<UUID, Long> topCounts = allCandidatures.stream()
                .filter(c -> c.getOffre() != null)
                .collect(Collectors.groupingBy(c -> c.getOffre().getId(), Collectors.counting()));
        Map<UUID, String> topTitres = allCandidatures.stream()
                .filter(c -> c.getOffre() != null)
                .collect(Collectors.toMap(c -> c.getOffre().getId(),
                        c -> c.getOffre().getTitre() != null ? c.getOffre().getTitre() : "Offre", (a, b) -> a));
        List<Map.Entry<UUID, Long>> topOffresEntries = topCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed()).limit(5).toList();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Brand colors
            float[] purple = {77f/255, 28f/255, 135f/255};
            float[] magenta = {174f/255, 31f/255, 131f/255};
            float[] gold = {255f/255, 202f/255, 35f/255};
            float[] white = {1f, 1f, 1f};
            float[] lightGray = {0.96f, 0.96f, 0.98f};
            float[] darkText = {0.1f, 0.1f, 0.1f};
            float[] green = {5f/255, 150f/255, 105f/255};
            float[] blue = {29f/255, 78f/255, 216f/255};
            float[] red = {220f/255, 38f/255, 38f/255};
            float margin = 40;
            float contentWidth = pageWidth - 2 * margin;
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // ═══════════════════════════════════════════════════════════════════
            // PAGE 1 - KPIs + Statut Bar Chart
            // ═══════════════════════════════════════════════════════════════════
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream cs = new PDPageContentStream(document, page1)) {
                // === HEADER ===
                float headerHeight = 70;
                float headerY = pageHeight - headerHeight;
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.addRect(0, headerY, pageWidth, headerHeight);
                cs.fill();
                cs.setNonStrokingColor(gold[0], gold[1], gold[2]);
                cs.addRect(0, headerY - 4, pageWidth, 4);
                cs.fill();

                try {
                    ClassPathResource logoRes = new ClassPathResource("logo.png");
                    if (logoRes.exists()) {
                        byte[] logoBytes;
                        try (InputStream is = logoRes.getInputStream()) { logoBytes = is.readAllBytes(); }
                        PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoBytes, "logo");
                        float logoH = 45;
                        float logoW = logoH * ((float) logo.getWidth() / logo.getHeight());
                        cs.drawImage(logo, margin, headerY + 12, logoW, logoH);
                    }
                } catch (Exception ignored) {}

                cs.beginText();
                cs.setFont(fontBold, 20);
                cs.setNonStrokingColor(white[0], white[1], white[2]);
                cs.newLineAtOffset(margin + 60, headerY + 30);
                cs.showText("Rapport Statistiques");
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setNonStrokingColor(gold[0], gold[1], gold[2]);
                cs.newLineAtOffset(margin + 60, headerY + 14);
                cs.showText("Portail RH - Sopra HR Software");
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.setNonStrokingColor(white[0], white[1], white[2]);
                cs.newLineAtOffset(pageWidth - margin - 80, headerY + 30);
                cs.showText(dateStr);
                cs.endText();

                float y = headerY - 30;

                // === KPI CARDS (2 rows x 3) ===
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.newLineAtOffset(margin, y);
                cs.showText("Indicateurs Cles");
                cs.endText();
                y -= 20;

                String[][] kpis = {
                        {"Candidatures", String.valueOf(d.totalCandidatures())},
                        {"Offres", String.valueOf(d.totalOffres())},
                        {"Publiees", String.valueOf(d.offresPubliees())},
                        {"Utilisateurs", String.valueOf(d.totalUtilisateurs())},
                        {"Taux Selection", String.format("%.1f%%", d.tauxSelection())},
                        {"Score Moyen", String.format("%.1f", d.scoreMoyen())},
                        {"Entretiens", String.valueOf(totalEntretiens)},
                        {"Offres/Candidat", d.totalCandidatures() > 0 ? String.format("%.1f", (double)d.totalOffres()/d.totalCandidatures()) : "0"},
                        {"Taux Rejet", d.candidaturesParStatut() != null ? String.format("%.1f%%", 100.0 * (d.candidaturesParStatut().getOrDefault("rejetee_rh", 0L) + d.candidaturesParStatut().getOrDefault("rejetee_manager", 0L)) / Math.max(1, d.totalCandidatures())) : "0%"},
                };

                float cardW = (contentWidth - 20) / 3;
                float cardH = 50;
                float[][] cardColors = {purple, magenta, green, blue, purple, magenta, green, blue, red};

                for (int i = 0; i < kpis.length; i++) {
                    int col = i % 3;
                    int rowIdx = i / 3;
                    float cx = margin + col * (cardW + 10);
                    float cy = y - rowIdx * (cardH + 10);

                    cs.setNonStrokingColor(cardColors[i % cardColors.length][0], cardColors[i % cardColors.length][1], cardColors[i % cardColors.length][2]);
                    cs.addRect(cx, cy - cardH, cardW, cardH);
                    cs.fill();

                    cs.beginText();
                    cs.setFont(fontBold, 16);
                    cs.setNonStrokingColor(white[0], white[1], white[2]);
                    cs.newLineAtOffset(cx + 12, cy - 22);
                    cs.showText(kpis[i][1]);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(fontRegular, 9);
                    cs.setNonStrokingColor(gold[0], gold[1], gold[2]);
                    cs.newLineAtOffset(cx + 12, cy - 38);
                    cs.showText(kpis[i][0]);
                    cs.endText();
                }

                y -= 3 * (cardH + 10) + 30;

                // === BAR CHART - Candidatures par Statut ===
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.newLineAtOffset(margin, y);
                cs.showText("Repartition par Statut");
                cs.endText();
                y -= 15;

                if (d.candidaturesParStatut() != null && !d.candidaturesParStatut().isEmpty()) {
                    Map<String, Long> statuts = d.candidaturesParStatut();
                    long maxVal = statuts.values().stream().mapToLong(v -> v).max().orElse(1);
                    float barMaxWidth = contentWidth - 130;
                    float barHeight = 18;
                    float barGap = 6;

                    float[][] barColors = {green, blue, purple, magenta, red, gold, darkText};
                    int colorIdx = 0;

                    for (Map.Entry<String, Long> entry : statuts.entrySet()) {
                        float barW = Math.max(4, (float) entry.getValue() / maxVal * barMaxWidth);
                        float[] barColor = barColors[colorIdx % barColors.length];

                        // Bar background (light gray)
                        cs.setNonStrokingColor(lightGray[0], lightGray[1], lightGray[2]);
                        cs.addRect(margin + 120, y - barHeight, barMaxWidth, barHeight);
                        cs.fill();

                        // Bar fill
                        cs.setNonStrokingColor(barColor[0], barColor[1], barColor[2]);
                        cs.addRect(margin + 120, y - barHeight, barW, barHeight);
                        cs.fill();

                        // Label
                        cs.beginText();
                        cs.setFont(fontRegular, 8);
                        cs.setNonStrokingColor(darkText[0], darkText[1], darkText[2]);
                        cs.newLineAtOffset(margin, y - 13);
                        String lbl = entry.getKey().replace("_", " ");
                        lbl = lbl.length() > 16 ? lbl.substring(0, 16) : lbl;
                        cs.showText(lbl);
                        cs.endText();

                        // Value
                        cs.beginText();
                        cs.setFont(fontBold, 9);
                        cs.setNonStrokingColor(barColor[0], barColor[1], barColor[2]);
                        cs.newLineAtOffset(margin + 125 + barW, y - 13);
                        cs.showText(String.valueOf(entry.getValue()));
                        cs.endText();

                        y -= (barHeight + barGap);
                        colorIdx++;
                    }
                }

                // === FOOTER PAGE 1 ===
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.addRect(0, 30, pageWidth, 2);
                cs.fill();
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                cs.newLineAtOffset(margin, 16);
                cs.showText("Sopra HR Software - Document confidentiel - " + dateStr + " - Page 1/2");
                cs.endText();
            }

            // ═══════════════════════════════════════════════════════════════════
            // PAGE 2 - Monthly Chart + Top Offres + Statut Table
            // ═══════════════════════════════════════════════════════════════════
            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);

            try (PDPageContentStream cs = new PDPageContentStream(document, page2)) {
                // Mini header
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.addRect(0, pageHeight - 35, pageWidth, 35);
                cs.fill();
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.setNonStrokingColor(white[0], white[1], white[2]);
                cs.newLineAtOffset(margin, pageHeight - 24);
                cs.showText("Rapport Statistiques - Analyses Detaillees");
                cs.endText();
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.setNonStrokingColor(gold[0], gold[1], gold[2]);
                cs.newLineAtOffset(pageWidth - margin - 70, pageHeight - 24);
                cs.showText(dateStr);
                cs.endText();

                float y = pageHeight - 60;

                // === BAR CHART - Candidatures par Mois ===
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.newLineAtOffset(margin, y);
                cs.showText("Candidatures par Mois");
                cs.endText();
                y -= 20;

                String[] moisLabels = {"Jan", "Fev", "Mar", "Avr", "Mai", "Jun", "Jul", "Aou", "Sep", "Oct", "Nov", "Dec"};
                long maxMois = parMois.values().stream().mapToLong(v -> v).max().orElse(1);
                float chartHeight = 120;
                float chartWidth = contentWidth;
                float barWidth = chartWidth / 12 - 6;

                // Y-axis background
                cs.setNonStrokingColor(lightGray[0], lightGray[1], lightGray[2]);
                cs.addRect(margin, y - chartHeight, chartWidth, chartHeight);
                cs.fill();

                // Grid lines
                cs.setNonStrokingColor(0.88f, 0.88f, 0.9f);
                for (int g = 1; g <= 4; g++) {
                    float gy = y - chartHeight + (chartHeight * g / 5);
                    cs.addRect(margin, gy, chartWidth, 0.5f);
                    cs.fill();
                }

                // Bars
                for (int m = 1; m <= 12; m++) {
                    long val = parMois.getOrDefault(m, 0L);
                    float barH = maxMois > 0 ? (float) val / maxMois * (chartHeight - 15) : 0;
                    float bx = margin + (m - 1) * (barWidth + 6) + 3;
                    float by = y - chartHeight;

                    // Gradient-like bar (purple to magenta based on value)
                    float ratio = maxMois > 0 ? (float)val / maxMois : 0;
                    float[] barColor = {
                            purple[0] * (1 - ratio) + magenta[0] * ratio,
                            purple[1] * (1 - ratio) + magenta[1] * ratio,
                            purple[2] * (1 - ratio) + magenta[2] * ratio
                    };
                    cs.setNonStrokingColor(barColor[0], barColor[1], barColor[2]);
                    cs.addRect(bx, by, barWidth, barH);
                    cs.fill();

                    // Value on top
                    if (val > 0) {
                        cs.beginText();
                        cs.setFont(fontBold, 7);
                        cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                        cs.newLineAtOffset(bx + barWidth / 2 - 4, by + barH + 2);
                        cs.showText(String.valueOf(val));
                        cs.endText();
                    }

                    // Month label
                    cs.beginText();
                    cs.setFont(fontRegular, 7);
                    cs.setNonStrokingColor(darkText[0], darkText[1], darkText[2]);
                    cs.newLineAtOffset(bx + barWidth / 2 - 6, by - 12);
                    cs.showText(moisLabels[m - 1]);
                    cs.endText();
                }

                y -= chartHeight + 35;

                // === TOP 5 OFFRES (horizontal bars) ===
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.newLineAtOffset(margin, y);
                cs.showText("Top 5 Offres les Plus Demandees");
                cs.endText();
                y -= 18;

                if (!topOffresEntries.isEmpty()) {
                    long topMax = topOffresEntries.get(0).getValue();
                    float topBarMax = contentWidth - 180;
                    float topBarH = 20;

                    for (int i = 0; i < topOffresEntries.size(); i++) {
                        Map.Entry<UUID, Long> entry = topOffresEntries.get(i);
                        String titre = topTitres.getOrDefault(entry.getKey(), "Offre");
                        if (titre.length() > 22) titre = titre.substring(0, 22) + "...";
                        float bW = (float) entry.getValue() / topMax * topBarMax;
                        float ratio = (float)(topOffresEntries.size() - i) / topOffresEntries.size();

                        // Bar
                        cs.setNonStrokingColor(
                                purple[0] * ratio + gold[0] * (1 - ratio),
                                purple[1] * ratio + gold[1] * (1 - ratio),
                                purple[2] * ratio + gold[2] * (1 - ratio));
                        cs.addRect(margin + 160, y - topBarH, bW, topBarH);
                        cs.fill();

                        // Rank + title
                        cs.beginText();
                        cs.setFont(fontBold, 9);
                        cs.setNonStrokingColor(darkText[0], darkText[1], darkText[2]);
                        cs.newLineAtOffset(margin, y - 14);
                        cs.showText((i + 1) + ". " + titre);
                        cs.endText();

                        // Value
                        cs.beginText();
                        cs.setFont(fontBold, 9);
                        cs.setNonStrokingColor(white[0], white[1], white[2]);
                        cs.newLineAtOffset(margin + 165, y - 14);
                        cs.showText(String.valueOf(entry.getValue()));
                        cs.endText();

                        y -= (topBarH + 5);
                    }
                }

                y -= 20;

                // === DETAILED TABLE - Candidatures par Statut ===
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.newLineAtOffset(margin, y);
                cs.showText("Detail Candidatures par Statut");
                cs.endText();
                y -= 18;

                if (d.candidaturesParStatut() != null && !d.candidaturesParStatut().isEmpty()) {
                    float tableW = contentWidth;
                    float rowH = 20;
                    float col1W = tableW * 0.45f;
                    float col2W = tableW * 0.2f;
                    float col3W = tableW * 0.35f;

                    // Header
                    cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                    cs.addRect(margin, y - rowH, tableW, rowH);
                    cs.fill();

                    cs.beginText(); cs.setFont(fontBold, 9); cs.setNonStrokingColor(white[0], white[1], white[2]);
                    cs.newLineAtOffset(margin + 8, y - 14); cs.showText("Statut"); cs.endText();
                    cs.beginText(); cs.setFont(fontBold, 9); cs.setNonStrokingColor(white[0], white[1], white[2]);
                    cs.newLineAtOffset(margin + col1W + 8, y - 14); cs.showText("Nombre"); cs.endText();
                    cs.beginText(); cs.setFont(fontBold, 9); cs.setNonStrokingColor(white[0], white[1], white[2]);
                    cs.newLineAtOffset(margin + col1W + col2W + 8, y - 14); cs.showText("Pourcentage"); cs.endText();

                    y -= rowH;

                    long totalStatut = d.candidaturesParStatut().values().stream().mapToLong(v -> v).sum();
                    int rowIndex = 0;
                    for (Map.Entry<String, Long> entry : d.candidaturesParStatut().entrySet()) {
                        if (rowIndex % 2 == 0) {
                            cs.setNonStrokingColor(lightGray[0], lightGray[1], lightGray[2]);
                            cs.addRect(margin, y - rowH, tableW, rowH);
                            cs.fill();
                        }

                        String statusLabel = entry.getKey().replace("_", " ");
                        statusLabel = statusLabel.substring(0, 1).toUpperCase() + statusLabel.substring(1);
                        double pct = totalStatut > 0 ? 100.0 * entry.getValue() / totalStatut : 0;

                        cs.beginText(); cs.setFont(fontRegular, 9); cs.setNonStrokingColor(darkText[0], darkText[1], darkText[2]);
                        cs.newLineAtOffset(margin + 8, y - 14); cs.showText(statusLabel); cs.endText();

                        cs.beginText(); cs.setFont(fontBold, 9); cs.setNonStrokingColor(magenta[0], magenta[1], magenta[2]);
                        cs.newLineAtOffset(margin + col1W + 8, y - 14); cs.showText(String.valueOf(entry.getValue())); cs.endText();

                        // Percentage bar
                        float pctBarW = (float)(pct / 100.0 * (col3W - 50));
                        cs.setNonStrokingColor(green[0], green[1], green[2]);
                        cs.addRect(margin + col1W + col2W + 8, y - rowH + 4, pctBarW, 12);
                        cs.fill();

                        cs.beginText(); cs.setFont(fontRegular, 8); cs.setNonStrokingColor(darkText[0], darkText[1], darkText[2]);
                        cs.newLineAtOffset(margin + col1W + col2W + 14 + pctBarW, y - 14); cs.showText(String.format("%.1f%%", pct)); cs.endText();

                        y -= rowH;
                        rowIndex++;
                    }

                    // Gold bottom line
                    cs.setNonStrokingColor(gold[0], gold[1], gold[2]);
                    cs.addRect(margin, y, tableW, 2);
                    cs.fill();
                }

                // === FOOTER PAGE 2 ===
                cs.setNonStrokingColor(purple[0], purple[1], purple[2]);
                cs.addRect(0, 30, pageWidth, 2);
                cs.fill();
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                cs.newLineAtOffset(margin, 16);
                cs.showText("Sopra HR Software - Document confidentiel - " + dateStr + " - Page 2/2");
                cs.endText();
            }

            document.save(baos);
            byte[] bytes = baos.toByteArray();
            String filename = "rapport-statistiques-" + LocalDate.now() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(bytes));
        }
    }

    private ResponseEntity<ByteArrayResource> exportExcel(DashboardResponse d) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Statistiques");

            // Brand colors
            byte[] purpleRgb = {(byte) 77, (byte) 28, (byte) 135};      // #4D1C87
            byte[] magentaRgb = {(byte) 174, (byte) 31, (byte) 131};    // #AE1F83
            byte[] goldRgb = {(byte) 255, (byte) 202, (byte) 35};       // #FFCA23
            byte[] whiteRgb = {(byte) 255, (byte) 255, (byte) 255};
            byte[] lightPurple = {(byte) 240, (byte) 230, (byte) 255};  // light purple bg

            XSSFColor purpleColor = new XSSFColor(purpleRgb, null);
            XSSFColor magentaColor = new XSSFColor(magentaRgb, null);
            XSSFColor goldColor = new XSSFColor(goldRgb, null);
            XSSFColor whiteColor = new XSSFColor(whiteRgb, null);
            XSSFColor lightPurpleColor = new XSSFColor(lightPurple, null);

            // === STYLES ===
            // Title style (large, purple text)
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);
            titleFont.setColor(purpleColor);
            titleStyle.setFont(titleFont);

            // Subtitle style
            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleFont.setColor(new XSSFColor(new byte[]{(byte) 100, (byte) 100, (byte) 100}, null));
            subtitleStyle.setFont(subtitleFont);

            // Section header (purple bg, white text, gold bottom border)
            XSSFCellStyle sectionStyle = workbook.createCellStyle();
            XSSFFont sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 12);
            sectionFont.setColor(whiteColor);
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(purpleColor);
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setBorderBottom(BorderStyle.MEDIUM);
            sectionStyle.setBottomBorderColor(goldColor);

            // Table header (magenta bg, white text)
            XSSFCellStyle tableHeaderStyle = workbook.createCellStyle();
            XSSFFont tableHeaderFont = workbook.createFont();
            tableHeaderFont.setBold(true);
            tableHeaderFont.setFontHeightInPoints((short) 10);
            tableHeaderFont.setColor(whiteColor);
            tableHeaderStyle.setFont(tableHeaderFont);
            tableHeaderStyle.setFillForegroundColor(magentaColor);
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
            tableHeaderStyle.setBorderTop(BorderStyle.THIN);
            tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
            tableHeaderStyle.setBorderRight(BorderStyle.THIN);

            // KPI label style (light purple bg, bold)
            XSSFCellStyle kpiLabelStyle = workbook.createCellStyle();
            XSSFFont kpiLabelFont = workbook.createFont();
            kpiLabelFont.setBold(true);
            kpiLabelFont.setFontHeightInPoints((short) 10);
            kpiLabelFont.setColor(purpleColor);
            kpiLabelStyle.setFont(kpiLabelFont);
            kpiLabelStyle.setFillForegroundColor(lightPurpleColor);
            kpiLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiLabelStyle.setBorderBottom(BorderStyle.THIN);
            kpiLabelStyle.setBorderLeft(BorderStyle.THIN);

            // KPI value style (light purple bg, magenta text)
            XSSFCellStyle kpiValueStyle = workbook.createCellStyle();
            XSSFFont kpiValueFont = workbook.createFont();
            kpiValueFont.setBold(true);
            kpiValueFont.setFontHeightInPoints((short) 12);
            kpiValueFont.setColor(magentaColor);
            kpiValueStyle.setFont(kpiValueFont);
            kpiValueStyle.setFillForegroundColor(lightPurpleColor);
            kpiValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiValueStyle.setAlignment(HorizontalAlignment.CENTER);
            kpiValueStyle.setBorderBottom(BorderStyle.THIN);
            kpiValueStyle.setBorderRight(BorderStyle.THIN);

            // Data row even
            XSSFCellStyle dataEvenStyle = workbook.createCellStyle();
            dataEvenStyle.setBorderBottom(BorderStyle.THIN);
            dataEvenStyle.setBorderLeft(BorderStyle.THIN);
            dataEvenStyle.setBorderRight(BorderStyle.THIN);

            // Data row odd (light background)
            XSSFCellStyle dataOddStyle = workbook.createCellStyle();
            dataOddStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 248, (byte) 245, (byte) 252}, null));
            dataOddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataOddStyle.setBorderBottom(BorderStyle.THIN);
            dataOddStyle.setBorderLeft(BorderStyle.THIN);
            dataOddStyle.setBorderRight(BorderStyle.THIN);

            // Value centered styles
            XSSFCellStyle dataEvenCenter = workbook.createCellStyle();
            dataEvenCenter.cloneStyleFrom(dataEvenStyle);
            dataEvenCenter.setAlignment(HorizontalAlignment.CENTER);
            XSSFCellStyle dataOddCenter = workbook.createCellStyle();
            dataOddCenter.cloneStyleFrom(dataOddStyle);
            dataOddCenter.setAlignment(HorizontalAlignment.CENTER);

            // === CONTENT ===
            int rowNum = 0;

            // Title
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Rapport Statistiques - Portail RH");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            // Date
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateCell.setCellValue("Sopra HR Software - Genere le " + dateStr);
            dateCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

            rowNum++; // spacer

            // KPIs section header
            Row kpiHeaderRow = sheet.createRow(rowNum++);
            kpiHeaderRow.setHeightInPoints(22);
            Cell kpiHeaderCell = kpiHeaderRow.createCell(0);
            kpiHeaderCell.setCellValue("INDICATEURS CLES");
            kpiHeaderCell.setCellStyle(sectionStyle);
            Cell kpiHeaderCell2 = kpiHeaderRow.createCell(1);
            kpiHeaderCell2.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            // KPI data
            String[][] kpis = {
                    {"Total Candidatures", String.valueOf(d.totalCandidatures())},
                    {"Total Offres", String.valueOf(d.totalOffres())},
                    {"Offres Publiees", String.valueOf(d.offresPubliees())},
                    {"Total Utilisateurs", String.valueOf(d.totalUtilisateurs())},
                    {"Taux de Selection", String.format("%.1f%%", d.tauxSelection())},
                    {"Score Moyen", String.format("%.1f", d.scoreMoyen())},
            };

            for (String[] kpi : kpis) {
                Row r = sheet.createRow(rowNum++);
                r.setHeightInPoints(20);
                Cell label = r.createCell(0);
                label.setCellValue(kpi[0]);
                label.setCellStyle(kpiLabelStyle);
                Cell value = r.createCell(1);
                value.setCellValue(kpi[1]);
                value.setCellStyle(kpiValueStyle);
            }

            rowNum++; // spacer

            // Candidatures par statut section
            Row statutSectionRow = sheet.createRow(rowNum++);
            statutSectionRow.setHeightInPoints(22);
            Cell statutSectionCell = statutSectionRow.createCell(0);
            statutSectionCell.setCellValue("CANDIDATURES PAR STATUT");
            statutSectionCell.setCellStyle(sectionStyle);
            Cell statutSectionCell2 = statutSectionRow.createCell(1);
            statutSectionCell2.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            // Table header
            Row tHeaderRow = sheet.createRow(rowNum++);
            tHeaderRow.setHeightInPoints(18);
            Cell th1 = tHeaderRow.createCell(0);
            th1.setCellValue("Statut");
            th1.setCellStyle(tableHeaderStyle);
            Cell th2 = tHeaderRow.createCell(1);
            th2.setCellValue("Nombre");
            th2.setCellStyle(tableHeaderStyle);

            // Table data
            if (d.candidaturesParStatut() != null) {
                int dataIdx = 0;
                for (Map.Entry<String, Long> entry : d.candidaturesParStatut().entrySet()) {
                    Row r = sheet.createRow(rowNum++);
                    r.setHeightInPoints(18);
                    boolean odd = dataIdx % 2 == 1;

                    Cell c1 = r.createCell(0);
                    String statusLabel = entry.getKey().replace("_", " ");
                    statusLabel = statusLabel.substring(0, 1).toUpperCase() + statusLabel.substring(1);
                    c1.setCellValue(statusLabel);
                    c1.setCellStyle(odd ? dataOddStyle : dataEvenStyle);

                    Cell c2 = r.createCell(1);
                    c2.setCellValue(entry.getValue());
                    c2.setCellStyle(odd ? dataOddCenter : dataEvenCenter);

                    dataIdx++;
                }
            }

            // Gold separator at end
            rowNum++;
            Row sepRow = sheet.createRow(rowNum);
            XSSFCellStyle sepStyle = workbook.createCellStyle();
            sepStyle.setFillForegroundColor(goldColor);
            sepStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sepRow.createCell(0).setCellStyle(sepStyle);
            sepRow.createCell(1).setCellStyle(sepStyle);

            // Auto-size columns
            sheet.setColumnWidth(0, 7000);
            sheet.setColumnWidth(1, 5000);

            workbook.write(baos);
            byte[] bytes = baos.toByteArray();
            String filename = "rapport-statistiques-" + LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new ByteArrayResource(bytes));
        }
    }
}
