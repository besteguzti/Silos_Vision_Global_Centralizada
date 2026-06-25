package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.AnalyticsComparePoint;
import com.tfg.dashboard.dto.OperationalImpactAnalysisResponse;
import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Fachada del módulo de análisis transversal.
 *
 * Mantiene estable el contrato usado por los controladores y delega la lógica
 * en servicios especializados de snapshots, relaciónes GLPI-plataforma,
 * degradación técnica e históricos.
 */
@Service
public class TransversalKpiAnalyticsService {

        private final AnalysisOrchestrator analysisOrchestrator;
        private final TransversalKpiHistoryService historyService;

        public TransversalKpiAnalyticsService(
                        AnalysisOrchestrator analysisOrchestrator,
                        TransversalKpiHistoryService historyService) {

                this.analysisOrchestrator = analysisOrchestrator;
                this.historyService = historyService;
        }

        /**
         * Recupera snapshots del panel de análisis para el periodo solicitado.
         */
        public List<AnalysisSnapshot> getAnalysisSnapshots(String period) {

                return analysisOrchestrator.getAnalysisSnapshots(period);
        }

        /**
         * Devuelve el analisis operativo principal del panel de analisis.
         */
        public OperationalImpactAnalysisResponse getGlpiPlatformRelation(String period) {

                return analysisOrchestrator.getGlpiPlatformRelation(period);
        }

        public List<AnalyticsComparePoint> getTechnicalDegradationImpact(String period) {

                return analysisOrchestrator.getTechnicalDegradationImpact(period);
        }

        public List<TimelinePointDto> getPlatformEvolution(String period) {

                return analysisOrchestrator.getPlatformEvolution(period);
        }

        /**
         * Guarda KPIs transversales en histórico.
         */
        public void saveCurrentSnapshot(LocalDateTime collectedAt) {

                historyService.saveCurrentSnapshot(collectedAt);
        }

        /**
         * Guarda el snapshot agregado que alimenta el panel de análisis.
         */
        public void saveAnalysisSnapshot(LocalDateTime collectedAt) {

                analysisOrchestrator.saveAnalysisSnapshot(collectedAt);
        }
}

