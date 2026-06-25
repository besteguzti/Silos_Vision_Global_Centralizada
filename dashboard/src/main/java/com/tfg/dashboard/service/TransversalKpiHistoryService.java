package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.model.TransversalKpiHistory;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;
import com.tfg.dashboard.repository.TransversalKpiHistoryRepository;

/**
 * Gestiona el histórico común de KPIs transversales.
 *
 * Calcula valores actuales a partir del dashboard y de los últimos snapshots de
 * plataforma, persiste solo datos disponibles y conserva las definiciones de
 * relación usadas por el análisis exploratorio.
 */
@Service
public class TransversalKpiHistoryService {

        public static final String GLOBAL_HEALTH = "global_health";
        public static final String GLOBAL_CRITICALITY = "global_criticality";
        public static final String GLOBAL_AVAILABILITY = "global_availability";
        public static final String USER_IMPACT = "user_impact";
        public static final String AFFECTED_SERVICES = "affected_services";
        public static final String TECHNICAL_DEGRADATION = "technical_degradation";
        public static final String OPERATIONAL_PRESSURE = "operational_pressure";
        public static final String OPERATIONAL_BACKLOG = "operational_backlog";
        public static final String SLA_RISK = "sla_risk";
        public static final String ENVIRONMENT_STABILITY = "environment_stability";
        public static final String OPERATIONAL_PRIORITY = "operational_priority";
        public static final String ARUBA_NETWORK_AFFECTATION = "aruba_network_affectation";
        public static final String ARUBA_NETWORK_DEGRADATION = "aruba_network_degradation";
        public static final String ARUBA_NETWORK_HEALTH = "aruba_network_health";
        public static final String GLPI_OPERATIONAL_PRESSURE = "glpi_operational_pressure";
        public static final String CITRIX_TECHNICAL_AFFECTION = "citrix_technical_affection";
        public static final String MICROSOFT365_TECHNICAL_AFFECTION = "microsoft365_technical_affection";

        private final MainDashboardService mainDashboardService;
        private final ArubaService arubaService;
        private final GlpiMetricsHistoryRepository glpiRepository;
        private final CitrixMetricsHistoryRepository citrixRepository;
        private final Microsoft365MetricsHistoryRepository microsoft365Repository;
        private final TransversalKpiHistoryRepository historyRepository;
        private final KpiProperties kpiProperties;

        public TransversalKpiHistoryService(
                        MainDashboardService mainDashboardService,
                        ArubaService arubaService,
                        GlpiMetricsHistoryRepository glpiRepository,
                        CitrixMetricsHistoryRepository citrixRepository,
                        Microsoft365MetricsHistoryRepository microsoft365Repository,
                        TransversalKpiHistoryRepository historyRepository,
                        KpiProperties kpiProperties) {

                this.mainDashboardService = mainDashboardService;
                this.arubaService = arubaService;
                this.glpiRepository = glpiRepository;
                this.citrixRepository = citrixRepository;
                this.microsoft365Repository = microsoft365Repository;
                this.historyRepository = historyRepository;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Calcula el mapa de valores transversales actuales.
         *
         * Un valor null significa que no hay dato de origen suficiente; no se
         * debe convertir automáticamente en 0 porque 0 también es una afección
         * real válida.
         */
        public Map<String, Double> calculateCurrentValues() {

                MainDashboardSummary summary = mainDashboardService.getSummary();
                ArubaNetworkStatusDto arubaNetworkStatus = arubaService.getNetworkStatus();
                Double arubaNetworkAffectation = arubaNetworkStatus == null
                                ? null
                                : (double) arubaNetworkStatus.getPercentage();

                Optional<GlpiMetricsHistory> glpiSnapshot = glpiRepository.findTopByOrderByCollectedAtDesc();
                Optional<CitrixMetricsHistory> citrixSnapshot = citrixRepository.findTopByOrderByCollectedAtDesc();
                Optional<Microsoft365MetricsHistory> microsoft365Snapshot =
                                microsoft365Repository.findTopByOrderByCollectedAtDesc();

                // Si no hay snapshot de una plataforma, no se inventa un 0:
                // 0 es un valor real valido de afección, mientras que null
                // indica que no hay datos suficientes para guardar histórico.
                Double glpiOperationalPressure =
                                glpiSnapshot.map(this::calculateGlpiOperationalPressure)
                                                .map(Integer::doubleValue)
                                                .orElse(null);
                Double citrixTechnicalAffection =
                                citrixSnapshot.map(this::calculateCitrixAffection)
                                                .map(Integer::doubleValue)
                                                .orElse(null);
                Double microsoft365TechnicalAffection =
                                microsoft365Snapshot.map(this::calculateMicrosoft365Affection)
                                                .map(Integer::doubleValue)
                                                .orElse(null);

                Map<String, Double> values = new LinkedHashMap<>();

                double globalHealthScore = clamp(100 - summary.getGlobalHealthPercentage());
                double criticality = summary.getGlobalCriticality();
                double availability = clamp(summary.getGlobalAvailability());
                double userImpact = summary.getUserImpact();
                double affectedServices = summary.getAffectedServicesPercent();
                double technicalDegradation = summary.getTechnicalDegradation();
                double backlogIndex = summary.getOperationalBacklog();
                double slaRisk = summary.getSlaRisk();
                double environmentStability = clamp(100 - technicalDegradation);
                double operationalPriority = clamp(
                                criticality * 0.35
                                                + slaRisk * 0.25
                                                + summary.getOperationalPressure() * 0.25
                                                + userImpact * 0.15);

                values.put(GLOBAL_HEALTH, globalHealthScore);
                values.put(GLOBAL_CRITICALITY, criticality);
                values.put(GLOBAL_AVAILABILITY, availability);
                values.put(USER_IMPACT, userImpact);
                values.put(AFFECTED_SERVICES, affectedServices);
                values.put(TECHNICAL_DEGRADATION, technicalDegradation);
                values.put(OPERATIONAL_PRESSURE, (double) summary.getOperationalPressure());
                values.put(OPERATIONAL_BACKLOG, backlogIndex);
                values.put(SLA_RISK, slaRisk);
                values.put(ENVIRONMENT_STABILITY, environmentStability);
                values.put(OPERATIONAL_PRIORITY, operationalPriority);
                values.put(ARUBA_NETWORK_AFFECTATION, arubaNetworkAffectation);
                values.put(ARUBA_NETWORK_DEGRADATION, arubaNetworkAffectation);
                values.put(
                                ARUBA_NETWORK_HEALTH,
                                arubaNetworkAffectation == null
                                                ? null
                                                : 100 - arubaNetworkAffectation);
                values.put(GLPI_OPERATIONAL_PRESSURE, glpiOperationalPressure);
                values.put(CITRIX_TECHNICAL_AFFECTION, citrixTechnicalAffection);
                values.put(MICROSOFT365_TECHNICAL_AFFECTION, microsoft365TechnicalAffection);

                return values;
        }

        /**
         * Persiste un snapshot de KPIs transversales, omitiendo valores nulos
         * para no falsear el histórico.
         */
        public void saveCurrentSnapshot(LocalDateTime collectedAt) {

                Map<String, Double> values = calculateCurrentValues();
                List<TransversalKpiHistory> histories = new ArrayList<>();

                for (KpiDefinition definition : definitions().values()) {

                        TransversalKpiHistory history = new TransversalKpiHistory();

                        history.setKpiCode(definition.code());
                        history.setKpiName(definition.name());
                        history.setUnit(definition.unit());
                        Double value = values.get(definition.code());

                        // Evita persistir muestras que parezcan 0% cuando en
                        // realidad no existe dato de origen para esa captura.
                        if (value == null) {
                                continue;
                        }

                        history.setValue(value);
                        history.setCollectedAt(collectedAt);

                        histories.add(history);
                }

                historyRepository.saveAll(histories);
        }

        /**
         * Devuelve definiciones internas de KPIs transversales y relaciónes
         * recomendadas para el módulo de análisis.
         */
        public Map<String, KpiDefinition> definitions() {

                Map<String, KpiDefinition> definitions = new LinkedHashMap<>();

                definitions.put(GLOBAL_HEALTH, new KpiDefinition(
                                GLOBAL_HEALTH,
                                "Salud global",
                                "Indice numerico derivado del estado global del dashboard.",
                                "%",
                                List.of(
                                                GLOBAL_CRITICALITY,
                                                USER_IMPACT,
                                                AFFECTED_SERVICES,
                                                ENVIRONMENT_STABILITY,
                                                GLOBAL_AVAILABILITY,
                                                ARUBA_NETWORK_HEALTH,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(GLOBAL_CRITICALITY, new KpiDefinition(
                                GLOBAL_CRITICALITY,
                                "Criticidad global",
                                "Nivel agregado de riesgo operativo observado.",
                                "indice 0-100",
                                List.of(
                                                GLOBAL_HEALTH,
                                                AFFECTED_SERVICES,
                                                OPERATIONAL_PRIORITY,
                                                ENVIRONMENT_STABILITY,
                                                ARUBA_NETWORK_AFFECTATION,
                                                ARUBA_NETWORK_DEGRADATION)));

                definitions.put(GLOBAL_AVAILABILITY, new KpiDefinition(
                                GLOBAL_AVAILABILITY,
                                "Disponibilidad global",
                                "Estimacion agregada de disponibilidad del entorno.",
                                "%",
                                List.of(
                                                USER_IMPACT,
                                                GLOBAL_HEALTH,
                                                AFFECTED_SERVICES,
                                                ARUBA_NETWORK_HEALTH)));

                definitions.put(USER_IMPACT, new KpiDefinition(
                                USER_IMPACT,
                                "Impacto en usuarios",
                                "Impacto relativo estimado a partir de la actividad agregada.",
                                "%",
                                List.of(
                                                GLOBAL_HEALTH,
                                                GLOBAL_AVAILABILITY,
                                                TECHNICAL_DEGRADATION,
                                                OPERATIONAL_PRIORITY,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(AFFECTED_SERVICES, new KpiDefinition(
                                AFFECTED_SERVICES,
                                "Servicios afectados",
                                "Porcentaje de plataformas monitorizadas con alerta.",
                                "%",
                                List.of(
                                                GLOBAL_CRITICALITY,
                                                GLOBAL_HEALTH,
                                                GLOBAL_AVAILABILITY,
                                                TECHNICAL_DEGRADATION)));

                definitions.put(TECHNICAL_DEGRADATION, new KpiDefinition(
                                TECHNICAL_DEGRADATION,
                                "Degradación técnica",
                                "Peso de elementos tecnicos que requieren actuacion.",
                                "indice 0-100",
                                List.of(
                                                OPERATIONAL_PRESSURE,
                                                USER_IMPACT,
                                                AFFECTED_SERVICES,
                                                GLOBAL_CRITICALITY,
                                                ARUBA_NETWORK_DEGRADATION)));

                definitions.put(OPERATIONAL_PRESSURE, new KpiDefinition(
                                OPERATIONAL_PRESSURE,
                                "Presión operativa",
                                "Presión agregada sobre recursos y operación.",
                                "indice 0-100",
                                List.of(
                                                TECHNICAL_DEGRADATION,
                                                OPERATIONAL_BACKLOG,
                                                SLA_RISK,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(OPERATIONAL_BACKLOG, new KpiDefinition(
                                OPERATIONAL_BACKLOG,
                                "Backlog operativo",
                                "Indice normalizado del volumen de trabajo GLPI pendiente.",
                                "indice 0-100",
                                List.of(
                                                SLA_RISK,
                                                OPERATIONAL_PRESSURE,
                                                OPERATIONAL_PRIORITY)));

                definitions.put(SLA_RISK, new KpiDefinition(
                                SLA_RISK,
                                "Riesgo de SLA",
                                "Riesgo derivado de tickets con SLA vencido.",
                                "indice 0-100",
                                List.of(
                                                OPERATIONAL_BACKLOG,
                                                OPERATIONAL_PRESSURE,
                                                OPERATIONAL_PRIORITY)));

                definitions.put(ENVIRONMENT_STABILITY, new KpiDefinition(
                                ENVIRONMENT_STABILITY,
                                "Estabilidad del entorno",
                                "Indicador inverso al riesgo y alertas activas.",
                                "%",
                                List.of(
                                                GLOBAL_HEALTH,
                                                GLOBAL_CRITICALITY,
                                                GLOBAL_AVAILABILITY,
                                                ARUBA_NETWORK_HEALTH)));

                definitions.put(OPERATIONAL_PRIORITY, new KpiDefinition(
                                OPERATIONAL_PRIORITY,
                                "Prioridad operativa",
                                "Prioridad de actuacion segun acciones, seguridad y SLA.",
                                "indice 0-100",
                                List.of(
                                                GLOBAL_CRITICALITY,
                                                USER_IMPACT,
                                                OPERATIONAL_BACKLOG,
                                                SLA_RISK,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(ARUBA_NETWORK_AFFECTATION, new KpiDefinition(
                                ARUBA_NETWORK_AFFECTATION,
                                "Afectacion de red Aruba",
                                "Porcentaje normalizado de riesgo o afectacion de la red Aruba.",
                                "%",
                                List.of(
                                                OPERATIONAL_PRESSURE,
                                                GLOBAL_CRITICALITY,
                                                USER_IMPACT,
                                                GLOBAL_HEALTH,
                                                ARUBA_NETWORK_HEALTH,
                                                ARUBA_NETWORK_DEGRADATION)));

                definitions.put(ARUBA_NETWORK_DEGRADATION, new KpiDefinition(
                                ARUBA_NETWORK_DEGRADATION,
                                "Degradación de red Aruba",
                                "Indice de degradación técnica especifico de Aruba.",
                                "indice 0-100",
                                List.of(
                                                TECHNICAL_DEGRADATION,
                                                OPERATIONAL_PRESSURE,
                                                GLOBAL_CRITICALITY,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(ARUBA_NETWORK_HEALTH, new KpiDefinition(
                                ARUBA_NETWORK_HEALTH,
                                "Salud de red Aruba",
                                "Indicador inverso a la afectacion de red Aruba.",
                                "%",
                                List.of(
                                                GLOBAL_HEALTH,
                                                ENVIRONMENT_STABILITY,
                                                GLOBAL_AVAILABILITY,
                                                ARUBA_NETWORK_AFFECTATION)));

                definitions.put(GLPI_OPERATIONAL_PRESSURE, new KpiDefinition(
                                GLPI_OPERATIONAL_PRESSURE,
                                "Presión operativa GLPI",
                                "Afección operativa derivada de tickets abiertos, críticos y capacidad de cierre.",
                                "%",
                                List.of(
                                                ARUBA_NETWORK_AFFECTATION,
                                                CITRIX_TECHNICAL_AFFECTION,
                                                MICROSOFT365_TECHNICAL_AFFECTION)));

                definitions.put(CITRIX_TECHNICAL_AFFECTION, new KpiDefinition(
                                CITRIX_TECHNICAL_AFFECTION,
                                "Afección técnica Citrix",
                                "Indice de salud Citrix expresado como porcentaje de afección.",
                                "%",
                                List.of(GLPI_OPERATIONAL_PRESSURE)));

                definitions.put(MICROSOFT365_TECHNICAL_AFFECTION, new KpiDefinition(
                                MICROSOFT365_TECHNICAL_AFFECTION,
                                "Afección técnica Microsoft 365",
                                "Indice de salud Microsoft 365 expresado como porcentaje de afección.",
                                "%",
                                List.of(GLPI_OPERATIONAL_PRESSURE)));

                return definitions;
        }

        /**
         * Calcula la presión operativa GLPI con pesos configurados.
         */
        public int calculateGlpiOperationalPressure(GlpiMetricsHistory glpi) {

                KpiProperties.GlpiPressureWeights weights =
                                kpiProperties.getWeights().getGlpiOperationalPressure();

                return clampToInt(
                                openTicketsScore(glpi) * weights.getOpenTickets()
                                                + closedPercentageScore(
                                                                glpi.getCreatedToday(),
                                                                glpi.getClosedToday()) * weights.getClosedTodayPercent()
                                                + criticalTicketsScore(glpi) * weights.getCriticalTickets()
                                                + closedPercentageScore(
                                                                glpi.getCreatedThisWeek(),
                                                                glpi.getClosedThisWeek()) * weights.getClosedWeekPercent());
        }

        /**
         * Normaliza Citrix como afección técnica para análisis transversal.
         */
        public int calculateCitrixAffection(CitrixMetricsHistory citrix) {

                CitrixAffectationCalculator.Result result = CitrixAffectationCalculator.calculate(
                                new CitrixAffectationCalculator.Input(
                                                citrix.getActiveSessions(),
                                                citrix.getActiveLicenses(),
                                                citrix.getAvailableDeliveryControllers(),
                                                citrix.getTotalDeliveryControllers(),
                                                citrix.getDisconnectedSessions(),
                                                citrix.getAverageLogonDurationSeconds(),
                                                citrix.getServerLoadPercent(),
                                                citrix.getFailedLogons(),
                                                0),
                                kpiProperties);

                return result.percentage();
        }

        /**
         * Normaliza Microsoft 365 como afección técnica para análisis
         * transversal.
         */
        public int calculateMicrosoft365Affection(Microsoft365MetricsHistory microsoft365) {

                Microsoft365AffectationCalculator.Result result =
                                Microsoft365AffectationCalculator.calculate(
                                                new Microsoft365AffectationCalculator.Input(
                                                                microsoft365.getActiveUsers(),
                                                                microsoft365.getUnassignedLicenses(),
                                                                microsoft365.getOutlookStatus(),
                                                                microsoft365.getTeamsStatus(),
                                                                microsoft365.getSharePointStatus(),
                                                                microsoft365.getNearlyFullMailboxes(),
                                                                microsoft365.getEmailsQuarantined(),
                                                                microsoft365.getSharePointStoragePercent(),
                                                                microsoft365.getRiskyUsers(),
                                                                microsoft365.getFailedSignIns(),
                                                                microsoft365.getUsersWithoutMfa(),
                                                                microsoft365.getAppsSecretsExpiringSoon(),
                                                                microsoft365.getUnusedApplications(),
                                                                microsoft365.getHighPrivilegeApplications(),
                                                                microsoft365.getNonCompliantDevices(),
                                                                0,
                                                                microsoft365.getOutdatedWindowsDevices(),
                                                                microsoft365.getDevicesWithoutEncryption(),
                                                                microsoft365.getStaleDevices()),
                                                kpiProperties);

                return result.percentage();
        }

        private int openTicketsScore(GlpiMetricsHistory glpi) {

                if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsRedMin()) {
                        return redScore();
                }

                if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsYellowMin()) {
                        return yellowScore();
                }

                return greenScore();
        }

        private int criticalTicketsScore(GlpiMetricsHistory glpi) {

                if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsRedAbove()) {
                        return redScore();
                }

                if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsYellowAbove()) {
                        return yellowScore();
                }

                return greenScore();
        }

        private int closedPercentageScore(int created,int closed) {

                if (created <= 0) {
                        return greenScore();
                }

                return closed * 100 / created >= kpiProperties.getGlpi().getClosedPercentGreenMin()
                                ? greenScore()
                                : yellowScore();
        }

        private int clampToInt(double value) {

                return (int) Math.round(clamp(value));
        }

        private double clamp(double value) {

                if (value < 0) {
                        return 0;
                }

                if (value > kpiProperties.getStatus().getMax()) {
                        return kpiProperties.getStatus().getMax();
                }

                return value;
        }

        private int greenScore() {

                return kpiProperties.getAffection().getGreen();
        }

        private int yellowScore() {

                return kpiProperties.getAffection().getYellow();
        }

        private int redScore() {

                return kpiProperties.getAffection().getRed();
        }

        public record KpiDefinition(
                        String code,
                        String name,
                        String description,
                        String unit,
                        List<String> relatedKpis) {
        }
}

