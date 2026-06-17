package com.tfg.dashboard.dto.report;

/**
 * Media mensual de un KPI transversal persistido en historico.
 */
public record MonthlyKpiAverageDto(
        String code,
        String name,
        Double average,
        Double maximum,
        Integer samples,
        String unit) {
}
