package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.ManualSyncPlatformResultDto;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

/**
 * Scheduler de sincronización de fuentes simuladas.
 *
 * Genera métricas dinámicas de Citrix, Microsoft 365 y GLPI, las persiste como
 * snapshots en MySQL y crea capturas transversales para el dashboard de
 * análisis. Cada plataforma se sincroniza de forma aislada para que un fallo
 * parcial no bloquee el resto.
 */
@Service
public class MetricsSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(MetricsSyncService.class);

    private static final int RETENTION_DAYS = 90;
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final CitrixService citrixService;

    private final Microsoft365Service microsoft365Service;

    private final GlpiService glpiService;

    private final CitrixMetricsHistoryRepository citrixRepository;

    private final Microsoft365MetricsHistoryRepository microsoft365Repository;

    private final GlpiMetricsHistoryRepository glpiRepository;

    private final TransversalKpiAnalyticsService analyticsService;

    private final SimulationConsistencyService consistencyService;
    private final SynchronizationControlService synchronizationControlService;
    private final AtomicBoolean syncInProgress =
            new AtomicBoolean(false);

    public MetricsSyncService(
            CitrixService citrixService,
            Microsoft365Service microsoft365Service,
            GlpiService glpiService,
            CitrixMetricsHistoryRepository citrixRepository,
            Microsoft365MetricsHistoryRepository microsoft365Repository,
            GlpiMetricsHistoryRepository glpiRepository,
            TransversalKpiAnalyticsService analyticsService,
            SimulationConsistencyService consistencyService,
            SynchronizationControlService synchronizationControlService
    ) {

        this.citrixService = citrixService;
        this.microsoft365Service = microsoft365Service;
        this.glpiService = glpiService;
        this.citrixRepository = citrixRepository;
        this.microsoft365Repository = microsoft365Repository;
        this.glpiRepository = glpiRepository;
        this.analyticsService = analyticsService;
        this.consistencyService = consistencyService;
        this.synchronizationControlService = synchronizationControlService;
    }

    /**
     * Ejecuta la sincronización periódica de las fuentes simuladas y snapshots
     * de análisis.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncExternalPlatformMetricsOnStartup() {

        if (!synchronizationControlService.isAutomaticSyncEnabled()) {
            log.info("Automatic synchronization skipped because it is paused");
            return;
        }

        log.info("Sincronizacion inicial de metricas externas al arrancar Spring");
        syncExternalPlatformMetricsOnce();
    }

    @Scheduled(
            initialDelayString = "${metrics.sync.initial-delay-ms:3600000}",
            fixedRateString = "${metrics.sync.fixed-rate-ms:3600000}"
    )
    public void syncExternalPlatformMetrics() {

        if (!synchronizationControlService.isAutomaticSyncEnabled()) {
            log.info("Automatic synchronization skipped because it is paused");
            return;
        }

        syncExternalPlatformMetricsOnce();
    }

    /**
     * Ejecuta una sincronizacion de Citrix, Microsoft 365 y GLPI reutilizable
     * tanto por el scheduler como por la sincronizacion manual del panel de
     * configuracion.
     */
    public List<ManualSyncPlatformResultDto> syncExternalPlatformMetricsOnce() {

        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Se omite sincronizacion de metricas externas porque ya hay otra en curso");
            return List.of(new ManualSyncPlatformResultDto(
                    "Plataformas simuladas",
                    STATUS_IN_PROGRESS,
                    "Ya hay una sincronizacion de metricas externas en curso."
            ));
        }

        try {
            return doSyncExternalPlatformMetricsOnce();
        } finally {
            syncInProgress.set(false);
        }
    }

    private List<ManualSyncPlatformResultDto> doSyncExternalPlatformMetricsOnce() {

        log.info("Sincronizacion de metricas externas iniciada");

        List<ManualSyncPlatformResultDto> platformResults =
                new ArrayList<>();
        LocalDateTime collectedAt =
                LocalDateTime.now();
        CitrixSummary citrixSummary =
                null;
        Microsoft365Summary microsoft365Summary =
                null;
        GlpiSummary glpiSummary =
                null;

        try {

            citrixSummary =
                    citrixService.generateSimulatedSummary();

        } catch (Exception exception) {

            log.error("Error sincronizando metricas Citrix", exception);
            platformResults.add(error(
                    "Citrix",
                    "Error generando datos simulados Citrix: " + exception.getMessage()
            ));
        }

        try {

            microsoft365Summary =
                    microsoft365Service.generateSimulatedSummary();

        } catch (Exception exception) {

            log.error(
                    "Error sincronizando metricas Microsoft 365",
                    exception
            );
            platformResults.add(error(
                    "Microsoft 365",
                    "Error generando datos simulados Microsoft 365: " + exception.getMessage()
            ));
        }

        try {

            glpiSummary =
                    glpiService.generateSimulatedSummary();

        } catch (Exception exception) {

            log.error("Error sincronizando metricas GLPI", exception);
            platformResults.add(error(
                    "GLPI",
                    "Error generando datos simulados GLPI: " + exception.getMessage()
            ));
        }

        if (citrixSummary != null
                && microsoft365Summary != null
                && glpiSummary != null) {

            try {

                consistencyService.applyToGeneratedSummaries(
                        citrixSummary,
                        microsoft365Summary,
                        glpiSummary,
                        collectedAt
                );

            } catch (Exception exception) {

                log.error("Error aplicando coherencia a metricas simuladas", exception);
            }
        }

        if (citrixSummary != null) {
            try {
                syncCitrixMetrics(citrixSummary, collectedAt);
                platformResults.add(ok(
                        "Citrix",
                        "Datos simulados Citrix actualizados."
                ));
            } catch (Exception exception) {
                log.error("Error guardando metricas Citrix", exception);
                platformResults.add(error(
                        "Citrix",
                        "Error guardando metricas Citrix: " + exception.getMessage()
                ));
            }
        }

        if (microsoft365Summary != null) {
            try {
                syncMicrosoft365Metrics(microsoft365Summary, collectedAt);
                platformResults.add(ok(
                        "Microsoft 365",
                        "Datos simulados Microsoft 365 actualizados."
                ));
            } catch (Exception exception) {
                log.error("Error guardando metricas Microsoft 365", exception);
                platformResults.add(error(
                        "Microsoft 365",
                        "Error guardando metricas Microsoft 365: " + exception.getMessage()
                ));
            }
        }

        if (glpiSummary != null) {
            try {
                syncGlpiMetrics(glpiSummary, collectedAt);
                platformResults.add(ok(
                        "GLPI",
                        "Datos simulados GLPI actualizados."
                ));
            } catch (Exception exception) {
                log.error("Error guardando metricas GLPI", exception);
                platformResults.add(error(
                        "GLPI",
                        "Error guardando metricas GLPI: " + exception.getMessage()
                ));
            }
        }

        try {

            analyticsService.saveCurrentSnapshot(collectedAt);
            log.info("Snapshot transversal guardado");
            analyticsService.saveAnalysisSnapshot(collectedAt);
            log.info("Snapshot de análisis guardado");

        } catch (Exception exception) {

            log.error("Error guardando snapshots de análisis", exception);
        }

        cleanOldMetrics();

        log.info("Sincronizacion de metricas externas finalizada");

        return platformResults;
    }

    /**
     * Genera y persiste un snapshot Citrix simulado.
     */
    private void syncCitrixMetrics(
            CitrixSummary citrixSummary,
            LocalDateTime collectedAt
    ) {

        CitrixMetricsHistory citrixHistory =
                mapCitrix(citrixSummary, collectedAt);
        citrixRepository.save(citrixHistory);
        log.info("Datos Citrix guardados");
    }

    /**
     * Genera y persiste un snapshot Microsoft 365 simulado.
     */
    private void syncMicrosoft365Metrics(
            Microsoft365Summary microsoft365Summary,
            LocalDateTime collectedAt
    ) {

        Microsoft365MetricsHistory microsoft365History =
                mapMicrosoft365(microsoft365Summary, collectedAt);
        microsoft365Repository.save(microsoft365History);
        log.info("Datos Microsoft 365 guardados");
    }

    /**
     * Genera y persiste un snapshot GLPI simulado.
     */
    private void syncGlpiMetrics(
            GlpiSummary glpiSummary,
            LocalDateTime collectedAt
    ) {

        GlpiMetricsHistory glpiHistory =
                mapGlpi(glpiSummary, collectedAt);
        glpiRepository.save(glpiHistory);
        log.info("Datos GLPI guardados");
    }

    /**
     * Aplica retención de históricos para evitar crecimiento indefinido de las
     * tablas de snapshots simulados.
     */
    private void cleanOldMetrics() {

        // Se aplica retencion de 90 dias
        // para evitar crecimiento indefinido
        // de snapshots y mantener histórico
        // suficiente para análisis temporal.

        try {

            LocalDateTime cutoff =
                    LocalDateTime.now().minusDays(RETENTION_DAYS);

            log.info(
                    "Limpieza de históricos iniciada. Fecha limite: {}",
                    cutoff
            );

            long deletedCitrix =
                    citrixRepository.deleteByCollectedAtBefore(cutoff);
            long deletedMicrosoft365 =
                    microsoft365Repository.deleteByCollectedAtBefore(cutoff);
            long deletedGlpi =
                    glpiRepository.deleteByCollectedAtBefore(cutoff);

            log.info(
                    "Limpieza completada. Eliminados: Citrix={}, Microsoft365={}, GLPI={}",
                    deletedCitrix,
                    deletedMicrosoft365,
                    deletedGlpi
            );

        } catch (Exception exception) {

            log.error("Error limpiando históricos antiguos", exception);
        }
    }

    private CitrixMetricsHistory mapCitrix(
            CitrixSummary summary,
            LocalDateTime collectedAt
    ) {

        CitrixMetricsHistory history =
                new CitrixMetricsHistory();

        history.setActiveSessions(summary.getActiveSessions());
        history.setActiveLicenses(summary.getActiveLicenses());
        history.setAvailableDeliveryControllers(
                summary.getAvailableDeliveryControllers()
        );
        history.setTotalDeliveryControllers(
                summary.getTotalDeliveryControllers()
        );
        history.setDisconnectedSessions(summary.getDisconnectedSessions());
        history.setAverageLogonDurationSeconds(
                summary.getAverageLogonDurationSeconds()
        );
        history.setServerLoadPercent(summary.getServerLoadPercent());
        history.setFailedLogons(summary.getFailedLogons());
        history.setCitrixHealth(
                summary.getCitrixHealthDetails() != null
                        ? summary.getCitrixHealthDetails().getColor()
                        : "NO_DATA"
        );
        history.setCollectedAt(collectedAt);

        return history;
    }

    private Microsoft365MetricsHistory mapMicrosoft365(
            Microsoft365Summary summary,
            LocalDateTime collectedAt
    ) {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();

        history.setActiveUsers(summary.getActiveUsers());
        history.setUnassignedLicenses(summary.getUnassignedLicenses());
        history.setOutlookStatus(summary.getOutlookStatus());
        history.setTeamsStatus(summary.getTeamsStatus());
        history.setSharePointStatus(summary.getSharePointStatus());
        history.setNearlyFullMailboxes(summary.getNearlyFullMailboxes());
        history.setEmailsQuarantined(summary.getEmailsQuarantined());
        history.setSharePointStoragePercent(
                summary.getSharePointStoragePercent()
        );
        history.setRiskyUsers(summary.getRiskyUsers());
        history.setFailedSignIns(summary.getFailedSignIns());
        history.setUsersWithoutMfa(summary.getUsersWithoutMfa());
        history.setAppsSecretsExpiringSoon(
                summary.getAppsSecretsExpiringSoon()
        );
        history.setUnusedApplications(summary.getUnusedApplications());
        history.setHighPrivilegeApplications(
                summary.getHighPrivilegeApplications()
        );
        history.setNonCompliantDevices(summary.getNonCompliantDevices());
        history.setOutdatedWindowsDevices(
                summary.getOutdatedWindowsDevices()
        );
        history.setDevicesWithoutEncryption(
                summary.getDevicesWithoutEncryption()
        );
        history.setStaleDevices(summary.getStaleDevices());
        history.setMicrosoft365Health(
                summary.getMicrosoft365HealthDetails() != null
                        ? summary.getMicrosoft365HealthDetails().getColor()
                        : "NO_DATA"
        );
        history.setCollectedAt(collectedAt);

        return history;
    }

    private GlpiMetricsHistory mapGlpi(
            GlpiSummary summary,
            LocalDateTime collectedAt
    ) {

        GlpiMetricsHistory history =
                new GlpiMetricsHistory();

        history.setOpenTickets(summary.getOpenTickets());
        history.setArubaOpenTickets(summary.getArubaOpenTickets());
        history.setCitrixOpenTickets(summary.getCitrixOpenTickets());
        history.setMicrosoft365OpenTickets(summary.getMicrosoft365OpenTickets());
        history.setCriticalOpenTickets(summary.getCriticalOpenTickets());
        history.setSlaBreachedTickets(summary.getSlaBreachedTickets());
        history.setAverageResolutionHours(
                summary.getAverageResolutionHours()
        );
        history.setCreatedToday(summary.getCreatedToday());
        history.setClosedToday(summary.getClosedToday());
        history.setCreatedThisWeek(summary.getCreatedThisWeek());
        history.setClosedThisWeek(summary.getClosedThisWeek());
        history.setOperationalBacklog(summary.getOperationalBacklog());
        history.setCollectedAt(collectedAt);

        return history;
    }

    private ManualSyncPlatformResultDto ok(String name, String message) {
        return new ManualSyncPlatformResultDto(name, "OK", message);
    }

    private ManualSyncPlatformResultDto error(String name, String message) {
        return new ManualSyncPlatformResultDto(name, "ERROR", message);
    }
}

