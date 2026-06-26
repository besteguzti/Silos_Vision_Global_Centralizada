package com.tfg.dashboard.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.TestScenarioEvaluationResponse;
import com.tfg.dashboard.dto.TestScenarioRequest;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;

@Service
public class TestScenarioEvaluationService {

    private final MainDashboardService mainDashboardService;
    private final GlobalKpiCalculationService globalKpiCalculationService;
    private final KpiScoringService kpiScoringService;
    private final ExecutiveSummaryService executiveSummaryService;

    public TestScenarioEvaluationService(
            MainDashboardService mainDashboardService,
            GlobalKpiCalculationService globalKpiCalculationService,
            KpiScoringService kpiScoringService,
            ExecutiveSummaryService executiveSummaryService
    ) {
        this.mainDashboardService = mainDashboardService;
        this.globalKpiCalculationService = globalKpiCalculationService;
        this.kpiScoringService = kpiScoringService;
        this.executiveSummaryService = executiveSummaryService;
    }

    public TestScenarioEvaluationResponse evaluate(
            TestScenarioRequest request
    ) {
        validateRequest(request);

        ArubaSummary aruba = buildArubaSummary(request.getAruba());
        CitrixMetricsHistory citrix = buildCitrixMetrics(request.getCitrix());
        Microsoft365MetricsHistory microsoft365 = buildMicrosoft365Metrics(request.getMicrosoft365());
        GlpiMetricsHistory glpi = buildGlpiMetrics(request);

        MainDashboardSummary summary = mainDashboardService.evaluateSummary(
                aruba,
                citrix,
                microsoft365,
                glpi
        );

        String arubaStatus =
                kpiScoringService.statusFromAffection(
                        globalKpiCalculationService.calculateArubaNetworkAffection(aruba)
                );
        String citrixStatus =
                globalKpiCalculationService.calculateCitrixHealthStatus(citrix);
        String microsoftStatus =
                kpiScoringService.statusFromAffection(
                        globalKpiCalculationService.calculateMicrosoft365HealthAffection(
                                microsoft365,
                                glpi.getMicrosoft365OpenTickets()
                        )
                );
        String glpiStatus =
                kpiScoringService.statusFromAffection(
                        globalKpiCalculationService.calculateGlpiHealthAffection(glpi)
                );

        TestScenarioEvaluationResponse.PlatformStatus platformStatus =
                new TestScenarioEvaluationResponse.PlatformStatus();
        platformStatus.setAruba(arubaStatus);
        platformStatus.setCitrix(citrixStatus);
        platformStatus.setMicrosoft365(microsoftStatus);
        platformStatus.setGlpi(glpiStatus);

        TestScenarioEvaluationResponse response =
                new TestScenarioEvaluationResponse();
        response.setSummary(summary);
        response.setPlatformStatus(platformStatus);
        response.setOperationalSummary(
                executiveSummaryService.buildScenarioSummary(
                        summary,
                        aruba,
                        citrix,
                        microsoft365,
                        glpi)
        );

        return response;
    }

    private void validateRequest(TestScenarioRequest request) {
        if (request == null
                || request.getAruba() == null
                || request.getCitrix() == null
                || request.getMicrosoft365() == null
                || request.getGlpi() == null) {
            throw new IllegalArgumentException(
                    "Todos los datos del escenario son obligatorios."
            );
        }

        TestScenarioRequest.ArubaData aruba = request.getAruba();
        TestScenarioRequest.CitrixData citrix = request.getCitrix();
        TestScenarioRequest.Microsoft365Data microsoft365 = request.getMicrosoft365();
        TestScenarioRequest.GlpiData glpi = request.getGlpi();

        int totalAps = requireNonNegative(aruba.getTotalAps(), "total APs");
        int downAps = requireNonNegative(aruba.getDownAps(), "APs caidos");
        int inactiveAps = requireNonNegative(aruba.getInactiveAps(), "APs inactivos");
        int firmwareOutdated = requireNonNegative(aruba.getFirmwareOutdated(), "firmware pendiente APs");
        int totalSwitches = requireNonNegative(aruba.getTotalSwitches(), "total switches");
        int downSwitches = requireNonNegative(aruba.getDownSwitches(), "switches caidos");
        int switchesFirmwareUpgradeRequired =
                requireNonNegative(aruba.getSwitchesFirmwareUpgradeRequired(), "switches con upgrade pendiente");
        int totalWifiClients = requireNonNegative(aruba.getTotalWifiClients(), "clientes WiFi");
        int mutualiaWifiClients = requireNonNegative(aruba.getMutualiaWifiClients(), "clientes Mutualia-WIFI");
        int mutualiaApsClients = requireNonNegative(aruba.getMutualiaApsClients(), "clientes Mutualia-APS");
        int arubaOpenTickets = requireNonNegative(aruba.getArubaOpenTickets(), "tickets abiertos Aruba");

        int activeSessions = requireNonNegative(citrix.getActiveSessions(), "sesiones activas Citrix");
        int disconnectedSessions = optionalNonNegative(citrix.getDisconnectedSessions(), "sesiones desconectadas Citrix");
        int totalDeliveryControllers =
                requireNonNegative(citrix.getTotalDeliveryControllers(), "Delivery Controllers totales");
        int availableDeliveryControllers =
                requireNonNegative(citrix.getAvailableDeliveryControllers(), "Delivery Controllers disponibles");
        requireNonNegative(citrix.getAverageLogonDurationSeconds(), "duracion media de logon");
        requirePercent(citrix.getServerLoadPercent(), "carga de servidores Citrix");
        requireNonNegative(citrix.getFailedLogons(), "errores de inicio Citrix");
        int citrixOpenTickets = requireNonNegative(citrix.getCitrixOpenTickets(), "tickets abiertos Citrix");

        requirePercent(microsoft365.getSharePointStoragePercent(), "almacenamiento SharePoint");
        int activeUsers = optionalNonNegative(microsoft365.getActiveUsers(), "usuarios activos Microsoft 365");
        int usersWithoutMfa = requireNonNegative(microsoft365.getUsersWithoutMfa(), "usuarios sin MFA");
        requireNonNegative(microsoft365.getAppsSecretsExpiringSoon(), "secretos proximos a caducar");
        int nonCompliantDevices = requireNonNegative(microsoft365.getNonCompliantDevices(), "equipos no conformes");
        requireNonNegative(microsoft365.getOutdatedWindowsDevices(), "Windows desactualizados");
        int devicesWithoutEncryption = requireNonNegative(microsoft365.getDevicesWithoutEncryption(), "equipos sin cifrado");
        int microsoft365OpenTickets = requireNonNegative(microsoft365.getMicrosoft365OpenTickets(), "tickets abiertos Microsoft 365");

        int criticalOpenTickets = requireNonNegative(glpi.getCriticalOpenTickets(), "tickets críticos GLPI");
        requirePercent(glpi.getDailyClosurePercent(), "porcentaje cierre diario");
        requirePercent(glpi.getWeeklyClosurePercent(), "porcentaje cierre semanal");
        int totalOpenTickets = arubaOpenTickets + citrixOpenTickets + microsoft365OpenTickets;

        if (downAps > totalAps) {
            throw new IllegalArgumentException(
                    "APs caidos no puede ser mayor que total APs."
            );
        }

        if (inactiveAps > totalAps) {
            throw new IllegalArgumentException(
                    "APs inactivos no puede ser mayor que total APs."
            );
        }

        if (firmwareOutdated > totalAps) {
            throw new IllegalArgumentException(
                    "Firmware pendiente APs no puede ser mayor que total APs."
            );
        }

        if (downSwitches > totalSwitches) {
            throw new IllegalArgumentException(
                    "Switches caidos no puede ser mayor que total switches."
            );
        }

        if (switchesFirmwareUpgradeRequired > totalSwitches) {
            throw new IllegalArgumentException(
                    "Switches con upgrade pendiente no puede ser mayor que total switches."
            );
        }

        if (mutualiaWifiClients > totalWifiClients || mutualiaApsClients > totalWifiClients) {
            throw new IllegalArgumentException(
                    "Clientes Mutualia no puede ser mayor que clientes WiFi totales."
            );
        }

        if (totalWifiClients == 0 && (mutualiaWifiClients > 0 || mutualiaApsClients > 0)) {
            throw new IllegalArgumentException(
                    "Si clientes WiFi es 0, clientes Mutualia-WIFI y Mutualia-APS deben ser 0."
            );
        }

        if (activeSessions + disconnectedSessions > totalWifiClients) {
            throw new IllegalArgumentException(
                    "Sesiones Citrix activas y desconectadas no pueden superar clientes WiFi."
            );
        }

        if (availableDeliveryControllers > totalDeliveryControllers) {
            throw new IllegalArgumentException(
                    "Delivery Controllers disponibles no puede ser mayor que total Delivery Controllers."
            );
        }

        if (microsoft365.getActiveUsers() != null && usersWithoutMfa > activeUsers) {
            throw new IllegalArgumentException(
                    "Usuarios sin MFA no puede ser mayor que usuarios activos Microsoft 365."
            );
        }

        if (devicesWithoutEncryption > nonCompliantDevices) {
            throw new IllegalArgumentException(
                    "Equipos sin cifrado no puede ser mayor que equipos no conformes."
            );
        }

        if (criticalOpenTickets > totalOpenTickets) {
            throw new IllegalArgumentException(
                    "Tickets críticos GLPI no puede ser mayor que tickets abiertos totales."
            );
        }
    }

    private ArubaSummary buildArubaSummary(TestScenarioRequest.ArubaData arubaData) {
        ArubaSummary aruba = new ArubaSummary();
        aruba.setTotalAps(arubaData.getTotalAps());
        aruba.setDownAps(arubaData.getDownAps());
        aruba.setUpAps(Math.max(0, arubaData.getTotalAps() - arubaData.getDownAps()));
        aruba.setInactiveAps(arubaData.getInactiveAps());
        aruba.setFirmwareOutdated(arubaData.getFirmwareOutdated());
        aruba.setTotalSwitches(arubaData.getTotalSwitches());
        aruba.setDownSwitches(arubaData.getDownSwitches());
        aruba.setSwitchesFirmwareUpgradeRequired(arubaData.getSwitchesFirmwareUpgradeRequired());
        aruba.setTotalWifiClients(arubaData.getTotalWifiClients());
        aruba.setMutualiaWifiClients(arubaData.getMutualiaWifiClients());
        aruba.setMutualiaApsClients(arubaData.getMutualiaApsClients());
        aruba.setArubaOpenTickets(arubaData.getArubaOpenTickets());
        aruba.setLastUpdated(LocalDateTime.now());
        aruba.setDataStatus("OK");
        return aruba;
    }

    private CitrixMetricsHistory buildCitrixMetrics(TestScenarioRequest.CitrixData citrixData) {
        CitrixMetricsHistory citrix = new CitrixMetricsHistory();
        citrix.setActiveSessions(citrixData.getActiveSessions());
        // Valor por defecto razonable para licencias en escenarios de prueba
        // Si el request incluye licencias, usar ese valor; si no, usar 500.
        citrix.setActiveLicenses(citrixData.getActiveLicenses() == null ? 500 : citrixData.getActiveLicenses());
        citrix.setDisconnectedSessions(
                citrixData.getDisconnectedSessions() == null ? 0 : citrixData.getDisconnectedSessions());
        citrix.setTotalDeliveryControllers(citrixData.getTotalDeliveryControllers());
        citrix.setAvailableDeliveryControllers(citrixData.getAvailableDeliveryControllers());
        citrix.setAverageLogonDurationSeconds(citrixData.getAverageLogonDurationSeconds());
        citrix.setServerLoadPercent(citrixData.getServerLoadPercent());
        citrix.setFailedLogons(citrixData.getFailedLogons());
        citrix.setCitrixHealth(null);
        citrix.setCollectedAt(LocalDateTime.now());
        return citrix;
    }

    private Microsoft365MetricsHistory buildMicrosoft365Metrics(TestScenarioRequest.Microsoft365Data microsoftData) {
        Microsoft365MetricsHistory microsoft365 = new Microsoft365MetricsHistory();
        microsoft365.setActiveUsers(microsoftData.getActiveUsers());
        microsoft365.setUnassignedLicenses(50);
        microsoft365.setOutlookStatus("HEALTHY");
        microsoft365.setTeamsStatus("HEALTHY");
        microsoft365.setSharePointStatus("HEALTHY");
        microsoft365.setNearlyFullMailboxes(0);
        microsoft365.setEmailsQuarantined(0);
        microsoft365.setSharePointStoragePercent(microsoftData.getSharePointStoragePercent());
        microsoft365.setRiskyUsers(0);
        microsoft365.setFailedSignIns(0);
        microsoft365.setUsersWithoutMfa(microsoftData.getUsersWithoutMfa());
        microsoft365.setAppsSecretsExpiringSoon(microsoftData.getAppsSecretsExpiringSoon());
        microsoft365.setUnusedApplications(0);
        microsoft365.setHighPrivilegeApplications(0);
        microsoft365.setNonCompliantDevices(microsoftData.getNonCompliantDevices());
        microsoft365.setOutdatedWindowsDevices(microsoftData.getOutdatedWindowsDevices());
        microsoft365.setDevicesWithoutEncryption(microsoftData.getDevicesWithoutEncryption());
        microsoft365.setStaleDevices(0);
        microsoft365.setMicrosoft365Health(null);
        microsoft365.setCollectedAt(LocalDateTime.now());
        return microsoft365;
    }

    private GlpiMetricsHistory buildGlpiMetrics(TestScenarioRequest request) {
        TestScenarioRequest.GlpiData glpiData = request.getGlpi();
        int arubaOpenTickets = request.getAruba().getArubaOpenTickets();
        int citrixOpenTickets = request.getCitrix().getCitrixOpenTickets();
        int microsoft365OpenTickets = request.getMicrosoft365().getMicrosoft365OpenTickets();
        int totalOpenTickets = arubaOpenTickets + citrixOpenTickets + microsoft365OpenTickets;
        GlpiMetricsHistory glpi = new GlpiMetricsHistory();
        glpi.setOpenTickets(totalOpenTickets);
        glpi.setArubaOpenTickets(arubaOpenTickets);
        glpi.setCitrixOpenTickets(citrixOpenTickets);
        glpi.setMicrosoft365OpenTickets(microsoft365OpenTickets);
        glpi.setCriticalOpenTickets(glpiData.getCriticalOpenTickets());
        glpi.setCreatedToday(100);
        glpi.setClosedToday(glpiData.getDailyClosurePercent());
        glpi.setCreatedThisWeek(100);
        glpi.setClosedThisWeek(glpiData.getWeeklyClosurePercent());
        glpi.setCollectedAt(LocalDateTime.now());
        return glpi;
    }

    private int requireNonNegative(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("El campo " + fieldName + " es obligatorio.");
        }

        if (value < 0) {
            throw new IllegalArgumentException("El campo " + fieldName + " no puede ser negativo.");
        }

        return value;
    }

    private int requirePercent(Integer value, String fieldName) {
        int safeValue = requireNonNegative(value, fieldName);

        if (safeValue > 100) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe estar entre 0 y 100.");
        }

        return safeValue;
    }

    private int optionalNonNegative(Integer value, String fieldName) {
        if (value == null) {
            return 0;
        }

        if (value < 0) {
            throw new IllegalArgumentException("El campo " + fieldName + " no puede ser negativo.");
        }

        return value;
    }

}

