package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;

class GlobalKpiCalculationServiceTest {

    private KpiScoringService scoringService;
    private GlobalKpiCalculationService service;
    private KpiProperties kpiProperties;

    @BeforeEach
    void setUp() {

        kpiProperties =
                new KpiProperties();
        scoringService =
                new KpiScoringService(kpiProperties);
        service =
                new GlobalKpiCalculationService(scoringService, kpiProperties);
    }

    @Test
    void globalStatusUsesConfiguredPlatformWeights() {

        int globalStatus =
                service.weightedAverage(
                        100, 40,
                        50, 30,
                        0, 20,
                        0, 10
                );

        assertThat(globalStatus)
                .isEqualTo(55);
        assertThat(scoringService.statusFromAffection(globalStatus))
                .isEqualTo("YELLOW");
    }

    @Test
    void affectionBoundariesUseGreenYellowAndRedScale() {

        assertThat(scoringService.statusFromAffection(0))
                .isEqualTo("GREEN");
        assertThat(scoringService.statusFromAffection(33))
                .isEqualTo("GREEN");
        assertThat(scoringService.statusFromAffection(34))
                .isEqualTo("YELLOW");
        assertThat(scoringService.statusFromAffection(66))
                .isEqualTo("YELLOW");
        assertThat(scoringService.statusFromAffection(67))
                .isEqualTo("RED");
        assertThat(scoringService.statusFromAffection(100))
                .isEqualTo("RED");
    }

    @Test
    void platformHealthIndexesAreCalculatedFromRepresentativeSignals() {

        ArubaSummary aruba =
                aruba();
        CitrixMetricsHistory citrix =
                citrix();
        Microsoft365MetricsHistory microsoft365 =
                microsoft365();
        GlpiMetricsHistory glpi =
                glpi();

        assertThat(service.calculateArubaNetworkAffection(aruba))
                .isEqualTo(75);
        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(45);
        assertThat(service.calculateMicrosoft365HealthAffection(microsoft365))
                .isEqualTo(59);
        assertThat(service.calculateGlpiHealthAffection(glpi))
                .isEqualTo(67);
    }

    @Test
    void transversalKpisAreCalculatedWithCurrentRules() {

        ArubaSummary aruba =
                aruba();
        CitrixMetricsHistory citrix =
                citrix();
        Microsoft365MetricsHistory microsoft365 =
                microsoft365();
        GlpiMetricsHistory glpi =
                glpi();

        int globalStatus =
                service.weightedAverage(
                        service.calculateArubaNetworkAffection(aruba), 40,
                        service.calculateCitrixHealthAffection(citrix), 30,
                        service.calculateMicrosoft365HealthAffection(microsoft365), 20,
                        service.calculateGlpiHealthAffection(glpi), 10
                );

        assertThat(globalStatus)
                .isEqualTo(62);
        assertThat(service.calculateGlobalCriticality(aruba,citrix,microsoft365,glpi))
                .isEqualTo(40);
        assertThat(service.calculateGlobalAvailability(aruba,citrix,microsoft365,glpi))
                .isEqualTo(34);
        assertThat(service.calculateOperationalPressure(aruba,citrix,microsoft365,glpi))
                .isEqualTo(60);
        assertThat(service.calculateTechnicalDegradation(aruba,citrix,microsoft365,glpi))
                .isEqualTo(50);
        assertThat(service.calculateSlaRisk(aruba,citrix,microsoft365,glpi))
                .isEqualTo(51);
        assertThat(service.calculateOperationalBacklog(aruba,citrix,microsoft365,glpi))
                .isEqualTo(66);
        // With the current delivery controller yellow threshold of 67%, Citrix is
        // counted as yellow here, raising user impact from the old 33 to 38.
        assertThat(service.calculateUserImpact(aruba,citrix,microsoft365,glpi))
                .isEqualTo(38);
        assertThat(service.calculateAffectedServicesPercent(75,40,50,37))
                .isEqualTo(100);
        assertThat(service.calculateAffectedPlatformCount(75,40,50,37))
                .isEqualTo(4);
        assertThat(service.calculateItemsRequiringAction(aruba,citrix,microsoft365,glpi))
                .isEqualTo(68);
    }

    @Test
    void platformHealthStaysGreenWhenAllInternalIndicatorsAreGreen() {

        assertThat(service.calculateArubaNetworkAffection(healthyAruba()))
                .isEqualTo(0);
        assertThat(service.calculateCitrixHealthAffection(healthyCitrix()))
                .isEqualTo(0);
        assertThat(service.calculateMicrosoft365HealthAffection(healthyMicrosoft365()))
                .isEqualTo(0);
        assertThat(service.calculateGlpiHealthAffection(healthyGlpi()))
                .isEqualTo(0);
    }

    @Test
    void oneInternalYellowRaisesPlatformToYellowFloor() {

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setAverageLogonDurationSeconds(30);

        Microsoft365MetricsHistory microsoft365 =
                healthyMicrosoft365();
        microsoft365.setAppsSecretsExpiringSoon(1);

        GlpiMetricsHistory glpi =
                healthyGlpi();
        glpi.setOpenTickets(150);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(15);
        assertThat(service.calculateMicrosoft365HealthAffection(microsoft365))
                .isEqualTo(2);
        assertThat(service.calculateGlpiHealthAffection(glpi))
                .isEqualTo(34);
    }

    @Test
    void twoInternalYellowsRaisePlatformToRedFloor() {

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setAverageLogonDurationSeconds(30);
        citrix.setFailedLogons(15);

        Microsoft365MetricsHistory microsoft365 =
                healthyMicrosoft365();
        microsoft365.setAppsSecretsExpiringSoon(1);
        microsoft365.setOutdatedWindowsDevices(1);

        GlpiMetricsHistory glpi =
                healthyGlpi();
        glpi.setOpenTickets(150);
        glpi.setCriticalOpenTickets(5);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(30);
        assertThat(service.calculateMicrosoft365HealthAffection(microsoft365))
                .isEqualTo(4);
        assertThat(service.calculateGlpiHealthAffection(glpi))
                .isEqualTo(67);
    }

    @Test
    void oneInternalRedRaisesCitrixAndGlpiWhileMicrosoft365UsesPartialSum() {

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setAverageLogonDurationSeconds(90);

        Microsoft365MetricsHistory microsoft365 =
                healthyMicrosoft365();
        microsoft365.setUsersWithoutMfa(5);

        GlpiMetricsHistory glpi =
                healthyGlpi();
        glpi.setCriticalOpenTickets(11);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(70);
        assertThat(service.calculateMicrosoft365HealthAffection(microsoft365))
                .isEqualTo(10);
        assertThat(service.calculateGlpiHealthAffection(glpi))
                .isEqualTo(67);
    }

    @Test
    void slaBreachedTicketsRaiseGlpiHealthAndSlaRisk() {

        GlpiMetricsHistory glpi =
                healthyGlpi();
        glpi.setSlaBreachedTickets(22);

        assertThat(service.calculateGlpiHealthAffection(glpi))
                .isEqualTo(67);
        assertThat(service.calculateSlaRisk(
                        healthyAruba(),
                        healthyCitrix(),
                        healthyMicrosoft365(),
                        glpi))
                .isEqualTo(6);
    }

    @Test
    void citrixAffectionUsesExactPartialSumWithoutForcingOneHundred() {

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setAvailableDeliveryControllers(2);
        citrix.setTotalDeliveryControllers(4);
        citrix.setAverageLogonDurationSeconds(30);
        citrix.setServerLoadPercent(70);
        citrix.setFailedLogons(15);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(45);
        assertThat(service.calculateCitrixHealthStatus(citrix))
                .isEqualTo("YELLOW");
    }

    @Test
    void citrixAffectionReachesOneHundredOnlyForExtremeConditions() {

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setActiveSessions(0);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(100);
        assertThat(service.calculateCitrixHealthStatus(citrix))
                .isEqualTo("RED");
    }

    @Test
    void citrixDashboardCalculationUsesActiveThresholdProperties() {

        kpiProperties.getCitrix().setServerLoadYellowMin(50);
        kpiProperties.getCitrix().setServerLoadRedMin(80);
        kpiProperties.getCitrix().setFailedLogonsYellowAbove(10);

        CitrixMetricsHistory citrix =
                healthyCitrix();
        citrix.setServerLoadPercent(75);
        citrix.setFailedLogons(8);

        assertThat(service.calculateCitrixHealthAffection(citrix))
                .isEqualTo(15);
        assertThat(service.calculateCitrixHealthStatus(citrix))
                .isEqualTo("GREEN");
    }

    private ArubaSummary aruba() {

        ArubaNetworkStatusDto networkStatus =
                new ArubaNetworkStatusDto();
        networkStatus.setPercentage(75);
        networkStatus.setTechnicalDegradationValue(75);
        networkStatus.setColor("RED");

        ArubaSummary summary =
                new ArubaSummary();
        summary.setTotalAps(10);
        summary.setDownAps(1);
        summary.setInactiveAps(2);
        summary.setFirmwareOutdated(1);
        summary.setTotalSwitches(5);
        summary.setDownSwitches(2);
        summary.setSwitchesFirmwareUpgradeRequired(1);
        summary.setTotalWifiClients(20);
        summary.setMutualiaApsClients(5);
        summary.setMutualiaWifiClients(15);
        summary.setNetworkStatusDetails(networkStatus);

        return summary;
    }

    private ArubaSummary healthyAruba() {

        ArubaSummary summary =
                new ArubaSummary();
        summary.setTotalAps(10);
        summary.setDownAps(0);
        summary.setInactiveAps(0);
        summary.setFirmwareOutdated(0);
        summary.setTotalSwitches(5);
        summary.setDownSwitches(0);
        summary.setSwitchesFirmwareUpgradeRequired(0);
        summary.setTotalWifiClients(20);
        summary.setMutualiaApsClients(5);
        summary.setMutualiaWifiClients(15);

        return summary;
    }

    private CitrixMetricsHistory citrix() {

        CitrixMetricsHistory history =
                new CitrixMetricsHistory();
        history.setActiveSessions(100);
        history.setActiveLicenses(50);
        history.setAvailableDeliveryControllers(2);
        history.setTotalDeliveryControllers(4);
        history.setAverageLogonDurationSeconds(25);
        history.setServerLoadPercent(70);
        history.setFailedLogons(15);
        history.setCitrixHealth("GREEN");

        return history;
    }

    private CitrixMetricsHistory healthyCitrix() {

        CitrixMetricsHistory history =
                new CitrixMetricsHistory();
        history.setActiveSessions(100);
        history.setActiveLicenses(50);
        history.setAvailableDeliveryControllers(4);
        history.setTotalDeliveryControllers(4);
        history.setAverageLogonDurationSeconds(10);
        history.setServerLoadPercent(20);
        history.setFailedLogons(0);
        history.setCitrixHealth("GREEN");

        return history;
    }

    private Microsoft365MetricsHistory microsoft365() {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();
        history.setActiveUsers(100);
        history.setUnassignedLicenses(20);
        history.setOutlookStatus("HEALTHY");
        history.setTeamsStatus("HEALTHY");
        history.setSharePointStatus("HEALTHY");
        history.setSharePointStoragePercent(85);
        history.setRiskyUsers(0);
        history.setFailedSignIns(0);
        history.setUsersWithoutMfa(4);
        history.setAppsSecretsExpiringSoon(1);
        history.setUnusedApplications(0);
        history.setHighPrivilegeApplications(0);
        history.setNonCompliantDevices(60);
        history.setOutdatedWindowsDevices(3);
        history.setDevicesWithoutEncryption(2);
        history.setStaleDevices(0);
        history.setMicrosoft365Health("GREEN");

        return history;
    }

    private Microsoft365MetricsHistory healthyMicrosoft365() {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();
        history.setActiveUsers(100);
        history.setUnassignedLicenses(20);
        history.setOutlookStatus("HEALTHY");
        history.setTeamsStatus("HEALTHY");
        history.setSharePointStatus("HEALTHY");
        history.setSharePointStoragePercent(40);
        history.setRiskyUsers(0);
        history.setFailedSignIns(0);
        history.setUsersWithoutMfa(0);
        history.setAppsSecretsExpiringSoon(0);
        history.setUnusedApplications(0);
        history.setHighPrivilegeApplications(0);
        history.setNonCompliantDevices(10);
        history.setOutdatedWindowsDevices(0);
        history.setDevicesWithoutEncryption(0);
        history.setStaleDevices(0);
        history.setMicrosoft365Health("GREEN");

        return history;
    }

    private GlpiMetricsHistory glpi() {

        GlpiMetricsHistory history =
                new GlpiMetricsHistory();
        history.setOpenTickets(150);
        history.setCriticalOpenTickets(5);
        history.setSlaBreachedTickets(3);
        history.setCreatedToday(10);
        history.setClosedToday(4);
        history.setCreatedThisWeek(20);
        history.setClosedThisWeek(15);

        return history;
    }

    private GlpiMetricsHistory healthyGlpi() {

        GlpiMetricsHistory history =
                new GlpiMetricsHistory();
        history.setOpenTickets(0);
        history.setCriticalOpenTickets(0);
        history.setSlaBreachedTickets(0);
        history.setCreatedToday(10);
        history.setClosedToday(10);
        history.setCreatedThisWeek(20);
        history.setClosedThisWeek(20);

        return history;
    }
}
