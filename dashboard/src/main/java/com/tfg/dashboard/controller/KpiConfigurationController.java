package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.tfg.dashboard.dto.PlatformWeightsConfigurationDto;
import com.tfg.dashboard.dto.ThresholdConfigurationDto;
import com.tfg.dashboard.service.KpiConfigurationService;

import jakarta.validation.Valid;

// Endpoint de configuración editable de KPIs. Permite consultar, actualizar y restaurar umbrales y pesos. 

@RestController
@RequestMapping("/api/config")
@Validated
public class KpiConfigurationController {

    private final KpiConfigurationService kpiConfigurationService;

    public KpiConfigurationController(KpiConfigurationService kpiConfigurationService) {
        this.kpiConfigurationService = kpiConfigurationService;
    }

    @GetMapping("/thresholds")
    public ThresholdConfigurationDto getThresholds() {
        return kpiConfigurationService.getThresholds();
    }

    @PutMapping("/thresholds")
    public ThresholdConfigurationDto updateThresholds(@Valid @RequestBody ThresholdConfigurationDto request) {
        return kpiConfigurationService.updateThresholds(request);
    }

    @PostMapping("/thresholds/reset")
    public ThresholdConfigurationDto resetThresholds() {
        return kpiConfigurationService.resetConfiguration();
    }

    @GetMapping("/platform-weights")
    public PlatformWeightsConfigurationDto getPlatformWeights() {
        return kpiConfigurationService.getPlatformWeights();
    }

    @PutMapping("/platform-weights")
    public PlatformWeightsConfigurationDto updatePlatformWeights(
            @Valid @RequestBody PlatformWeightsConfigurationDto request) {
        return kpiConfigurationService.updatePlatformWeights(request);
    }
}
