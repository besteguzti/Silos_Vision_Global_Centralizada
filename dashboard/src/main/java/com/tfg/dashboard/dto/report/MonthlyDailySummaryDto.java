package com.tfg.dashboard.dto.report;

import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de un dia dentro del contexto mensual.
 */
public record MonthlyDailySummaryDto(
        LocalDate date,
        Integer snapshotCount,
        Boolean hasData,
        Boolean containsGeneratedScenario,
        Integer globalStatusValue,
        String globalStatusLevel,
        Integer arubaAffectation,
        Integer citrixAffectation,
        Integer microsoft365Affectation,
        Integer glpiAffectation,
        Integer glpiOperationalPressure,
        Integer technicalDegradation,
        Integer userImpact,
        Integer affectedServicesPercent,
        String worstDailyStatus,
        List<String> findings) {
}
