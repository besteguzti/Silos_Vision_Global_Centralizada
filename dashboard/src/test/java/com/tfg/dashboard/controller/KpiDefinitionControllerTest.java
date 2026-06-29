package com.tfg.dashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.service.KpiDefinitionService;

@WebMvcTest(KpiDefinitionController.class)
@Import({KpiDefinitionService.class, KpiProperties.class})
class KpiDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDefinitionsReturnsDocumentedKpis() throws Exception {

        mockMvc.perform(get("/api/kpis/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id")
                        .value(hasItem("aruba_network_status")))
                .andExpect(jsonPath("$[*].id")
                        .value(hasItem("global_status")))
                .andExpect(jsonPath("$[?(@.id == 'global_status')].thresholds.green")
                        .value(hasItem("0-33")))
                .andExpect(jsonPath("$[?(@.id == 'global_status')].formula")
                        .value(hasItem(containsString("Índice de salud Aruba * 0.40"))));
    }
}
