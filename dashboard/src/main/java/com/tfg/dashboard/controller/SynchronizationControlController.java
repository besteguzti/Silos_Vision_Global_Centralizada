package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.SynchronizationControlDto;
import com.tfg.dashboard.service.SynchronizationControlService;

/**
 * Endpoints para consultar, pausar y reanudar la sincronización automática. El boton manual sigue
 * usando POST /api/metrics/sync.
 */
@RestController
@RequestMapping("/api/metrics/sync-control")
public class SynchronizationControlController {

    private final SynchronizationControlService synchronizationControlService;

    public SynchronizationControlController(
            SynchronizationControlService synchronizationControlService
    ) {
        this.synchronizationControlService = synchronizationControlService;
    }

    @GetMapping
    public SynchronizationControlDto getSynchronizationControl() {
        return synchronizationControlService.getStatus();
    }

    @PostMapping("/pause")
    public SynchronizationControlDto pauseSynchronization() {
        return synchronizationControlService.pauseAutomaticSync();
    }

    @PostMapping("/resume")
    public SynchronizationControlDto resumeSynchronization() {
        return synchronizationControlService.resumeAutomaticSync();
    }
}
