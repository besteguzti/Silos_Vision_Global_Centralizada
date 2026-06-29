package com.tfg.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.SynchronizationControlDto;
import com.tfg.dashboard.service.SynchronizationControlService;

@WebMvcTest(SynchronizationControlController.class)
class SynchronizationControlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SynchronizationControlService synchronizationControlService;

    @Test
    void getSyncControlReturnsCurrentStatus() throws Exception {
        when(synchronizationControlService.getStatus())
                .thenReturn(syncStatus(false, "PAUSED"));

        mockMvc.perform(get("/api/metrics/sync-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automaticSyncEnabled").value(false))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void pauseSyncControlReturnsPausedStatus() throws Exception {
        when(synchronizationControlService.pauseAutomaticSync())
                .thenReturn(syncStatus(false, "PAUSED"));

        mockMvc.perform(post("/api/metrics/sync-control/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automaticSyncEnabled").value(false))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void resumeSyncControlReturnsActiveStatus() throws Exception {
        when(synchronizationControlService.resumeAutomaticSync())
                .thenReturn(syncStatus(true, "ACTIVE"));

        mockMvc.perform(post("/api/metrics/sync-control/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automaticSyncEnabled").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private SynchronizationControlDto syncStatus(boolean enabled, String status) {
        return new SynchronizationControlDto(
                enabled,
                status,
                "Estado de prueba",
                LocalDateTime.parse("2026-06-05T12:30:00")
        );
    }
}
