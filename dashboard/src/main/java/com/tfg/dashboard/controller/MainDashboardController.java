package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.service.MainDashboardService;

/**
 * Publica el resumen que usa la pantalla principal del dashboard.
 *
 * La ruta se mantiene porque es la que consume React. El cálculo de los KPIs no
 * se hace aquí, sino en MainDashboardService; este controlador solo actúa como
 * punto de entrada HTTP.
 */
@RestController
@RequestMapping("/dashboard")
public class MainDashboardController {

    private final MainDashboardService mainDashboardService;

    public MainDashboardController(MainDashboardService mainDashboardService) {
        this.mainDashboardService = mainDashboardService;
    }

    /**
     * Devuelve los KPIs transversales calculados desde las plataformas monitorizadas.
     */
    @GetMapping("/summary")
    public MainDashboardSummary getSummary() {

        return mainDashboardService.getSummary();
    }
}

