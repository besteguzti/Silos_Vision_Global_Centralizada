package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.ExecutiveSummaryDto;
import com.tfg.dashboard.service.ExecutiveSummaryService;

/**
 * Endpoints del dashboard principal. Expone el resumen ejecutivo que se muestra en el panel.
 */
@RestController
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final ExecutiveSummaryService executiveSummaryService;

    public ApiDashboardController(ExecutiveSummaryService executiveSummaryService) {
        this.executiveSummaryService = executiveSummaryService;
    }
     
    @GetMapping("/executive-summary")
    public ExecutiveSummaryDto getExecutiveSummary() {
        return executiveSummaryService.getExecutiveSummary();
    }
}

