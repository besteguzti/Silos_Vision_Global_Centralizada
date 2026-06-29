package com.tfg.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.PlatformWeightsConfigurationDto;
import com.tfg.dashboard.dto.ThresholdConfigurationDto;
import com.tfg.dashboard.dto.ThresholdSectionDto;
import com.tfg.dashboard.dto.ThresholdValueDto;
import com.tfg.dashboard.service.KpiConfigurationService;

@WebMvcTest(KpiConfigurationController.class)
class KpiConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KpiConfigurationService kpiConfigurationService;

    @Test
    void thresholdsEndpointReturnsEditableConfiguration() throws Exception {
        when(kpiConfigurationService.getThresholds())
                .thenReturn(thresholds());

        mockMvc.perform(get("/api/config/thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].key").value("transversal"))
                .andExpect(jsonPath("$.sections[0].values[0].key").value("transversal.globalStatus.yellowMin"))
                .andExpect(jsonPath("$.sections[0].values[0].value").value(34));
    }

    @Test
    void platformWeightsEndpointReturnsWeights() throws Exception {
        when(kpiConfigurationService.getPlatformWeights())
                .thenReturn(new PlatformWeightsConfigurationDto(40, 30, 20, 10));

        mockMvc.perform(get("/api/config/platform-weights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aruba").value(40))
                .andExpect(jsonPath("$.total").value(100));
    }

    @Test
    void updateAndResetEndpointsAreExposed() throws Exception {
        when(kpiConfigurationService.updateThresholds(any()))
                .thenReturn(thresholds());
        when(kpiConfigurationService.updatePlatformWeights(any()))
                .thenReturn(new PlatformWeightsConfigurationDto(40, 30, 20, 10));
        when(kpiConfigurationService.resetConfiguration())
                .thenReturn(thresholds());

        mockMvc.perform(put("/api/config/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sections\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/config/platform-weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aruba\":40,\"citrix\":30,\"microsoft365\":20,\"glpi\":10}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/config/thresholds/reset"))
                .andExpect(status().isOk());
    }

    private ThresholdConfigurationDto thresholds() {
        return new ThresholdConfigurationDto(List.of(
                new ThresholdSectionDto(
                        "transversal",
                        "Estado global y KPIs transversales",
                        "Rangos independientes",
                        List.of(new ThresholdValueDto(
                                "transversal.globalStatus.yellowMin",
                                "Estado global - inicio amarillo",
                                34,
                                34,
                                "%",
                                "Valor minimo amarillo")))));
    }
}
