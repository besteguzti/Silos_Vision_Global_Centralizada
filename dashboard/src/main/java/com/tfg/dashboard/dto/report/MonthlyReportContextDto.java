package com.tfg.dashboard.dto.report;

import java.util.List;

/**
 * Contexto mensual estructurado para futuros informes.
 *
 * Agrupa datos historicos ya persistidos para revisarlos desde un cliente HTTP
 * o reutilizarlos en informes posteriores.
 */
public record MonthlyReportContextDto(
        String period,
        MonthlyReportSummaryDto monthlySummary,
        List<MonthlyKpiAverageDto> transversalKpiAverages,
        List<MonthlyPlatformAffectationDto> platformAffectations,
        List<MonthlyDailySummaryDto> dailySummaries,
        List<MonthlyWorstDayDto> worstDays,
        List<String> frequentFindings,
        List<String> limitations) {
}
