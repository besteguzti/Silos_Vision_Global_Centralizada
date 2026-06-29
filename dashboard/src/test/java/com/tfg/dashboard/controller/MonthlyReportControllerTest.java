package com.tfg.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.report.MonthlyDailySummaryDto;
import com.tfg.dashboard.dto.report.MonthlyKpiAverageDto;
import com.tfg.dashboard.dto.report.MonthlyPlatformAffectationDto;
import com.tfg.dashboard.dto.report.MonthlyReportContextDto;
import com.tfg.dashboard.dto.report.MonthlyReportSummaryDto;
import com.tfg.dashboard.dto.report.MonthlyWorstDayDto;
import com.tfg.dashboard.service.MonthlyReportContextService;

@WebMvcTest(MonthlyReportController.class)
class MonthlyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonthlyReportContextService monthlyReportContextService;

    @Test
    void monthlyContextReturnsStructuredResponse() throws Exception {

        when(monthlyReportContextService.buildMonthlyContext("LAST_30_DAYS", false))
                .thenReturn(response(false));

        mockMvc.perform(get("/api/reports/monthly-context")
                        .param("period", "LAST_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_30_DAYS"))
                .andExpect(jsonPath("$.monthlySummary.daysRequested").value(30))
                .andExpect(jsonPath("$.monthlySummary.includeGenerated").value(false))
                .andExpect(jsonPath("$.monthlySummary.globalStatusValue").value(42.0))
                .andExpect(jsonPath("$.monthlySummary.globalStatusLevel").value("YELLOW"))
                .andExpect(jsonPath("$.monthlySummary.worstDailyStatus").value("YELLOW"))
                .andExpect(jsonPath("$.transversalKpiAverages[0].code").value("global_status"))
                .andExpect(jsonPath("$.platformAffectations[0].platform").value("Aruba"))
                .andExpect(jsonPath("$.dailySummaries[0].arubaAffectation").value(30))
                .andExpect(jsonPath("$.dailySummaries[0].globalStatusLevel").value("YELLOW"))
                .andExpect(jsonPath("$.dailySummaries[0].worstDailyStatus").value("YELLOW"))
                .andExpect(jsonPath("$.dailySummaries[0].arubaHealth").doesNotExist())
                .andExpect(jsonPath("$.worstDays[0].severityScore").value(60))
                .andExpect(jsonPath("$.limitations").isArray());
    }

    @Test
    void monthlyContextPassesIncludeGeneratedParameterToService() throws Exception {

        when(monthlyReportContextService.buildMonthlyContext("LAST_30_DAYS", true))
                .thenReturn(response(true));

        mockMvc.perform(get("/api/reports/monthly-context")
                        .param("period", "LAST_30_DAYS")
                        .param("includeGenerated", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlySummary.includeGenerated").value(true));
    }

    private MonthlyReportContextDto response(boolean includeGenerated) {

        LocalDate today =
                LocalDate.now();

        return new MonthlyReportContextDto(
                "LAST_30_DAYS",
                new MonthlyReportSummaryDto(
                        today.minusDays(29),
                        today,
                        30,
                        1,
                        4,
                        includeGenerated ? 1 : 0,
                        includeGenerated ? 0 : 1,
                        includeGenerated,
                        42.0,
                        "YELLOW",
                        "YELLOW",
                        "Resumen mensual de prueba."),
                List.of(new MonthlyKpiAverageDto(
                        "global_status",
                        "Estado global",
                        42.0,
                        60.0,
                        4,
                        "%")),
                List.of(new MonthlyPlatformAffectationDto(
                        "Aruba",
                        30.0,
                        40,
                        1,
                        0,
                        0,
                        1)),
                List.of(new MonthlyDailySummaryDto(
                        today,
                        4,
                        true,
                        includeGenerated,
                        40,
                        "YELLOW",
                        30,
                        60,
                        20,
                        10,
                        35,
                        45,
                        30,
                        25,
                        "YELLOW",
                        List.of("Duracion media de logon Citrix elevada."))),
                List.of(new MonthlyWorstDayDto(
                        today,
                        60,
                        "Peor estado diario YELLOW; estado global=40% (YELLOW).",
                        includeGenerated)),
                List.of("Duracion media de logon Citrix elevada. (1 dias)"),
                List.of("El contexto mensual no genera texto mediante IA."));
    }
}
