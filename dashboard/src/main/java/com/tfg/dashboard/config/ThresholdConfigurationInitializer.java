package com.tfg.dashboard.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.tfg.dashboard.service.KpiConfigurationService;

/**
 * Carga los umbrales por defecto al arrancar la aplicación y solo los crea si todavía no hay configuración guardada en base de datos.
 * Así el dashboard puede calcular los KPIs desde el primer inicio.
 */
@Component
public class ThresholdConfigurationInitializer implements ApplicationRunner {

    private final KpiConfigurationService kpiConfigurationService;

    public ThresholdConfigurationInitializer(KpiConfigurationService kpiConfigurationService) {
        this.kpiConfigurationService = kpiConfigurationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        kpiConfigurationService.ensureDefaultConfigurationExists();
    }
}

