package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.service.GlpiService;


 //Endpoints de GLPI. Usa el último snapshot guardado para mostrar tickets, SLA y estado operativo.

@RestController
@RequestMapping("/glpi")
public class GlpiController {

    private final GlpiService glpiService;

    public GlpiController(GlpiService glpiService) {
        this.glpiService = glpiService;
    }

    /**
     * Devuelve el resumen actual de GLPI.
     */
    @GetMapping("/summary")
    public GlpiSummary getSummary() {
        return glpiService.getSummary();
    }
}

