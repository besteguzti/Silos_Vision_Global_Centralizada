package com.tfg.dashboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.KpiDefinitionDto;
import com.tfg.dashboard.service.KpiDefinitionService;

/**
 * Endpoint documental de KPIs. Expone definiciones, fórmulas, fuentes y umbrales del proyecto usado como catalogo de referencia para el desarrollo.
 */
@RestController
@RequestMapping("/api/kpis")
public class KpiDefinitionController {

    private final KpiDefinitionService kpiDefinitionService;

    public KpiDefinitionController(KpiDefinitionService kpiDefinitionService) {
        this.kpiDefinitionService = kpiDefinitionService;
    }

    // Devuelve el catálogo de KPIs de plataforma, dashboard general y análisis.
     
    @GetMapping("/definitions")
    public List<KpiDefinitionDto> getDefinitions() {
        return kpiDefinitionService.getDefinitions();
    }
}

