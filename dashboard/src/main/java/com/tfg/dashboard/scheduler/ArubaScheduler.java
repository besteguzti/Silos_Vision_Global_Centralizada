package com.tfg.dashboard.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tfg.dashboard.service.ArubaService;
import com.tfg.dashboard.service.SynchronizationControlService;

// Scheduler que lanza la sincronización periódica de Aruba.
@Component
public class ArubaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ArubaScheduler.class);

    private final ArubaService arubaService;
    private final SynchronizationControlService synchronizationControlService;

    public ArubaScheduler(
            ArubaService arubaService,
            SynchronizationControlService synchronizationControlService
    ) {
        this.arubaService = arubaService;
        this.synchronizationControlService = synchronizationControlService;
    }

    // Ejecuta una sincronizacion inicial al arrancar Spring.
    @EventListener(ApplicationReadyEvent.class)
    public void syncArubaOnStartup() {

        if (!synchronizationControlService.isAutomaticSyncEnabled()) {
            log.info("Automatic synchronization skipped because it is paused");
            return;
        }

        log.info("Sincronizacion inicial Aruba al arrancar Spring");
        syncAruba();
    }

    // Sincroniza APs, switches, clientes WiFi y snapshots derivados de Aruba.

    @Scheduled(initialDelayString = "${aruba.sync.initial-delay-ms:3600000}", fixedRateString = "${aruba.sync.fixed-rate-ms:3600000}")
    public void syncAruba() {

        if (!synchronizationControlService.isAutomaticSyncEnabled()) {
            log.info("Automatic synchronization skipped because it is paused");
            return;
        }

        log.info("Sincronizando datos Aruba en MySQL");

        try {

            arubaService.syncAll();
            log.info("Sincronizacion Aruba finalizada");
        } catch (Exception exception) {
            log.error("Error sincronizando datos Aruba", exception);
        }
    }
}

