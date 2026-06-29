package com.tfg.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.ArubaApAnnotationDto;
import com.tfg.dashboard.dto.ArubaInactiveApDto;
import com.tfg.dashboard.service.ArubaService;

@WebMvcTest(ArubaController.class)
class ArubaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArubaService arubaService;

    @Test
    void inactiveApsEndpointReturnsDetailsWithAnnotation() throws Exception {
        when(arubaService.getInactiveAps())
                .thenReturn(List.of(new ArubaInactiveApDto(
                        "SER-1",
                        "AP Planta 1",
                        "Down",
                        "Site A",
                        "Swarm A",
                        LocalDateTime.of(2026, 5, 18, 10, 35),
                        16,
                        "Pendiente revisar alimentacion")));

        mockMvc.perform(get("/aruba/inactive-aps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serial").value("SER-1"))
                .andExpect(jsonPath("$[0].name").value("AP Planta 1"))
                .andExpect(jsonPath("$[0].daysInactive").value(16))
                .andExpect(jsonPath("$[0].annotation").value("Pendiente revisar alimentacion"));
    }

    @Test
    void annotationEndpointSavesManualNote() throws Exception {
        when(arubaService.saveInactiveApAnnotation(eq("SER-1"), any()))
                .thenReturn(new ArubaApAnnotationDto(
                        "SER-1",
                        "Nueva nota",
                        LocalDateTime.of(2026, 6, 3, 12, 0)));

        mockMvc.perform(put("/aruba/inactive-aps/SER-1/annotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annotation\":\"Nueva nota\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serial").value("SER-1"))
                .andExpect(jsonPath("$.annotation").value("Nueva nota"));
    }
}
