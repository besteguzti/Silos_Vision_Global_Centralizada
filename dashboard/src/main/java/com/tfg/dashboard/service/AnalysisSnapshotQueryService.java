package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Encapsula el acceso a snapshots del panel de análisis.
 */
@Service
public class AnalysisSnapshotQueryService {

    private final AnalysisSnapshotService analysisSnapshotService;

    public AnalysisSnapshotQueryService(AnalysisSnapshotService analysisSnapshotService) {

        this.analysisSnapshotService = analysisSnapshotService;
    }

    public List<AnalysisSnapshot> getAnalysisSnapshots(String period) {

        return analysisSnapshotService.getSnapshots(period);
    }

    public boolean isGeneratedScenario(LocalDateTime timestamp) {

        return analysisSnapshotService.isGeneratedScenario(timestamp);
    }
}

