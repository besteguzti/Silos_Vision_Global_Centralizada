package com.tfg.dashboard.dto.report;

/**
 * Estadistica mensual de afeccion para una plataforma monitorizada.
 */
public record MonthlyPlatformAffectationDto(
        String platform,
        Double averageAffectation,
        Integer maxAffectation,
        Integer greenDays,
        Integer yellowDays,
        Integer redDays,
        Integer daysWithData) {
}
