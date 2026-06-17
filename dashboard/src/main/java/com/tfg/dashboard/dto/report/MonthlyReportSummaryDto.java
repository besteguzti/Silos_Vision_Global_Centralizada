package com.tfg.dashboard.dto.report;

import java.time.LocalDate;

/**
 * Resumen agregado del periodo mensual solicitado.
 */
public record MonthlyReportSummaryDto(
        LocalDate startDate,
        LocalDate endDate,
        int daysRequested,
        int daysWithData,
        int totalSnapshots,
        int generatedScenarioSnapshots,
        int excludedGeneratedScenarioSnapshots,
        boolean includeGenerated,
        Double globalStatusValue,
        String globalStatusLevel,
        String worstDailyStatus,
        String summaryText) {
}
