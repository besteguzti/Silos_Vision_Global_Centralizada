package com.tfg.dashboard.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.ManualSyncPlatformResultDto;
import com.tfg.dashboard.dto.ManualSyncResponseDto;
import com.tfg.dashboard.service.KpiConfigurationService;
import com.tfg.dashboard.service.ManualSyncService;

@WebMvcTest(ManualSyncController.class)
class ManualSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManualSyncService manualSyncService;

    @MockBean
    private KpiConfigurationService kpiConfigurationService;

    @Test
    void manualSyncEndpointReturnsSyncResult() throws Exception {
        when(manualSyncService.syncAllPlatformsManually())
                .thenReturn(new ManualSyncResponseDto(
                        "OK",
                        "Sincronizacion completada correctamente.",
                        LocalDateTime.parse("2026-06-04T16:40:00"),
                        LocalDateTime.parse("2026-06-04T16:40:12"),
                        List.of(new ManualSyncPlatformResultDto(
                                "Aruba",
                                "OK",
                                "Sincronizacion Aruba completada."
                        ))
                ));

        mockMvc.perform(post("/api/metrics/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.platforms[0].name").value("Aruba"));

        verifyNoInteractions(kpiConfigurationService);
    }
}
