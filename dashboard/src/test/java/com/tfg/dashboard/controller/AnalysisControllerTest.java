package com.tfg.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tfg.dashboard.dto.AnalyticsComparePoint;
import com.tfg.dashboard.dto.OperationalImpactAnalysisResponse;
import com.tfg.dashboard.dto.TechnicalPlatformRelationDto;
import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.service.TransversalKpiAnalyticsService;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransversalKpiAnalyticsService analyticsService;

    @Test
    void glpiPlatformRelationReturnsExpectedStructure() throws Exception {

        OperationalImpactAnalysisResponse response =
                new OperationalImpactAnalysisResponse();
        TechnicalPlatformRelationDto relation = new TechnicalPlatformRelationDto();
        relation.setRelation("Aruba-Citrix");
        relation.setOrigin("Aruba");
        relation.setTarget("Citrix");
        response.setTechnicalRelations(List.of(relation));
        response.setTechnicalTimeline(List.of(timelinePoint()));
        response.setSpecificKpiRelations(List.of());

        when(analyticsService.getGlpiPlatformRelation("30d"))
                .thenReturn(response);

        mockMvc.perform(get("/api/analysis/glpi-platform-relation")
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technicalRelations[0].origin").value("Aruba"))
                .andExpect(jsonPath("$.technicalTimeline[0].aruba").value(20.0))
                .andExpect(jsonPath("$.specificKpiRelations").isArray());
    }

    @Test
    void technicalDegradationImpactReturnsPoints() throws Exception {

        when(analyticsService.getTechnicalDegradationImpact("30d"))
                .thenReturn(List.of(point(30, 55)));

        mockMvc.perform(get("/api/analysis/technical-degradation-impact")
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].x").value(30.0))
                .andExpect(jsonPath("$[0].y").value(55.0));
    }

    @Test
    void snapshotsEndpointReturnsExtendedAnalysisFields() throws Exception {

        AnalysisSnapshot snapshot = new AnalysisSnapshot();
        snapshot.setTimestamp(LocalDateTime.of(2026, 5, 25, 12, 0));
        snapshot.setMicrosoft365ActiveUsers(180);
        snapshot.setCitrixAvailableDeliveryControllers(3);
        snapshot.setArubaDownAps(4);
        snapshot.setGlpiCreatedToday(22);
        snapshot.setGlpiClosedToday(18);
        snapshot.setGlpiCreatedThisWeek(120);
        snapshot.setGlpiClosedThisWeek(90);
        snapshot.setGlpiOperationalBacklog(30);

        when(analyticsService.getAnalysisSnapshots("30d"))
                .thenReturn(List.of(snapshot));

        mockMvc.perform(get("/api/analysis/snapshots")
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].microsoft365ActiveUsers").value(180))
                .andExpect(jsonPath("$[0].citrixAvailableDeliveryControllers").value(3))
                .andExpect(jsonPath("$[0].arubaDownAps").value(4))
                .andExpect(jsonPath("$[0].glpiCreatedToday").value(22))
                .andExpect(jsonPath("$[0].glpiClosedToday").value(18))
                .andExpect(jsonPath("$[0].glpiCreatedThisWeek").value(120))
                .andExpect(jsonPath("$[0].glpiClosedThisWeek").value(90))
                .andExpect(jsonPath("$[0].glpiOperationalBacklog").value(30));
    }

    @Test
    void platformEvolutionReturnsTimeline() throws Exception {

        TimelinePointDto point =
                new TimelinePointDto();
        point.setTimestamp(LocalDateTime.now());
        point.setAruba(20.0);
        point.setCitrix(30.0);
        point.setMicrosoft365(40.0);

        when(analyticsService.getPlatformEvolution("30d"))
                .thenReturn(List.of(point));

        mockMvc.perform(get("/api/analysis/platform-evolution")
                        .param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aruba").value(20.0))
                .andExpect(jsonPath("$[0].citrix").value(30.0))
                .andExpect(jsonPath("$[0].microsoft365").value(40.0));
    }

    @Test
    void emptyServiceResponsesDoNotReturnServerError() throws Exception {

        OperationalImpactAnalysisResponse emptyRelation =
                new OperationalImpactAnalysisResponse();
        emptyRelation.setTechnicalRelations(List.of());
        emptyRelation.setTechnicalTimeline(List.of());
        emptyRelation.setSpecificKpiRelations(List.of());

        when(analyticsService.getGlpiPlatformRelation("7d"))
                .thenReturn(emptyRelation);
        when(analyticsService.getTechnicalDegradationImpact("7d"))
                .thenReturn(List.of());
        when(analyticsService.getPlatformEvolution("7d"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analysis/glpi-platform-relation")
                        .param("period", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technicalRelations").isArray())
                .andExpect(jsonPath("$.technicalTimeline").isArray())
                .andExpect(jsonPath("$.specificKpiRelations").isArray());

        mockMvc.perform(get("/api/analysis/technical-degradation-impact")
                        .param("period", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/analysis/platform-evolution")
                        .param("period", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private AnalyticsComparePoint point(double x, double y) {

        return new AnalyticsComparePoint(
                LocalDateTime.now(),
                x,
                y
        );
    }

    private TimelinePointDto timelinePoint() {

        TimelinePointDto point =
                new TimelinePointDto();
        point.setTimestamp(LocalDateTime.now());
        point.setAruba(20.0);
        point.setCitrix(30.0);
        point.setMicrosoft365(40.0);
        return point;
    }
}
