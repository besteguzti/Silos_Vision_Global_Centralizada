package com.tfg.dashboard.dto.report;

import java.time.LocalDate;

/**
 * Dia con mayor severidad observada dentro del periodo mensual.
 */
public record MonthlyWorstDayDto(
        LocalDate date,
        Integer severityScore,
        String reason,
        Boolean generatedScenario) {
}
