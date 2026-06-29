package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

@ExtendWith(MockitoExtension.class)
class MainDashboardServiceTest {

    @Mock
    private ArubaService arubaService;

    @Mock
    private CitrixMetricsHistoryRepository citrixRepository;

    @Mock
    private Microsoft365MetricsHistoryRepository microsoft365Repository;

    @Mock
    private GlpiMetricsHistoryRepository glpiRepository;

    private MainDashboardService service;

    private KpiScoringService kpiScoringService;
    private KpiProperties kpiProperties;

    @BeforeEach
    void setUp() {

        kpiProperties =
                new KpiProperties();
        kpiScoringService =
                new KpiScoringService(kpiProperties);

        service = new MainDashboardService(
                arubaService,
                citrixRepository,
                microsoft365Repository,
                glpiRepository,
                kpiScoringService,
                new GlobalKpiCalculationService(kpiScoringService, kpiProperties),
                new DashboardFreshnessService(kpiScoringService, kpiProperties),
                kpiProperties
        );
    }

    @Test
    void globalHealthIsRedWhenWeightedAffectationIsHigh() {

        baseHealthyData();

        ArubaSummary aruba =
                healthyAruba();
        aruba.setDownAps(8);
        aruba.setTotalWifiClients(0);
        aruba.setTotalSwitches(10);
        aruba.setDownSwitches(6);
        aruba.getNetworkStatusDetails().setPercentage(100);
        aruba.getNetworkStatusDetails().setColor("RED");

        CitrixMetricsHistory citrix =
                healthyCitrix(LocalDateTime.now());
        citrix.setActiveSessions(0);
        citrix.setAvailableDeliveryControllers(0);
        citrix.setAverageLogonDurationSeconds(80);
        citrix.setServerLoadPercent(90);
        citrix.setFailedLogons(30);

        Microsoft365MetricsHistory microsoft365 =
                healthyMicrosoft365(LocalDateTime.now());
        microsoft365.setSharePointStoragePercent(95);
        microsoft365.setUsersWithoutMfa(5);
        microsoft365.setNonCompliantDevices(51);
        microsoft365.setDevicesWithoutEncryption(1);
        microsoft365.setStaleDevices(1);

        when(arubaService.getSummary())
                .thenReturn(aruba);
        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(citrix));
        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(microsoft365));

        MainDashboardSummary summary =
                service.getSummary();

        assertThat(summary.getGlobalHealthPercentage())
                .isGreaterThanOrEqualTo(67);
        assertThat(summary.getGlobalHealth()).isEqualTo("RED");
    }

    @Test
    void globalHealthCannotBeGreenWhenAnySourceIsStale() {

        baseHealthyData();

        Microsoft365MetricsHistory microsoft365 =
                healthyMicrosoft365(LocalDateTime.now().minusMinutes(
                        kpiProperties.getFreshness().getMicrosoft365Minutes() + 1));

        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(microsoft365));

        MainDashboardSummary summary =
                service.getSummary();

        assertThat(summary.getDataStatus()).isEqualTo("STALE");
        assertThat(summary.getGlobalHealth()).isNotEqualTo("GREEN");
        assertThat(summary.getGlobalCriticalityStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getGlobalAvailabilityStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getOperationalPressureStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getTechnicalDegradationStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getSlaRiskStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getOperationalBacklogStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getUserImpactStatus()).isNotEqualTo("GREEN");
        assertThat(summary.getAffectedServicesStatus()).isNotEqualTo("GREEN");
    }

    @Test
    void globalHealthCannotBeGreenWhenAnySourceHasNoData() {

        baseHealthyData();

        when(glpiRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.empty());

        MainDashboardSummary summary =
                service.getSummary();

        assertThat(summary.getDataStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getGlobalHealth()).isNotEqualTo("GREEN");
        assertThat(summary.getGlobalCriticalityStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getGlobalAvailabilityStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getOperationalPressureStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getTechnicalDegradationStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getSlaRiskStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getOperationalBacklogStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getUserImpactStatus()).isEqualTo("NO_DATA");
        assertThat(summary.getAffectedServicesStatus()).isEqualTo("NO_DATA");
    }

    @Test
    void globalAvailabilityIsExposedAsHealthKpi() {

        baseHealthyData();

        MainDashboardSummary summary =
                service.getSummary();

        assertThat(summary.getGlobalAvailability()).isGreaterThanOrEqualTo(67);
        assertThat(summary.getGlobalAvailabilityStatus()).isEqualTo("GREEN");
    }

    @Test
    void citrixComponentUsesAffectionTotalForTopStatus() {

        baseHealthyData();

        CitrixMetricsHistory citrix =
                healthyCitrix(LocalDateTime.now());
        citrix.setActiveSessions(42);
        citrix.setActiveLicenses(580);
        citrix.setAvailableDeliveryControllers(3);
        citrix.setTotalDeliveryControllers(4);
        citrix.setAverageLogonDurationSeconds(21);
        citrix.setServerLoadPercent(75);
        citrix.setFailedLogons(7);

        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(citrix));

        MainDashboardSummary summary =
                service.getSummary();

        assertThat(summary.getKpis())
                .flatExtracting(kpi -> kpi.getComponents())
                .filteredOn(component -> "citrix_health".equals(component.getId()))
                .allSatisfy(component -> {
                    assertThat(component.getScore()).isEqualTo(30);
                    assertThat(component.getStatus().name()).isEqualTo("GREEN");
                });
    }

    private void baseHealthyData() {

        when(arubaService.getSummary())
                .thenReturn(healthyAruba());
        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(healthyCitrix(LocalDateTime.now())));
        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(
                        Optional.of(healthyMicrosoft365(LocalDateTime.now()))
                );
        when(glpiRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(healthyGlpi(LocalDateTime.now())));
    }

    private ArubaSummary healthyAruba() {

        ArubaSummary summary =
                new ArubaSummary();

        ArubaNetworkStatusDto networkStatusDetails =
                new ArubaNetworkStatusDto();
        networkStatusDetails.setColor("GREEN");
        networkStatusDetails.setPercentage(0);
        summary.setNetworkStatusDetails(networkStatusDetails);
        summary.setDataStatus("OK");
        summary.setLastUpdated(LocalDateTime.now());
        summary.setTotalAps(10);
        summary.setTotalSwitches(5);
        summary.setTotalWifiClients(10);

        return summary;
    }

    private CitrixMetricsHistory healthyCitrix(
            LocalDateTime collectedAt
    ) {

        CitrixMetricsHistory history =
                new CitrixMetricsHistory();

        history.setCitrixHealth("GREEN");
        history.setActiveSessions(10);
        history.setTotalDeliveryControllers(4);
        history.setAvailableDeliveryControllers(4);
        history.setCollectedAt(collectedAt);

        return history;
    }

    private Microsoft365MetricsHistory healthyMicrosoft365(
            LocalDateTime collectedAt
    ) {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();

        history.setMicrosoft365Health("GREEN");
        history.setActiveUsers(100);
        history.setUnassignedLicenses(20);
        history.setOutlookStatus("HEALTHY");
        history.setTeamsStatus("HEALTHY");
        history.setSharePointStatus("HEALTHY");
        history.setSharePointStoragePercent(40);
        history.setUsersWithoutMfa(0);
        history.setAppsSecretsExpiringSoon(0);
        history.setNonCompliantDevices(10);
        history.setOutdatedWindowsDevices(0);
        history.setDevicesWithoutEncryption(0);
        history.setStaleDevices(0);
        history.setCollectedAt(collectedAt);

        return history;
    }

    private GlpiMetricsHistory healthyGlpi(
            LocalDateTime collectedAt
    ) {

        GlpiMetricsHistory history =
                new GlpiMetricsHistory();

        history.setCollectedAt(collectedAt);

        return history;
    }
}
