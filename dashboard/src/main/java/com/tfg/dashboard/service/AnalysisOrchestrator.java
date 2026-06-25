package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.AnalyticsComparePoint;
import com.tfg.dashboard.dto.OperationalImpactAnalysisResponse;
import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;

/**
 * Orquesta los datos del panel de análisis exploratorio.
 *
 * Mantiene la fachada del módulo de análisis y delega la construcción de
 * snapshots y respuestas a servicios especializados.
 */
@Service
public class AnalysisOrchestrator {

        private final AnalysisSnapshotRepository analysisSnapshotRepository;
        private final AnalysisSnapshotQueryService snapshotQueryService;
        private final AnalysisSnapshotBuilder snapshotBuilder;
        private final AnalysisPanelResponseService panelResponseService;
        private final AnalysisTestScenarioService testScenarioService;
        private final ImpactAnalysisService impactAnalysisService;
        private final AnalysisTimelineService analysisTimelineService;

        public AnalysisOrchestrator(
                        AnalysisSnapshotRepository analysisSnapshotRepository,
                        AnalysisSnapshotQueryService snapshotQueryService,
                        AnalysisSnapshotBuilder snapshotBuilder,
                        AnalysisPanelResponseService panelResponseService,
                        AnalysisTestScenarioService testScenarioService,
                        ImpactAnalysisService impactAnalysisService,
                        AnalysisTimelineService analysisTimelineService) {

                this.analysisSnapshotRepository = analysisSnapshotRepository;
                this.snapshotQueryService = snapshotQueryService;
                this.snapshotBuilder = snapshotBuilder;
                this.panelResponseService = panelResponseService;
                this.testScenarioService = testScenarioService;
                this.impactAnalysisService = impactAnalysisService;
                this.analysisTimelineService = analysisTimelineService;
        }

        /**
         * Asegura que exista histórico suficiente para el periodo y devuelve los
         * snapshots ordenados.
         */
        public List<AnalysisSnapshot> getAnalysisSnapshots(String period) {

                testScenarioService.ensureAnalysisSnapshots(period);
                return snapshotQueryService.getAnalysisSnapshots(period);
        }

        /**
         * Construye la respuesta principal del panel de analisis exploratorio.
         */
        public OperationalImpactAnalysisResponse getGlpiPlatformRelation(String period) {

                testScenarioService.ensureAnalysisSnapshots(period);
                return panelResponseService.buildOperationalImpactResponse(period);
        }

        public List<AnalyticsComparePoint> getTechnicalDegradationImpact(String period) {

                testScenarioService.ensureAnalysisSnapshots(period);
                return impactAnalysisService.buildTechnicalImpactPoints(getAnalysisSnapshots(period));
        }

        public List<TimelinePointDto> getPlatformEvolution(String period) {

                testScenarioService.ensureAnalysisSnapshots(period);
                return analysisTimelineService.buildPlatformEvolution(getAnalysisSnapshots(period));
        }

        /**
         * Persiste una captura real de análisis calculada a partir del estado
         * actual del dashboard.
         */
        public void saveAnalysisSnapshot(LocalDateTime collectedAt) {

                analysisSnapshotRepository.save(snapshotBuilder.buildAnalysisSnapshot(collectedAt, false));
        }
}

