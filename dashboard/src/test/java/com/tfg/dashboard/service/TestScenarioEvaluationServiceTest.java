package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.controller.TestScenarioController;
import com.tfg.dashboard.dto.TestScenarioEvaluationResponse;
import com.tfg.dashboard.dto.TestScenarioRequest;
import com.tfg.dashboard.dto.PlatformFindingDto;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;

class TestScenarioEvaluationServiceTest {

    @Test
    void arubaCriticalScenarioReturnsRedAndRaisesImpactKpis() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setTotalAps(10);
        request.getAruba().setDownAps(10);
        request.getAruba().setTotalSwitches(4);
        request.getAruba().setDownSwitches(4);
        request.getAruba().setTotalWifiClients(0);
        request.getAruba().setMutualiaApsClients(0);
        request.getAruba().setMutualiaWifiClients(0);
        request.getCitrix().setActiveSessions(0);
        request.getCitrix().setDisconnectedSessions(0);
        request.getMicrosoft365().setActiveUsers(0);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(response.getPlatformStatus().getAruba()).isEqualTo(KpiScoringService.RED);
        assertThat(response.getSummary().getGlobalAvailability()).isGreaterThanOrEqualTo(34);
        assertThat(response.getSummary().getUserImpact()).isGreaterThanOrEqualTo(34);
    }

    @Test
    void criticalScenarioIncludesHighOperationalSummaryWithoutHistoricalTrend() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setTotalAps(10);
        request.getAruba().setDownAps(10);
        request.getAruba().setTotalSwitches(4);
        request.getAruba().setDownSwitches(4);
        request.getAruba().setTotalWifiClients(0);
        request.getAruba().setMutualiaApsClients(0);
        request.getAruba().setMutualiaWifiClients(0);
        request.getCitrix().setActiveSessions(0);
        request.getCitrix().setDisconnectedSessions(0);
        request.getCitrix().setAvailableDeliveryControllers(0);
        request.getCitrix().setAverageLogonDurationSeconds(80);
        request.getCitrix().setServerLoadPercent(90);
        request.getCitrix().setFailedLogons(40);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(response.getOperationalSummary().getPriority()).isEqualTo("HIGH");
        assertThat(response.getOperationalSummary().getAffectedServices())
                .contains("Red corporativa / conectividad")
                .contains("Acceso a aplicaciones corporativas");
        assertThat(response.getOperationalSummary().getFirstAction())
                .contains("APs");
        assertThat(response.getOperationalSummary().getTrend())
                .isEqualTo("Tendencia no disponible en escenario manual");
        assertThat(response.getOperationalSummary().getSummaryText())
                .contains("no demuestra causalidad")
                .contains("ni modifica los datos reales");
    }

    @Test
    void normalScenarioIncludesLowOperationalSummaryWithoutAffectedServices() {
        TestScenarioEvaluationResponse response = realService().evaluate(validRequest());

        assertThat(response.getOperationalSummary().getPriority()).isEqualTo("LOW");
        assertThat(response.getOperationalSummary().getAffectedServices()).isEmpty();
        assertThat(response.getOperationalSummary().getPlatformFindings()).isEmpty();
        assertThat(response.getOperationalSummary().getMainAffectedPlatform())
                .isEqualTo("Sin plataforma afectada");
        assertThat(response.getOperationalSummary().getTrend())
                .isEqualTo("Tendencia no disponible en escenario manual");
    }

    @Test
    void citrixHighLogonIsReportedEvenWhenWeightedGlobalStatusIsGreen() {
        TestScenarioRequest request = validRequest();

        request.getCitrix().setAverageLogonDurationSeconds(55);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(response.getOperationalSummary().getGlobalStatus()).isEqualTo("GREEN");
        assertThat(findingsFor(response, "Citrix"))
                .anySatisfy(finding -> assertThat(finding).contains("logon").contains("55"));
    }

    @Test
    void arubaDownAccessPointsAreReportedAsPlatformFinding() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setDownAps(5);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(findingsFor(response, "Aruba"))
                .anySatisfy(finding -> assertThat(finding).contains("APs caidos"));
    }

    @Test
    void microsoft365UsersWithoutMfaAreReportedAsPlatformFinding() {
        TestScenarioRequest request = validRequest();

        request.getMicrosoft365().setUsersWithoutMfa(2);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(findingsFor(response, "Microsoft 365"))
                .anySatisfy(finding -> assertThat(finding).contains("usuarios sin MFA"));
    }

    @Test
    void glpiCriticalTicketsAreReportedAsPlatformFinding() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setArubaOpenTickets(5);
        request.getGlpi().setCriticalOpenTickets(2);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(findingsFor(response, "GLPI"))
                .anySatisfy(finding -> assertThat(finding).contains("tickets criticos"));
    }

    @Test
    void invalidArubaCountsFailWithControlledValidationError() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setTotalAps(5);
        request.getAruba().setDownAps(6);

        assertThatThrownBy(() -> realService().evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APs caidos no puede ser mayor que total APs");
    }

    @Test
    void citrixSessionsAboveWifiClientsFailWithControlledValidationError() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setTotalWifiClients(50);
        request.getCitrix().setActiveSessions(45);
        request.getCitrix().setDisconnectedSessions(10);

        assertThatThrownBy(() -> realService().evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sesiones Citrix activas y desconectadas no pueden superar clientes WiFi");
    }

    @Test
    void microsoft365ActiveUsersAboveCitrixSessionsAreAllowed() {
        TestScenarioRequest request = validRequest();

        request.getCitrix().setActiveSessions(0);
        request.getCitrix().setDisconnectedSessions(0);
        request.getMicrosoft365().setActiveUsers(101);
        request.getMicrosoft365().setUsersWithoutMfa(3);

        TestScenarioEvaluationResponse response = realService().evaluate(request);

        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getPlatformStatus().getMicrosoft365()).isNotBlank();
    }

    @Test
    void usersWithoutMfaAboveActiveUsersFailWithControlledValidationError() {
        TestScenarioRequest request = validRequest();

        request.getMicrosoft365().setActiveUsers(3);
        request.getMicrosoft365().setUsersWithoutMfa(4);

        assertThatThrownBy(() -> realService().evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuarios sin MFA no puede ser mayor que usuarios activos Microsoft 365");
    }

    @Test
    void criticalTicketsAboveOpenTicketsFailWithControlledValidationError() {
        TestScenarioRequest request = validRequest();

        request.getAruba().setArubaOpenTickets(1);
        request.getCitrix().setCitrixOpenTickets(1);
        request.getMicrosoft365().setMicrosoft365OpenTickets(1);
        request.getGlpi().setCriticalOpenTickets(4);

        assertThatThrownBy(() -> realService().evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tickets críticos GLPI no puede ser mayor que tickets abiertos totales");
    }

    @Test
    void categorizedTicketsAreUsedAsCoherentGlpiTotal() {
        MainDashboardService mainDashboardService = mock(MainDashboardService.class);
        KpiProperties kpiProperties = new KpiProperties();
        KpiScoringService kpiScoringService = new KpiScoringService(kpiProperties);
        GlobalKpiCalculationService globalKpiCalculationService =
                new GlobalKpiCalculationService(kpiScoringService, kpiProperties);
        TestScenarioEvaluationService service =
                new TestScenarioEvaluationService(
                        mainDashboardService,
                        globalKpiCalculationService,
                        kpiScoringService,
                        mock(ExecutiveSummaryService.class));
        TestScenarioRequest request = validRequest();

        request.getAruba().setArubaOpenTickets(35);
        request.getCitrix().setCitrixOpenTickets(100);
        request.getMicrosoft365().setMicrosoft365OpenTickets(25);

        when(mainDashboardService.evaluateSummary(
                any(ArubaSummary.class),
                any(CitrixMetricsHistory.class),
                any(Microsoft365MetricsHistory.class),
                any(GlpiMetricsHistory.class)))
                .thenReturn(new MainDashboardSummary());

        service.evaluate(request);

        ArgumentCaptor<GlpiMetricsHistory> glpiCaptor =
                ArgumentCaptor.forClass(GlpiMetricsHistory.class);
        verify(mainDashboardService).evaluateSummary(
                any(ArubaSummary.class),
                any(CitrixMetricsHistory.class),
                any(Microsoft365MetricsHistory.class),
                glpiCaptor.capture());

        GlpiMetricsHistory glpi = glpiCaptor.getValue();

        assertThat(glpi.getArubaOpenTickets()).isEqualTo(35);
        assertThat(glpi.getCitrixOpenTickets()).isEqualTo(100);
        assertThat(glpi.getMicrosoft365OpenTickets()).isEqualTo(25);
        assertThat(glpi.getOpenTickets()).isEqualTo(160);
    }

    @Test
    void partialJsonReturnsBadRequestInsteadOfServerError() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestScenarioController(realService()))
                .build();

        mockMvc.perform(post("/api/test-scenarios/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aruba": {},
                                  "citrix": {},
                                  "microsoft365": {},
                                  "glpi": {}
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private TestScenarioEvaluationService realService() {
        KpiProperties kpiProperties = new KpiProperties();
        KpiScoringService kpiScoringService = new KpiScoringService(kpiProperties);
        GlobalKpiCalculationService globalKpiCalculationService =
                new GlobalKpiCalculationService(kpiScoringService, kpiProperties);
        DashboardFreshnessService dashboardFreshnessService =
                new DashboardFreshnessService(kpiScoringService, kpiProperties);
        MainDashboardService mainDashboardService =
                new MainDashboardService(
                        null,
                        null,
                        null,
                        null,
                        kpiScoringService,
                        globalKpiCalculationService,
                        dashboardFreshnessService,
                        kpiProperties);

        return new TestScenarioEvaluationService(
                mainDashboardService,
                globalKpiCalculationService,
                kpiScoringService,
                new ExecutiveSummaryService(
                        mainDashboardService,
                        null,
                        null,
                        null,
                        null,
                        null,
                        kpiProperties));
    }

    private TestScenarioRequest validRequest() {
        TestScenarioRequest request = new TestScenarioRequest();
        TestScenarioRequest.ArubaData aruba = new TestScenarioRequest.ArubaData();
        TestScenarioRequest.CitrixData citrix = new TestScenarioRequest.CitrixData();
        TestScenarioRequest.Microsoft365Data microsoft365 =
                new TestScenarioRequest.Microsoft365Data();
        TestScenarioRequest.GlpiData glpi = new TestScenarioRequest.GlpiData();

        aruba.setTotalAps(10);
        aruba.setDownAps(0);
        aruba.setInactiveAps(0);
        aruba.setFirmwareOutdated(0);
        aruba.setTotalSwitches(4);
        aruba.setDownSwitches(0);
        aruba.setSwitchesFirmwareUpgradeRequired(0);
        aruba.setTotalWifiClients(240);
        aruba.setMutualiaWifiClients(40);
        aruba.setMutualiaApsClients(40);
        aruba.setArubaOpenTickets(0);

        citrix.setActiveSessions(200);
        citrix.setDisconnectedSessions(10);
        citrix.setTotalDeliveryControllers(4);
        citrix.setAvailableDeliveryControllers(4);
        citrix.setAverageLogonDurationSeconds(10);
        citrix.setServerLoadPercent(20);
        citrix.setFailedLogons(0);
        citrix.setCitrixOpenTickets(0);

        microsoft365.setSharePointStoragePercent(40);
        microsoft365.setUsersWithoutMfa(0);
        microsoft365.setAppsSecretsExpiringSoon(0);
        microsoft365.setNonCompliantDevices(10);
        microsoft365.setOutdatedWindowsDevices(0);
        microsoft365.setDevicesWithoutEncryption(0);
        microsoft365.setMicrosoft365OpenTickets(0);

        glpi.setCriticalOpenTickets(0);
        glpi.setDailyClosurePercent(100);
        glpi.setWeeklyClosurePercent(100);

        request.setAruba(aruba);
        request.setCitrix(citrix);
        request.setMicrosoft365(microsoft365);
        request.setGlpi(glpi);

        return request;
    }

    private List<String> findingsFor(TestScenarioEvaluationResponse response, String platform) {
        return response.getOperationalSummary()
                .getPlatformFindings()
                .stream()
                .filter(finding -> platform.equals(finding.getPlatform()))
                .findFirst()
                .map(PlatformFindingDto::getFindings)
                .orElse(List.of());
    }
}

