package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.ManualSyncPlatformResultDto;
import com.tfg.dashboard.dto.ManualSyncResponseDto;

@Service
public class ManualSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(ManualSyncService.class);

    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "ERROR";
    private static final String STATUS_PARTIAL_ERROR = "PARTIAL_ERROR";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final ArubaService arubaService;
    private final MetricsSyncService metricsSyncService;
    private final SynchronizationControlService synchronizationControlService;
    private final AtomicBoolean syncInProgress =
            new AtomicBoolean(false);

    public ManualSyncService(
            MetricsSyncService metricsSyncService,
            ArubaService arubaService,
            SynchronizationControlService synchronizationControlService
    ) {
        this.metricsSyncService = metricsSyncService;
        this.arubaService = arubaService;
        this.synchronizationControlService = synchronizationControlService;
    }

    public ManualSyncResponseDto syncAllPlatformsManually() {

        LocalDateTime startedAt =
                LocalDateTime.now();

        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Manual sync requested while another sync is already running");
            return new ManualSyncResponseDto(
                    STATUS_IN_PROGRESS,
                    "Ya hay una sincronizacion en curso.",
                    startedAt,
                    LocalDateTime.now(),
                    List.of()
            );
        }

        if (!synchronizationControlService.isAutomaticSyncEnabled()) {
            log.info("Manual synchronization requested while automatic synchronization is paused");
        }

        log.info("Manual sync requested from configuration panel");

        List<ManualSyncPlatformResultDto> platformResults =
                new ArrayList<>();

        try {
            platformResults.add(syncAruba());
            platformResults.addAll(syncSimulatedPlatforms());

            boolean hasErrors =
                    platformResults.stream()
                            .anyMatch(result -> STATUS_ERROR.equals(result.getStatus()));
            boolean hasSyncInProgress =
                    platformResults.stream()
                            .anyMatch(result -> STATUS_IN_PROGRESS.equals(result.getStatus()));

            String status =
                    hasSyncInProgress
                            ? STATUS_IN_PROGRESS
                            : hasErrors ? STATUS_PARTIAL_ERROR : STATUS_OK;
            String message =
                    hasSyncInProgress
                            ? "Ya hay una sincronizacion en curso."
                            : hasErrors
                            ? "Sincronizacion completada con errores parciales."
                            : "Sincronizacion completada correctamente.";

            for (ManualSyncPlatformResultDto result : platformResults) {
                log.info(
                        "Manual sync platform result: platform={}, status={}, message={}",
                        result.getName(),
                        result.getStatus(),
                        result.getMessage()
                );
            }

            if (hasErrors) {
                log.warn("Manual sync completed with partial errors");
            } else {
                log.info("Manual sync completed successfully");
            }

            return new ManualSyncResponseDto(
                    status,
                    message,
                    startedAt,
                    LocalDateTime.now(),
                    platformResults
            );

        } finally {
            syncInProgress.set(false);
        }
    }

    private ManualSyncPlatformResultDto syncAruba() {
        try {
            arubaService.syncAll();
            return ok("Aruba", "Sincronizacion Aruba completada.");
        } catch (Exception exception) {
            log.error("Manual sync failed for Aruba", exception);
            return error(
                    "Aruba",
                    "No se pudo sincronizar Aruba: " + exception.getMessage()
            );
        }
    }

    private List<ManualSyncPlatformResultDto> syncSimulatedPlatforms() {
        try {
            return metricsSyncService.syncExternalPlatformMetricsOnce();
        } catch (Exception exception) {
            log.error("Manual sync failed for simulated platforms", exception);
            return List.of(error(
                    "Plataformas simuladas",
                    "No se pudieron sincronizar Citrix, Microsoft 365 y GLPI: "
                            + exception.getMessage()
            ));
        }
    }

    private ManualSyncPlatformResultDto ok(String name, String message) {
        return new ManualSyncPlatformResultDto(name, STATUS_OK, message);
    }

    private ManualSyncPlatformResultDto error(String name, String message) {
        return new ManualSyncPlatformResultDto(name, STATUS_ERROR, message);
    }
}
