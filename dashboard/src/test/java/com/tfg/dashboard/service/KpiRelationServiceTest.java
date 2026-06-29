package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiRelationDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

class KpiRelationServiceTest {

    private KpiRelationService service;

    @BeforeEach
    void setUp() {
        KpiProperties kpiProperties =
                new KpiProperties();

        service =
                new KpiRelationService(
                        kpiProperties,
                        new KpiScoringService(kpiProperties));
    }

    @Test
    void specificRelationsUseCategorizedGlpiTickets() {
        List<KpiRelationDto> relations =
                service.buildRelations(snapshotsForDays(7));

        assertThat(relations)
                .extracting(KpiRelationDto::getCode)
                .containsExactly(
                        "aruba_affectation_vs_wifi_clients",
                        "aruba_affectation_vs_aruba_tickets",
                        "citrix_affectation_vs_citrix_tickets",
                        "microsoft365_affectation_vs_microsoft365_tickets",
                        "aruba_wifi_clients_vs_citrix_sessions",
                        "aruba_wifi_clients_vs_microsoft365_active_users",
                        "citrix_delivery_controllers_vs_failed_logons",
                        "citrix_delivery_controllers_vs_sessions",
                        "glpi_pressure_vs_operational_backlog",
                        "glpi_pressure_vs_open_tickets",
                        "aruba_down_switches_vs_down_aps",
                        "glpi_created_vs_closed_tickets",
                        "microsoft365_active_users_vs_citrix_sessions");

        KpiRelationDto citrixRelation =
                relation(relations, "citrix_affectation_vs_citrix_tickets");
        KpiRelationDto microsoftRelation =
                relation(relations, "microsoft365_affectation_vs_microsoft365_tickets");
        KpiRelationDto arubaRelation =
                relation(relations, "aruba_affectation_vs_aruba_tickets");
        KpiRelationDto glpiPressureBacklogRelation =
                relation(relations, "glpi_pressure_vs_operational_backlog");
        KpiRelationDto glpiCreatedClosedRelation =
                relation(relations, "glpi_created_vs_closed_tickets");

        assertThat(citrixRelation.getYLabel())
                .isEqualTo("Tickets abiertos Citrix");
        assertThat(citrixRelation.getPoints().get(0).getY())
                .isEqualTo(50.0);
        assertThat(microsoftRelation.getYLabel())
                .isEqualTo("Tickets abiertos Microsoft 365");
        assertThat(microsoftRelation.getPoints().get(0).getY())
                .isEqualTo(20.0);
        assertThat(arubaRelation.getYLabel())
                .isEqualTo("Tickets abiertos Aruba");
        assertThat(arubaRelation.getPoints().get(0).getY())
                .isEqualTo(40.0);
        assertThat(glpiPressureBacklogRelation.getXLabel())
                .isEqualTo("Presión operativa GLPI");
        assertThat(glpiPressureBacklogRelation.getYLabel())
                .isEqualTo("Backlog operativo");
        assertThat(glpiPressureBacklogRelation.getPoints().get(0).getY())
                .isEqualTo(110.0);
        assertThat(glpiCreatedClosedRelation.getXLabel())
                .isEqualTo("Tickets creados GLPI");
        assertThat(glpiCreatedClosedRelation.getYLabel())
                .isEqualTo("Tickets cerrados GLPI");
        assertThat(glpiCreatedClosedRelation.getPoints().get(0).getX())
                .isEqualTo(35.0);
        assertThat(glpiCreatedClosedRelation.getPoints().get(0).getY())
                .isEqualTo(25.0);
    }

    @Test
    void aggregatesSpecificRelationPointsByDayUsingFourSixHourBuckets() {
        KpiRelationDto relation =
                relation(
                        service.buildRelations(snapshotsForDays(7)),
                        "aruba_affectation_vs_wifi_clients");

        assertThat(relation.getPoints())
                .hasSize(7)
                .allSatisfy(point -> assertThat(point.getSamplesUsed()).isEqualTo(4));
        assertThat(relation.getXLabel()).contains("Aruba");
        assertThat(relation.getXUnit()).isEqualTo("%");
        assertThat(relation.getYLabel()).contains("WiFi");
        assertThat(relation.getYUnit()).isEmpty();
    }

    @Test
    void lowVariationDoesNotReturnHighRelationReading() {
        KpiRelationDto relation =
                relation(
                        service.buildRelations(flatSnapshots()),
                        "aruba_affectation_vs_wifi_clients");

        assertThat(relation.getReading())
                .contains("No hay variaci");
        assertThat(relation.getReading())
                .doesNotContain("alta");
    }

    private KpiRelationDto relation(
            List<KpiRelationDto> relations,
            String code) {

        return relations.stream()
                .filter(candidate -> code.equals(candidate.getCode()))
                .findFirst()
                .orElseThrow();
    }

    private List<AnalysisSnapshot> snapshotsForDays(int days) {
        List<AnalysisSnapshot> snapshots =
                new ArrayList<>();

        for (int day = 0; day < days; day++) {
            for (int bucket = 0; bucket < 4; bucket++) {
                AnalysisSnapshot snapshot =
                        baseSnapshot(day, bucket);

                snapshot.setCitrixFailedLogons(12 + day);
                snapshot.setCitrixOpenTickets(50 + day);
                snapshot.setMicrosoft365NonCompliantDevices(60 + day);
                snapshot.setMicrosoft365OpenTickets(20 + day);
                snapshot.setArubaOpenTickets(40 + day);
                snapshot.setMicrosoft365UsersWithoutMfa(3 + day);
                snapshot.setMicrosoft365FailedSignIns(10 + day);
                snapshot.setAffectedServicesPercent(25 + day * 10);
                snapshot.setTechnicalDegradation(35 + day * 7);
                snapshot.setGlpiOperationalPressure(30 + day * 8);
                snapshot.setGlpiCreatedToday(35 + day);
                snapshot.setGlpiClosedToday(25 + day);
                snapshot.setGlpiCreatedThisWeek(220 + day);
                snapshot.setGlpiClosedThisWeek(160 + day);
                snapshot.setGlpiOperationalBacklog(110 + day);
                snapshots.add(snapshot);
            }
        }

        return snapshots;
    }

    private List<AnalysisSnapshot> flatSnapshots() {
        List<AnalysisSnapshot> snapshots =
                new ArrayList<>();

        for (int day = 0; day < 4; day++) {
            AnalysisSnapshot snapshot =
                    baseSnapshot(day, 0);

            snapshot.setTechnicalDegradation(50);
            snapshot.setGlpiOperationalPressure(53);
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private AnalysisSnapshot baseSnapshot(int day, int bucket) {
        AnalysisSnapshot snapshot =
                new AnalysisSnapshot();

        snapshot.setTimestamp(
                LocalDate.of(2026, 5, 1)
                        .plusDays(day)
                        .atStartOfDay()
                        .plusHours(bucket * 6L));
        snapshot.setArubaHealth(30 + day);
        snapshot.setCitrixHealth(35 + day);
        snapshot.setMicrosoft365Health(25 + day);
        snapshot.setArubaInactiveAps(2 + day);
        snapshot.setArubaDownSwitches(1 + day);
        snapshot.setArubaOpenTickets(40 + day);
        snapshot.setCitrixServerLoadPercent(75 + day);
        snapshot.setCitrixAverageLogonDurationSeconds(20 + day);
        snapshot.setArubaWifiClients(200 - day);
        snapshot.setArubaDownAps(day);
        snapshot.setCitrixActiveSessions(400 - day);
        snapshot.setCitrixAvailableDeliveryControllers(4);
        snapshot.setMicrosoft365ActiveUsers(1200 - day);
        snapshot.setGeneratedScenario(false);

        return snapshot;
    }
}

