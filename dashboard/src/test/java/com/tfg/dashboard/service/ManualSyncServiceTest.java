package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.dto.ManualSyncPlatformResultDto;
import com.tfg.dashboard.dto.ManualSyncResponseDto;

@ExtendWith(MockitoExtension.class)
class ManualSyncServiceTest {

    @Mock
    private ArubaService arubaService;

    @Mock
    private MetricsSyncService metricsSyncService;

    @Mock
    private SynchronizationControlService synchronizationControlService;

    @Test
    void manualSyncReturnsOkWhenAllPlatformsFinishSuccessfully() {
        when(metricsSyncService.syncExternalPlatformMetricsOnce())
                .thenReturn(List.of(
                        ok("Citrix"),
                        ok("Microsoft 365"),
                        ok("GLPI")
                ));

        ManualSyncResponseDto response =
                service().syncAllPlatformsManually();

        assertThat(response.getStatus()).isEqualTo("OK");
        assertThat(response.getPlatforms())
                .extracting(ManualSyncPlatformResultDto::getName)
                .containsExactly("Aruba", "Citrix", "Microsoft 365", "GLPI");
        assertThat(response.getPlatforms())
                .allMatch(result -> "OK".equals(result.getStatus()));
        verify(arubaService).syncAll();
        verify(metricsSyncService).syncExternalPlatformMetricsOnce();
    }

    @Test
    void manualSyncReturnsPartialErrorAndIdentifiesFailingPlatform() {
        doThrow(new RuntimeException("Aruba Central no responde"))
                .when(arubaService)
                .syncAll();
        when(metricsSyncService.syncExternalPlatformMetricsOnce())
                .thenReturn(List.of(
                        ok("Citrix"),
                        ok("Microsoft 365"),
                        ok("GLPI")
                ));

        ManualSyncResponseDto response =
                service().syncAllPlatformsManually();

        assertThat(response.getStatus()).isEqualTo("PARTIAL_ERROR");
        assertThat(response.getPlatforms())
                .anySatisfy(result -> {
                    assertThat(result.getName()).isEqualTo("Aruba");
                    assertThat(result.getStatus()).isEqualTo("ERROR");
                    assertThat(result.getMessage()).contains("Aruba Central no responde");
                });
        assertThat(response.getMessage()).contains("errores parciales");
    }

    @Test
    void manualSyncReturnsInProgressWhenAnotherSyncIsRunning() throws Exception {
        ManualSyncService service =
                service();
        CountDownLatch arubaSyncStarted =
                new CountDownLatch(1);
        CountDownLatch allowFirstSyncToFinish =
                new CountDownLatch(1);

        doAnswer(invocation -> {
            arubaSyncStarted.countDown();
            assertThat(allowFirstSyncToFinish.await(2, TimeUnit.SECONDS))
                    .isTrue();
            return null;
        }).when(arubaService).syncAll();
        when(metricsSyncService.syncExternalPlatformMetricsOnce())
                .thenReturn(List.of(
                        ok("Citrix"),
                        ok("Microsoft 365"),
                        ok("GLPI")
                ));

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {
            Future<ManualSyncResponseDto> firstSync =
                    executor.submit(service::syncAllPlatformsManually);

            assertThat(arubaSyncStarted.await(2, TimeUnit.SECONDS))
                    .isTrue();

            ManualSyncResponseDto response =
                    service.syncAllPlatformsManually();

            assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(response.getPlatforms()).isEmpty();

            allowFirstSyncToFinish.countDown();
            assertThat(firstSync.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("OK");

        } finally {
            allowFirstSyncToFinish.countDown();
            executor.shutdownNow();
        }

        verify(arubaService).syncAll();
        verify(metricsSyncService).syncExternalPlatformMetricsOnce();
    }

    @Test
    void manualSyncReturnsInProgressWhenSimulatedMetricsAreAlreadyRunning() {
        when(metricsSyncService.syncExternalPlatformMetricsOnce())
                .thenReturn(List.of(new ManualSyncPlatformResultDto(
                        "Plataformas simuladas",
                        "IN_PROGRESS",
                        "Ya hay una sincronizacion de metricas externas en curso."
                )));

        ManualSyncResponseDto response =
                service().syncAllPlatformsManually();

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getMessage()).contains("sincronizacion en curso");
        assertThat(response.getPlatforms())
                .extracting(ManualSyncPlatformResultDto::getStatus)
                .contains("IN_PROGRESS");
    }

    @Test
    void manualSyncStillRunsWhenAutomaticSynchronizationIsPaused() {
        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(false);
        when(metricsSyncService.syncExternalPlatformMetricsOnce())
                .thenReturn(List.of(
                        ok("Citrix"),
                        ok("Microsoft 365"),
                        ok("GLPI")
                ));

        ManualSyncResponseDto response =
                new ManualSyncService(
                        metricsSyncService,
                        arubaService,
                        synchronizationControlService
                ).syncAllPlatformsManually();

        assertThat(response.getStatus()).isEqualTo("OK");
        verify(arubaService).syncAll();
        verify(metricsSyncService).syncExternalPlatformMetricsOnce();
    }

    private ManualSyncService service() {
        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(true);
        return new ManualSyncService(
                metricsSyncService,
                arubaService,
                synchronizationControlService
        );
    }

    private ManualSyncPlatformResultDto ok(String name) {
        return new ManualSyncPlatformResultDto(
                name,
                "OK",
                "Sincronizacion correcta"
        );
    }
}
