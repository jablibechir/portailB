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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
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
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 770;
                float margin = 50;

                // Title
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("Rapport Statistiques - Portail RH");
                cs.endText();
                y -= 25;

                // Date
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Genere le " + LocalDate.now());
                cs.endText();
                y -= 40;

                // KPIs
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Indicateurs cles");
                cs.endText();
                y -= 25;

                String[][] kpis = {
                        {"Total candidatures", String.valueOf(d.totalCandidatures())},
                        {"Total offres", String.valueOf(d.totalOffres())},
                        {"Offres publiees", String.valueOf(d.offresPubliees())},
                        {"Total utilisateurs", String.valueOf(d.totalUtilisateurs())},
                        {"Taux de selection", d.tauxSelection() + "%"},
                        {"Score moyen", String.valueOf(d.scoreMoyen())},
                };

                for (String[] kpi : kpis) {
                    cs.beginText();
                    cs.setFont(fontRegular, 11);
                    cs.newLineAtOffset(margin + 10, y);
                    cs.showText(kpi[0] + " : " + kpi[1]);
                    cs.endText();
                    y -= 18;
                }

                y -= 20;

                // Candidatures par statut
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Candidatures par statut");
                cs.endText();
                y -= 25;

                if (d.candidaturesParStatut() != null) {
                    for (Map.Entry<String, Long> entry : d.candidaturesParStatut().entrySet()) {
                        cs.beginText();
                        cs.setFont(fontRegular, 11);
                        cs.newLineAtOffset(margin + 10, y);
                        cs.showText(entry.getKey() + " : " + entry.getValue());
                        cs.endText();
                        y -= 18;
                    }
                }
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

            Sheet sheet = workbook.createSheet("Statistiques");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Rapport Statistiques - Portail RH");
            titleCell.setCellStyle(headerStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Genere le " + LocalDate.now());

            // KPIs header
            Row kpiHeader = sheet.createRow(3);
            Cell h1 = kpiHeader.createCell(0);
            h1.setCellValue("Indicateur");
            h1.setCellStyle(headerStyle);
            Cell h2 = kpiHeader.createCell(1);
            h2.setCellValue("Valeur");
            h2.setCellStyle(headerStyle);

            // KPI data
            int row = 4;
            sheet.createRow(row).createCell(0).setCellValue("Total candidatures");
            sheet.getRow(row++).createCell(1).setCellValue(d.totalCandidatures());
            sheet.createRow(row).createCell(0).setCellValue("Total offres");
            sheet.getRow(row++).createCell(1).setCellValue(d.totalOffres());
            sheet.createRow(row).createCell(0).setCellValue("Offres publiees");
            sheet.getRow(row++).createCell(1).setCellValue(d.offresPubliees());
            sheet.createRow(row).createCell(0).setCellValue("Total utilisateurs");
            sheet.getRow(row++).createCell(1).setCellValue(d.totalUtilisateurs());
            sheet.createRow(row).createCell(0).setCellValue("Taux de selection (%)");
            sheet.getRow(row++).createCell(1).setCellValue(d.tauxSelection());
            sheet.createRow(row).createCell(0).setCellValue("Score moyen");
            sheet.getRow(row++).createCell(1).setCellValue(d.scoreMoyen());

            // Candidatures par statut
            row += 1;
            Row statutHeader = sheet.createRow(row++);
            Cell sh1 = statutHeader.createCell(0);
            sh1.setCellValue("Statut");
            sh1.setCellStyle(headerStyle);
            Cell sh2 = statutHeader.createCell(1);
            sh2.setCellValue("Nombre");
            sh2.setCellStyle(headerStyle);

            if (d.candidaturesParStatut() != null) {
                for (Map.Entry<String, Long> entry : d.candidaturesParStatut().entrySet()) {
                    Row r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(entry.getKey());
                    r.createCell(1).setCellValue(entry.getValue());
                }
            }

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

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
