package com.tfg.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.ExecutiveSummaryDto;
import com.tfg.dashboard.service.ExecutiveSummaryService;

@WebMvcTest(ApiDashboardController.class)
class ApiDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutiveSummaryService executiveSummaryService;

    @Test
    void getExecutiveSummaryReturnsOperationalDiagnosis() throws Exception {

        ExecutiveSummaryDto summary =
                new ExecutiveSummaryDto();

        summary.setGlobalStatus("YELLOW");
        summary.setAffectedServices(List.of("Red corporativa / conectividad"));
        summary.setMainAffectedPlatform("Aruba");
        summary.setProbableOrigin("Aruba");
        summary.setImpactLevel("MODERATE");
        summary.setEstimatedAffectedUsers("42 clientes WiFi observados");
        summary.setPriority("MEDIUM");
        summary.setFirstAction("Revisar APs inactivos");
        summary.setTrend("STABLE");
        summary.setSummaryText("Diagnóstico operativo de prueba");

        when(executiveSummaryService.getExecutiveSummary())
                .thenReturn(summary);

        mockMvc.perform(get("/api/dashboard/executive-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalStatus").value("YELLOW"))
                .andExpect(jsonPath("$.mainAffectedPlatform").value("Aruba"))
                .andExpect(jsonPath("$.trend").value("STABLE"));
    }
}

