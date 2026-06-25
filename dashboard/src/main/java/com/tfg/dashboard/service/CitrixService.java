package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.CitrixHealthStatusDto;
import com.tfg.dashboard.dto.CitrixIndicatorStatusDto;
import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.KpiStatus;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;

/**
 * Servicio de Citrix simulado.
 *
 * Genera métricas dinámicas, recupera el último snapshot persistido y calcula
 * el índice de salud Citrix con la misma escala de afección que el resto de
 * plataformas.
 */
@Service
public class CitrixService {

        private final Random random = new Random();
        private static final String GREEN = "GREEN";
        private static final String YELLOW = "YELLOW";
        private static final String RED = "RED";
        private final CitrixMetricsHistoryRepository metricsHistoryRepository;
        private final GlpiPlatformTicketService glpiPlatformTicketService;
        private final KpiProperties kpiProperties;

        public CitrixService(
                        CitrixMetricsHistoryRepository metricsHistoryRepository,
                        GlpiPlatformTicketService glpiPlatformTicketService,
                        KpiProperties kpiProperties) {
                this.metricsHistoryRepository = metricsHistoryRepository;
                this.glpiPlatformTicketService = glpiPlatformTicketService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Devuelve el último snapshot Citrix almacenado en MySQL o NO_DATA si
         * aún no existe histórico.
         */
        public CitrixSummary getSummary() {

                return metricsHistoryRepository.findTopByOrderByCollectedAtDesc().map(this::mapHistoryToSummary)
                                .orElseGet(this::noDataSummary);
        }

        /**
         * Genera un resumen simulado que posteriormente se guarda como snapshot.
         */
        public CitrixSummary generateSimulatedSummary() {

                // Crear DTO respuesta

                CitrixSummary summary = new CitrixSummary();

                int activeSessions = 250 + random.nextInt(200);
                int activeLicenses = 500 + random.nextInt(100);
                int totalDeliveryControllers = 4;
                int availableDeliveryControllers = 3 + random.nextInt(2);
                int disconnectedSessions = random.nextInt(40);
                int averageLogonDurationSeconds = 10 + random.nextInt(45);
                int serverLoadPercent = 40 + random.nextInt(55);
                int failedLogons = random.nextInt(15);

                summary.setActiveSessions(activeSessions);
                summary.setActiveLicenses(activeLicenses);
                summary.setAvailableDeliveryControllers(availableDeliveryControllers);
                summary.setTotalDeliveryControllers(totalDeliveryControllers);
                summary.setDisconnectedSessions(disconnectedSessions);
                summary.setAverageLogonDurationSeconds(averageLogonDurationSeconds);
                summary.setServerLoadPercent(serverLoadPercent);
                summary.setFailedLogons(failedLogons);
                summary.setCitrixOpenTickets(glpiPlatformTicketService.getCitrixOpenTickets());
                CitrixHealthStatusDto citrixHealthDetails = calculateCitrixHealthDetails(
                                activeSessions,
                                activeLicenses,
                                availableDeliveryControllers,
                                totalDeliveryControllers,
                                disconnectedSessions,
                                averageLogonDurationSeconds,
                                serverLoadPercent,
                                failedLogons,
                                summary.getCitrixOpenTickets());

                summary.setCitrixHealthDetails(citrixHealthDetails);
                summary.setCitrixHealthKpi(buildCitrixHealthKpi(citrixHealthDetails,LocalDateTime.now(),"SIMULATED"));

                return summary;
        }

        private CitrixSummary mapHistoryToSummary(CitrixMetricsHistory history) {

                CitrixSummary summary = new CitrixSummary();

                summary.setActiveSessions(history.getActiveSessions());
                summary.setActiveLicenses(history.getActiveLicenses());
                summary.setAvailableDeliveryControllers(history.getAvailableDeliveryControllers());
                summary.setTotalDeliveryControllers(history.getTotalDeliveryControllers());
                summary.setDisconnectedSessions(history.getDisconnectedSessions());
                summary.setAverageLogonDurationSeconds(history.getAverageLogonDurationSeconds());
                summary.setServerLoadPercent(history.getServerLoadPercent());
                summary.setFailedLogons(history.getFailedLogons());
                summary.setCitrixOpenTickets(glpiPlatformTicketService.getCitrixOpenTickets());
                CitrixHealthStatusDto citrixHealthDetails = calculateCitrixHealthDetails(
                                history.getActiveSessions(),
                                history.getActiveLicenses(),
                                history.getAvailableDeliveryControllers(),
                                history.getTotalDeliveryControllers(),
                                history.getDisconnectedSessions(),
                                history.getAverageLogonDurationSeconds(),
                                history.getServerLoadPercent(),
                                history.getFailedLogons(),
                                summary.getCitrixOpenTickets());

                summary.setCitrixHealthDetails(citrixHealthDetails);
                summary.setLastUpdated(history.getCollectedAt());
                summary.setDataStatus(calculateDataStatus(history.getCollectedAt()));
                summary.setCitrixHealthKpi(buildCitrixHealthKpi(citrixHealthDetails,history.getCollectedAt(),summary.getDataStatus()));

                return summary;
        }

        private CitrixHealthStatusDto calculateCitrixHealthDetails(
                        int activeSessions,
                        int activeLicenses,
                        int availableDeliveryControllers,
                        int totalDeliveryControllers,
                        int disconnectedSessions,
                        int averageLogonDurationSeconds,
                        int serverLoadPercent,
                        int failedLogons,
                        int citrixOpenTickets) {

                CitrixAffectationCalculator.Result result = CitrixAffectationCalculator.calculate(
                                new CitrixAffectationCalculator.Input(
                                                activeSessions,
                                                activeLicenses,
                                                availableDeliveryControllers,
                                                totalDeliveryControllers,
                                                disconnectedSessions,
                                                averageLogonDurationSeconds,
                                                serverLoadPercent,
                                                failedLogons,
                                                citrixOpenTickets),
                                kpiProperties);

                CitrixHealthStatusDto details = new CitrixHealthStatusDto();

                details.setPercentage(result.percentage());
                details.setColor(result.color());
                details.setIndicators(result.indicators());
                details.setReasons(result.reasons());
                details.setAffectedService(result.affectedService());
                details.setCriticalCondition(result.criticalCondition());
                details.setTechnicalDegradationValue(result.percentage());
                details.setTransversalReady(true);

                return details;
        }

        private String calculateDataStatus(
                        LocalDateTime collectedAt) {

                if (collectedAt == null) {
                        return "NO_DATA";
                }

                if (collectedAt.isAfter(LocalDateTime.now().minusMinutes(
                                kpiProperties.getFreshness().getCitrixMinutes()))) {
                        return "OK";
                }

                return "STALE";
        }

        private CitrixSummary noDataSummary() {
                CitrixSummary summary = new CitrixSummary();
                summary.setCitrixHealthDetails(noDataCitrixHealthDetails());
                summary.setDataStatus("NO_DATA");
                summary.setLastUpdated(null);
                return summary;
        }

        private KpiResultDto buildCitrixHealthKpi(CitrixHealthStatusDto details,LocalDateTime timestamp,String freshness) {

                return new KpiResultDto(
                                "citrix_health",
                                "Índice de salud Citrix",
                                details.getPercentage(),
                                KpiStatus.from(details.getColor()),
                                "Afección normalizada del entorno Citrix.",
                                "Suma de afecciones parciales de los indicadores Citrix.",
                                timestamp,
                                freshness,
                                details.getPercentage(),
                                details.getIndicators().stream()
                                                .map(indicator -> new KpiResultDto(
                                                                indicatorId(indicator.getName()),
                                                                indicator.getName(),
                                                                indicator.getAffectionPercent(),
                                                                KpiStatus.from(indicator.getColor()),
                                                                indicator.getReason(),
                                                                null,
                                                                timestamp,
                                                                freshness,
                                                                indicator.getAffectionPercent(),
                                                                List.of()))
                                                .toList());
        }

        private String indicatorId(String name) {

                return name.toLowerCase().replace(" ", "_").replace("%", "percent");
        }

        private CitrixHealthStatusDto noDataCitrixHealthDetails() {

                CitrixIndicatorStatusDto noData = new CitrixIndicatorStatusDto();
                noData.setName("Datos Citrix");
                noData.setColor(RED);
                noData.setAffectionPercent(kpiProperties.getAffection().getRed());
                noData.setReason("No hay snapshot Citrix disponible");

                CitrixHealthStatusDto details = new CitrixHealthStatusDto();

                details.setPercentage(kpiProperties.getAffection().getRed());
                details.setColor(RED);
                details.setIndicators(List.of(noData));
                details.setReasons(List.of(noData.getReason()));
                details.setAffectedService(true);
                details.setCriticalCondition(true);
                details.setTechnicalDegradationValue(kpiProperties.getAffection().getRed());
                details.setTransversalReady(true);

                return details;
        }

}

