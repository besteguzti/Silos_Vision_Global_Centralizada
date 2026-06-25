package com.tfg.dashboard.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.KpiStatus;

/**
 * Servicio común de scoring de KPIs.
 *
 * Traduce porcentajes de afección a estados GREEN/YELLOW/RED y construye DTOs
 * homogéneos para que el frontend no tenga que aplicar reglas de negocio ni
 * decidir colores por su cuenta.
 */
@Service
public class KpiScoringService {

    public static final String GREEN =
            "GREEN";

    public static final String YELLOW =
            "YELLOW";

    public static final String RED =
            "RED";

    public static final String NO_DATA =
            "NO_DATA";

    public static final String STALE =
            "STALE";

    private static final int HIGH_RELATION_INCREASE_THRESHOLD = 25;
    private static final int MODERATE_RELATION_INCREASE_THRESHOLD = 10;

    private final KpiProperties kpiProperties;

    public KpiScoringService(KpiProperties kpiProperties) {

        this.kpiProperties = kpiProperties;
    }

    /**
     * Devuelve el estado textual asociado a una afección normalizada.
     */
    public String statusFromAffection(
            double value
    ) {

        return statusEnumFromAffection(value).name();
    }

    /**
     * Devuelve el estado textual usando la configuración propia de un KPI
     * transversal. Esto evita que el umbral de Estado global actue como umbral
     * común para Criticidad, Disponibilidad, Presión, Backlog u otros KPIs.
     */
    public String statusFromTransversalKpi(
            String metricKey,
            double value
    ) {

        return statusEnumFromTransversalKpi(metricKey, value).name();
    }

    /**
     * Aplica los umbrales configurados para clasificar la afección.
     */
    public KpiStatus statusEnumFromAffection(
            double value
    ) {

        if (value >= kpiProperties.getStatus().getRedMin()) {

            return KpiStatus.RED;
        }

        if (value >= kpiProperties.getStatus().getYellowMin()) {

            return KpiStatus.YELLOW;
        }

        return KpiStatus.GREEN;
    }

    /**
     * Clasifica un KPI transversal segun su direccion:
     * RISK significa que 0 es bueno y 100 es critico; HEALTH significa que 0 es
     * critico y 100 es bueno.
     */
    public KpiStatus statusEnumFromTransversalKpi(
            String metricKey,
            double value
    ) {

        KpiProperties.TransversalKpiThreshold threshold =
                kpiProperties.getTransversal().thresholdFor(metricKey);

        if ("HEALTH".equalsIgnoreCase(threshold.getDirection())) {
            if (value >= threshold.getGreenMin()) {
                return KpiStatus.GREEN;
            }

            if (value >= threshold.getYellowMin()) {
                return KpiStatus.YELLOW;
            }

            return KpiStatus.RED;
        }

        if (value >= threshold.getRedMin()) {
            return KpiStatus.RED;
        }

        if (value >= threshold.getYellowMin()) {
            return KpiStatus.YELLOW;
        }

        return KpiStatus.GREEN;
    }

    /**
     * Ajusta el estado visual cuando la frescura indica NO_DATA o STALE.
     */
    public String statusFromFreshness(
            String freshness,
            String currentStatus
    ) {

        if (NO_DATA.equalsIgnoreCase(freshness)) {

            return NO_DATA;
        }

        if (STALE.equalsIgnoreCase(freshness)
                && GREEN.equalsIgnoreCase(currentStatus)) {

            return YELLOW;
        }

        return currentStatus;
    }

    /**
     * Genera una lectura simple para relaciónes aparentes entre plataformas.
     */
    public String relationReading(
            Integer cooccurrencePercentage,
            Integer averageIncrease
    ) {

        if (cooccurrencePercentage == null
                || averageIncrease == null) {

            return "Sin datos suficientes";
        }

        if (cooccurrencePercentage >= kpiProperties.getStatus().getRedMin()
                || averageIncrease >= HIGH_RELATION_INCREASE_THRESHOLD) {

            return "Alta";
        }

        if (cooccurrencePercentage >= kpiProperties.getStatus().getYellowMin()
                || averageIncrease >= MODERATE_RELATION_INCREASE_THRESHOLD) {

            return "Moderada";
        }

        return "Baja";
    }

    /**
     * Acota cualquier valor numérico a la escala común 0-100.
     */
    public int clampToInt(
            double value
    ) {

        if (value < 0) {

            return 0;
        }

        if (value > kpiProperties.getStatus().getMax()) {

            return kpiProperties.getStatus().getMax();
        }

        return (int) Math.round(value);
    }

    /**
     * Construye un KPI compuesto con metadatos, frescura y componentes.
     */
    public KpiResultDto kpi(
            String id,
            String name,
            Object value,
            String status,
            String description,
            String calculation,
            LocalDateTime timestamp,
            String freshness,
            List<KpiResultDto> components
    ) {

        return new KpiResultDto(
                id,
                name,
                value,
                KpiStatus.from(status),
                description,
                calculation,
                timestamp,
                freshness,
                null,
                components
        );
    }

    /**
     * Construye un componente interno de un KPI compuesto.
     */
    public KpiResultDto component(
            String id,
            String name,
            Object value,
            String status,
            Integer score
    ) {

        return new KpiResultDto(
                id,
                name,
                value,
                KpiStatus.from(status),
                null,
                null,
                null,
                null,
                score,
                List.of()
        );
    }
}

