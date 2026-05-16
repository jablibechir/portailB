package com.soprarh.portail.dashboard.controller;

import com.soprarh.portail.dashboard.dto.DashboardResponse;
import com.soprarh.portail.dashboard.service.DashboardService;
import com.soprarh.portail.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller du dashboard RH.
 * Fournit les KPIs et statistiques.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard
     * Retourne les KPIs et statistiques pour le RH.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VOIR_STATISTIQUES')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard charge"));
    }

    /**
     * GET /api/dashboard/stats
     * Alias attendu par le frontend pour les KPIs du dashboard.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VOIR_STATISTIQUES')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats() {
        DashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Statistiques du dashboard"));
    }
}

