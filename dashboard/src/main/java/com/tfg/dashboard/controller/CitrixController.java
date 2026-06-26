package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.service.CitrixService;

//Endpoints de Citrix. Usa el último snapshot guardado en MySQL para mostrar los datos en el panel.

@RestController
@RequestMapping("/citrix")
public class CitrixController {

    private final CitrixService citrixService;

    public CitrixController(CitrixService citrixService) {
        this.citrixService = citrixService;
    }

    // Devuelve el resumen actual de Citrix.

    @GetMapping("/summary")
    public CitrixSummary getSummary() {
        return citrixService.getSummary();
    }
}
