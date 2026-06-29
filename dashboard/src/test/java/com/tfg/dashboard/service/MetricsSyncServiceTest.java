package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.dto.CitrixHealthStatusDto;
import com.tfg.dashboard.dto.ManualSyncPlatformResultDto;
import com.tfg.dashboard.dto.Microsoft365HealthStatusDto;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

@ExtendWith(MockitoExtension.class)
class MetricsSyncServiceTest {

    @Mock
    private CitrixService citrixService;

    @Mock
    private Microsoft365Service microsoft365Service;

    @Mock
    private GlpiService glpiService;

    @Mock
    private CitrixMetricsHistoryRepository citrixRepository;

    @Mock
    private Microsoft365MetricsHistoryRepository microsoft365Repository;

    @Mock
    private GlpiMetricsHistoryRepository glpiRepository;

    @Mock
    private TransversalKpiAnalyticsService analyticsService;

    @Mock
    private SimulationConsistencyService consistencyService;

    @Mock
    private SynchronizationControlService synchronizationControlService;

    private MetricsSyncService service;

    @BeforeEach
    void setUp() {

        service = new MetricsSyncService(
                citrixService,
                microsoft365Service,
                glpiService,
                citrixRepository,
                microsoft365Repository,
                glpiRepository,
                analyticsService,
                consistencyService,
                synchronizationControlService
        );
    }

    @Test
    void savesSnapshotsForAllPlatforms() {

        mockSuccessfulSummaries();
        automaticSyncEnabled();

        service.syncExternalPlatformMetrics();

        ArgumentCaptor<CitrixMetricsHistory> citrixCaptor =
                ArgumentCaptor.forClass(CitrixMetricsHistory.class);
        ArgumentCaptor<Microsoft365MetricsHistory> microsoftCaptor =
                ArgumentCaptor.forClass(Microsoft365MetricsHistory.class);
        ArgumentCaptor<GlpiMetricsHistory> glpiCaptor =
                ArgumentCaptor.forClass(GlpiMetricsHistory.class);

        verify(citrixRepository).save(citrixCaptor.capture());
        verify(microsoft365Repository).save(microsoftCaptor.capture());
        verify(glpiRepository).save(glpiCaptor.capture());

        assertThat(citrixCaptor.getValue().getActiveSessions())
                .isEqualTo(300);
        assertThat(microsoftCaptor.getValue().getActiveUsers())
                .isEqualTo(1200);
        assertThat(glpiCaptor.getValue().getOpenTickets())
                .isEqualTo(80);
        assertThat(glpiCaptor.getValue().getArubaOpenTickets())
                .isEqualTo(20);
        assertThat(glpiCaptor.getValue().getCitrixOpenTickets())
                .isEqualTo(45);
        assertThat(glpiCaptor.getValue().getMicrosoft365OpenTickets())
                .isEqualTo(15);
        assertThat(glpiCaptor.getValue().getOpenTickets())
                .isEqualTo(glpiCaptor.getValue().getArubaOpenTickets()
                        + glpiCaptor.getValue().getCitrixOpenTickets()
                        + glpiCaptor.getValue().getMicrosoft365OpenTickets());
        assertThat(citrixCaptor.getValue().getCollectedAt()).isNotNull();
        assertThat(microsoftCaptor.getValue().getCollectedAt()).isNotNull();
        assertThat(glpiCaptor.getValue().getCollectedAt()).isNotNull();
        verify(analyticsService).saveCurrentSnapshot(any());
    }

    @Test
    void continuesWhenCitrixFails() {

        when(citrixService.generateSimulatedSummary())
                .thenThrow(new RuntimeException("Citrix falla"));
        when(microsoft365Service.generateSimulatedSummary())
                .thenReturn(microsoft365Summary());
        when(glpiService.generateSimulatedSummary())
                .thenReturn(glpiSummary());
        automaticSyncEnabled();

        service.syncExternalPlatformMetrics();

        verify(citrixRepository, never()).save(any());
        verify(microsoft365Repository).save(any());
        verify(glpiRepository).save(any());
    }

    @Test
    void continuesWhenMicrosoft365Fails() {

        when(citrixService.generateSimulatedSummary())
                .thenReturn(citrixSummary());
        when(microsoft365Service.generateSimulatedSummary())
                .thenThrow(new RuntimeException("M365 falla"));
        when(glpiService.generateSimulatedSummary())
                .thenReturn(glpiSummary());
        automaticSyncEnabled();

        service.syncExternalPlatformMetrics();

        verify(citrixRepository).save(any());
        verify(microsoft365Repository, never()).save(any());
        verify(glpiRepository).save(any());
    }

    @Test
    void continuesWhenGlpiFails() {

        when(citrixService.generateSimulatedSummary())
                .thenReturn(citrixSummary());
        when(microsoft365Service.generateSimulatedSummary())
                .thenReturn(microsoft365Summary());
        when(glpiService.generateSimulatedSummary())
                .thenThrow(new RuntimeException("GLPI falla"));
        automaticSyncEnabled();

        service.syncExternalPlatformMetrics();

        verify(citrixRepository).save(any());
        verify(microsoft365Repository).save(any());
        verify(glpiRepository, never()).save(any());
    }

    @Test
    void appliesNinetyDayRetentionPolicy() {

        mockSuccessfulSummaries();
        automaticSyncEnabled();

        service.syncExternalPlatformMetrics();

        verify(citrixRepository).deleteByCollectedAtBefore(any());
        verify(microsoft365Repository).deleteByCollectedAtBefore(any());
        verify(glpiRepository).deleteByCollectedAtBefore(any());
    }

    @Test
    void skipsSecondMetricsSyncWhenAnotherOneIsRunning() throws Exception {

        CountDownLatch firstSyncStarted =
                new CountDownLatch(1);
        CountDownLatch allowFirstSyncToFinish =
                new CountDownLatch(1);

        doAnswer(invocation -> {
            firstSyncStarted.countDown();
            assertThat(allowFirstSyncToFinish.await(2, TimeUnit.SECONDS))
                    .isTrue();
            return citrixSummary();
        }).when(citrixService).generateSimulatedSummary();
        when(microsoft365Service.generateSimulatedSummary())
                .thenReturn(microsoft365Summary());
        when(glpiService.generateSimulatedSummary())
                .thenReturn(glpiSummary());

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {
            Future<List<ManualSyncPlatformResultDto>> firstSync =
                    executor.submit(service::syncExternalPlatformMetricsOnce);

            assertThat(firstSyncStarted.await(2, TimeUnit.SECONDS))
                    .isTrue();

            List<ManualSyncPlatformResultDto> secondSync =
                    service.syncExternalPlatformMetricsOnce();

            assertThat(secondSync)
                    .singleElement()
                    .satisfies(result -> {
                        assertThat(result.getName()).isEqualTo("Plataformas simuladas");
                        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
                    });

            allowFirstSyncToFinish.countDown();
            assertThat(firstSync.get(2, TimeUnit.SECONDS))
                    .extracting(ManualSyncPlatformResultDto::getStatus)
                    .contains("OK");

        } finally {
            allowFirstSyncToFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void scheduledMetricsSyncIsSkippedWhenAutomaticSynchronizationIsPaused() {

        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(false);

        service.syncExternalPlatformMetrics();

        verify(citrixService, never()).generateSimulatedSummary();
        verify(microsoft365Service, never()).generateSimulatedSummary();
        verify(glpiService, never()).generateSimulatedSummary();
        verify(citrixRepository, never()).save(any());
        verify(microsoft365Repository, never()).save(any());
        verify(glpiRepository, never()).save(any());
    }

    @Test
    void manualMetricsSyncStillRunsWhenAutomaticSynchronizationIsPaused() {

        mockSuccessfulSummaries();

        service.syncExternalPlatformMetricsOnce();

        verify(citrixRepository).save(any());
        verify(microsoft365Repository).save(any());
        verify(glpiRepository).save(any());
    }

    private void mockSuccessfulSummaries() {

        when(citrixService.generateSimulatedSummary())
                .thenReturn(citrixSummary());
        when(microsoft365Service.generateSimulatedSummary())
                .thenReturn(microsoft365Summary());
        when(glpiService.generateSimulatedSummary())
                .thenReturn(glpiSummary());
    }

    private void automaticSyncEnabled() {
        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(true);
    }

    private CitrixSummary citrixSummary() {

        CitrixSummary summary =
                new CitrixSummary();

        summary.setActiveSessions(300);
        CitrixHealthStatusDto citrixHealthDetails =
                new CitrixHealthStatusDto();
        citrixHealthDetails.setColor("GREEN");
        summary.setCitrixHealthDetails(citrixHealthDetails);

        return summary;
    }

    private Microsoft365Summary microsoft365Summary() {

        Microsoft365Summary summary =
                new Microsoft365Summary();

        summary.setActiveUsers(1200);
        Microsoft365HealthStatusDto microsoft365HealthDetails =
                new Microsoft365HealthStatusDto();
        microsoft365HealthDetails.setColor("GREEN");
        summary.setMicrosoft365HealthDetails(microsoft365HealthDetails);

        return summary;
    }

    private GlpiSummary glpiSummary() {

        GlpiSummary summary =
                new GlpiSummary();

        summary.setOpenTickets(80);
        summary.setArubaOpenTickets(20);
        summary.setCitrixOpenTickets(45);
        summary.setMicrosoft365OpenTickets(15);

        return summary;
    }
}
