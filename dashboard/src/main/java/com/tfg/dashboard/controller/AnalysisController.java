package com.tfg.dashboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.tfg.dashboard.dto.AnalyticsComparePoint;
import com.tfg.dashboard.dto.AnalysisSnapshotDto;
import com.tfg.dashboard.dto.OperationalImpactAnalysisResponse;
import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.service.TransversalKpiAnalyticsService;

import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints del panel analisis. Este panel busca identificar patrones y relaciones entre indicadores.
 */
@RestController
@RequestMapping("/api/analysis")
@Validated
public class AnalysisController {

    private final TransversalKpiAnalyticsService analyticsService;

    public AnalysisController(TransversalKpiAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/snapshots")
    public List<AnalysisSnapshotDto> snapshots(@RequestParam(defaultValue = "30d") @NotBlank String period) {
        return analyticsService.getAnalysisSnapshots(period).stream()
                .map(AnalysisSnapshotDto::new)
                .toList();
    }

    /**
     * Respuesta principal del panel de analisis. Devuelve los bloques agregados
     * que consume React: relaciones, evolucion temporal y relaciones especificas.
     */
    @GetMapping("/glpi-platform-relation")
    public OperationalImpactAnalysisResponse glpiPlatformRelation(
            @RequestParam(defaultValue = "30d") @NotBlank String period) {

        return analyticsService.getGlpiPlatformRelation(period);
    }

    /**
     * Devuelve puntos de degradacion tecnica frente a impacto en usuarios para
     * consultas auxiliares del modulo de analisis.
     */
    @GetMapping("/technical-degradation-impact")
    public List<AnalyticsComparePoint> technicalDegradationImpact(
            @RequestParam(defaultValue = "30d") @NotBlank String period) {

        return analyticsService.getTechnicalDegradationImpact(period);
    }

    @GetMapping("/platform-evolution")
    public List<TimelinePointDto> platformEvolution(
            @RequestParam(defaultValue = "30d") @NotBlank String period) {

        return analyticsService.getPlatformEvolution(period);
    }
}

