package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PlatformSummarySeverityServiceTest {

    @Mock
    private CitrixMetricsHistoryRepository citrixRepository;

    @Mock
    private Microsoft365MetricsHistoryRepository microsoft365Repository;

    @Mock
    private GlpiMetricsHistoryRepository glpiRepository;

    @Mock
    private GlpiPlatformTicketService glpiPlatformTicketService;

    private final KpiProperties kpiProperties =
            new KpiProperties();

    @Test
    void citrixSummaryUsesAffectionTotalForTopSeverity() {
        CitrixMetricsHistory history =
                healthyCitrix();
        history.setAverageLogonDurationSeconds(30);
        history.setFailedLogons(15);

        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        CitrixSummary summary =
                new CitrixService(citrixRepository, glpiPlatformTicketService, kpiProperties)
                        .getSummary();

        assertThat(summary.getCitrixHealthDetails().getColor()).isEqualTo("GREEN");
        assertThat(summary.getCitrixHealthDetails().getPercentage()).isEqualTo(30);
        assertThat(summary.getCitrixHealthKpi().getStatus().name()).isEqualTo("GREEN");
    }

    @Test
    void citrixSummaryDoesNotBecomeCriticalWhenAffectionIsBelowRedRange() {
        CitrixMetricsHistory history =
                healthyCitrix();
        history.setActiveSessions(42);
        history.setActiveLicenses(580);
        history.setAvailableDeliveryControllers(3);
        history.setTotalDeliveryControllers(4);
        history.setAverageLogonDurationSeconds(21);
        history.setServerLoadPercent(75);
        history.setFailedLogons(7);

        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));
        when(glpiPlatformTicketService.getCitrixOpenTickets())
                .thenReturn(26);

        CitrixSummary summary =
                new CitrixService(citrixRepository, glpiPlatformTicketService, kpiProperties)
                        .getSummary();

        assertThat(summary.getCitrixHealthDetails().getColor()).isEqualTo("GREEN");
        assertThat(summary.getCitrixHealthDetails().getPercentage()).isEqualTo(30);
        assertThat(summary.getCitrixHealthKpi().getStatus().name()).isEqualTo("GREEN");
        assertThat(summary.getCitrixHealthDetails().getIndicators())
                .filteredOn(indicator -> "Errores de inicio".equals(indicator.getName()))
                .singleElement()
                .satisfies(indicator -> assertThat(indicator.getColor()).isEqualTo("YELLOW"));
    }

    @Test
    void microsoft365SummaryUsesFinalSeverityFromInternalIndicators() {
        Microsoft365MetricsHistory history =
                healthyMicrosoft365();
        history.setAppsSecretsExpiringSoon(1);
        history.setOutdatedWindowsDevices(1);

        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        Microsoft365Summary summary =
                new Microsoft365Service(microsoft365Repository, glpiPlatformTicketService, kpiProperties)
                        .getSummary();

        assertThat(summary.getMicrosoft365HealthDetails().getColor()).isEqualTo("GREEN");
        assertThat(summary.getMicrosoft365HealthDetails().getPercentage()).isEqualTo(4);
        assertThat(summary.getMicrosoft365HealthKpi().getStatus().name()).isEqualTo("GREEN");
    }

    @Test
    void glpiSummaryUsesFinalSeverityFromInternalIndicators() {
        GlpiMetricsHistory history =
                healthyGlpi();
        history.setOpenTickets(150);
        history.setCriticalOpenTickets(5);

        when(glpiRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        GlpiSummary summary =
                new GlpiService(glpiRepository, kpiProperties)
                        .getSummary();

        assertThat(summary.getGlpiHealthDetails().getColor()).isEqualTo("RED");
        assertThat(summary.getGlpiHealthDetails().getPercentage()).isEqualTo(67);
        assertThat(summary.getGlpiHealthKpi().getStatus().name()).isEqualTo("RED");
    }

    private CitrixMetricsHistory healthyCitrix() {
        CitrixMetricsHistory history =
                new CitrixMetricsHistory();
        history.setCollectedAt(LocalDateTime.now());
        history.setActiveSessions(100);
        history.setActiveLicenses(500);
        history.setAvailableDeliveryControllers(4);
        history.setTotalDeliveryControllers(4);
        history.setAverageLogonDurationSeconds(10);
        history.setServerLoadPercent(20);
        history.setFailedLogons(0);
        history.setCitrixHealth("GREEN");

        return history;
    }

    private Microsoft365MetricsHistory healthyMicrosoft365() {
        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();
        history.setCollectedAt(LocalDateTime.now());
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

    private GlpiMetricsHistory healthyGlpi() {
        GlpiMetricsHistory history =
                new GlpiMetricsHistory();
        history.setCollectedAt(LocalDateTime.now());
        history.setOpenTickets(0);
        history.setCriticalOpenTickets(0);
        history.setCreatedToday(10);
        history.setClosedToday(10);
        history.setCreatedThisWeek(20);
        history.setClosedThisWeek(20);

        return history;
    }
}
