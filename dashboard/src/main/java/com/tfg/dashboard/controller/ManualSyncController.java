package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.dashboard.dto.ManualSyncResponseDto;
import com.tfg.dashboard.service.ManualSyncService;

@RestController
@RequestMapping("/api/metrics")
public class ManualSyncController {

    private final ManualSyncService manualSyncService;

    public ManualSyncController(ManualSyncService manualSyncService) {
        this.manualSyncService = manualSyncService;
    }

    @PostMapping("/sync")
    public ManualSyncResponseDto syncPlatforms() {
        return manualSyncService.syncAllPlatformsManually();
    }
}
