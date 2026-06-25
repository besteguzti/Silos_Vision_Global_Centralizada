package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;

/**
 * Servicio de acceso a snapshots del panel de análisis.
 *
 * Encapsula consultas por periodo y detección de snapshots generados para que
 * el orquestador no dependa directamente de detalles del repositorio.
 */
@Service
public class AnalysisSnapshotService {

    private final AnalysisSnapshotRepository analysisSnapshotRepository;

    public AnalysisSnapshotService(AnalysisSnapshotRepository analysisSnapshotRepository) {
        this.analysisSnapshotRepository = analysisSnapshotRepository;
    }

    /**
     * Recupera snapshots posteriores a la fecha calculada por el periodo.
     */
    public List<AnalysisSnapshot> getSnapshots(String period) {

        return analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(LocalDateTime.now().minusDays(daysFromPeriod(period)));
    }

    /**
     * Indica si un timestamp pertenece a un escenario de prueba persistido.
     */
    public boolean isGeneratedScenario(LocalDateTime timestamp) {

        if (timestamp == null) {

            return false;
        }

        return analysisSnapshotRepository.existsGeneratedScenarioAt(timestamp);
    }

    /**
     * Traduce el selector del frontend a días de histórico.
     */
    public int daysFromPeriod(String period) {

        if ("7d".equalsIgnoreCase(period)) {

            return 7;
        }

        if ("90d".equalsIgnoreCase(period)) {

            return 90;
        }

        return 30;
    }
}

