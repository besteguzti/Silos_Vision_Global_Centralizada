package com.tfg.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;

/**
 * Gestiona escenarios de prueba del panel de análisis.
 *
 * Solo genera snapshots cuando no hay histórico real suficiente y los marca con
 * generatedScenario para que el frontend pueda advertir que son datos de prueba.
 */

@Service
public class AnalysisTestScenarioService {

        private static final int MINIMUM_REAL_SNAPSHOTS = 2;
        private static final int TEST_SNAPSHOTS_PER_DAY = 4;
        private static final int TEST_BUCKET_HOURS = 6;
        private static final double TEST_WAVE_STEP = 0.9;
        private static final double TEST_WAVE_AMPLITUDE = 24;
        private static final double TEST_CITRIX_WAVE_FACTOR = 0.7;
        private static final double TEST_MICROSOFT365_WAVE_FACTOR = 0.45;
        private static final double TEST_GLPI_PLATFORM_INFLUENCE = 0.18;
        private static final double TEST_GLPI_WAVE_FACTOR = 0.35;
        private static final int TEST_BASE_WIFI_CLIENTS = 230;
        private static final int TEST_BASE_CITRIX_SESSIONS = 460;
        private static final int TEST_BASE_MICROSOFT365_ACTIVE_USERS = 1200;
        private static final int TEST_MICROSOFT365_USER_LOSS_FACTOR = 3;
        private static final int TEST_AVAILABLE_DELIVERY_CONTROLLERS = 4;
        private static final int TEST_BASE_NON_COMPLIANT_DEVICES = 20;
        private static final int TEST_LOGON_BASE_SECONDS = 12;
        private static final int TEST_SERVER_LOAD_BASE_PERCENT = 45;
        private static final int TEST_ARUBA_CLIENT_LOSS_FACTOR = 2;
        private static final int TEST_CITRIX_SESSION_LOSS_FACTOR = 2;
        private static final int TEST_ARUBA_DOWN_APS_DIVISOR = 20;
        private static final int TEST_CITRIX_DELIVERY_CONTROLLER_AFFECTION_DIVISOR = 34;
        private static final int TEST_LOGON_AFFECTION_DIVISOR = 2;
        private static final int TEST_FAILED_LOGON_CITRIX_DIVISOR = 4;
        private static final int TEST_FAILED_LOGON_GLPI_DIVISOR = 10;
        private static final int TEST_ARUBA_INACTIVE_APS_DIVISOR = 5;
        private static final int TEST_ARUBA_DOWN_SWITCHES_DIVISOR = 20;
        private static final int TEST_MFA_USERS_DIVISOR = 10;
        private static final int TEST_M365_FAILED_SIGN_INS_DIVISOR = 3;
        private static final int TEST_ARUBA_TICKET_BASE = 15;
        private static final int TEST_CITRIX_TICKET_BASE = 25;
        private static final int TEST_MICROSOFT365_TICKET_BASE = 12;
        private static final int TEST_ARUBA_TICKET_DIVISOR = 2;
        private static final int TEST_CITRIX_TICKET_DIVISOR = 1;
        private static final int TEST_MICROSOFT365_TICKET_DIVISOR = 2;

        private final AnalysisSnapshotRepository analysisSnapshotRepository;
        private final AnalysisSnapshotService analysisSnapshotService;
        private final TransversalKpiHistoryService historyService;
        private final KpiScoringService kpiScoringService;
        private final ImpactAnalysisService impactAnalysisService;
        private final KpiProperties kpiProperties;

        public AnalysisTestScenarioService(
                        AnalysisSnapshotRepository analysisSnapshotRepository,
                        AnalysisSnapshotService analysisSnapshotService,
                        TransversalKpiHistoryService historyService,
                        KpiScoringService kpiScoringService,
                        ImpactAnalysisService impactAnalysisService,
                        KpiProperties kpiProperties) {

                this.analysisSnapshotRepository = analysisSnapshotRepository;
                this.analysisSnapshotService = analysisSnapshotService;
                this.historyService = historyService;
                this.kpiScoringService = kpiScoringService;
                this.impactAnalysisService = impactAnalysisService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Comprueba si hay suficientes snapshots reales para el periodo; si no,
         * persiste un escenario de prueba controlado.
         */
        public void ensureAnalysisSnapshots(String period) {

                int days = analysisSnapshotService.daysFromPeriod(period);
                LocalDateTime since = LocalDateTime.now().minusDays(days);
                long daysWithUsableSnapshots = analysisSnapshotService.getSnapshots(period)
                                .stream()
                                .filter(snapshot -> snapshot.getTimestamp() != null)
                                .filter(this::hasSpecificRelationData)
                                .map(snapshot -> snapshot.getTimestamp().toLocalDate())
                                .distinct()
                                .count();

                if (analysisSnapshotRepository.countByTimestampAfter(since) >= MINIMUM_REAL_SNAPSHOTS
                                && daysWithUsableSnapshots >= days) {
                        return;
                }

                persistScenarioSnapshots(period);
        }

        /**
         * Genera una serie coherente de snapshots de prueba a partir de los valores
         * actuales disponibles.
         */
        private void persistScenarioSnapshots(String period) {

                Map<String, Double> values = historyService.calculateCurrentValues();
                int days = analysisSnapshotService.daysFromPeriod(period);

                for (int dayIndex = days - 1; dayIndex >= 0; dayIndex--) {
                        for (int bucket = 0; bucket < TEST_SNAPSHOTS_PER_DAY; bucket++) {

                        int sequence = dayIndex * TEST_SNAPSHOTS_PER_DAY + bucket;
                        double wave = Math.sin(sequence * TEST_WAVE_STEP) * TEST_WAVE_AMPLITUDE;
                        AnalysisSnapshot snapshot = new AnalysisSnapshot();

                        int aruba = kpiScoringService.clampToInt(
                                        baseValue(values, TransversalKpiHistoryService.ARUBA_NETWORK_AFFECTATION)
                                                        + wave);
                        int citrix = kpiScoringService.clampToInt(
                                        baseValue(values, TransversalKpiHistoryService.CITRIX_TECHNICAL_AFFECTION)
                                                        + wave * TEST_CITRIX_WAVE_FACTOR);
                        int microsoft365 = kpiScoringService.clampToInt(
                                        baseValue(
                                                        values,
                                                        TransversalKpiHistoryService.MICROSOFT365_TECHNICAL_AFFECTION)
                                                        + wave * TEST_MICROSOFT365_WAVE_FACTOR);
                        int glpiPressure = kpiScoringService.clampToInt(
                                        baseValue(values, TransversalKpiHistoryService.GLPI_OPERATIONAL_PRESSURE)
                                                        + Math.max(aruba, citrix) * TEST_GLPI_PLATFORM_INFLUENCE
                                                        + wave * TEST_GLPI_WAVE_FACTOR);
                        int technicalDegradation =
                                        impactAnalysisService.calculateTechnicalDegradation(
                                                        aruba,
                                                        citrix,
                                                        microsoft365);
                        int userImpact =
                                        impactAnalysisService.calculateUserImpact(
                                                        aruba,
                                                        citrix,
                                                        microsoft365,
                                                        glpiPressure);
                        KpiProperties.PlatformWeights globalWeights =
                                        kpiProperties.getWeights().getGlobalStatus();

                        int global = kpiScoringService.clampToInt(
                                        aruba * globalWeights.getAruba()
                                                        + citrix * globalWeights.getCitrix()
                                                        + microsoft365 * globalWeights.getMicrosoft365()
                                                        + glpiPressure * globalWeights.getGlpi());
                        int affectedServicesPercent = affectedServicesPercent(
                                        aruba,
                                        citrix,
                                        microsoft365,
                                        glpiPressure);
                        int arubaOpenTickets = TEST_ARUBA_TICKET_BASE + aruba / TEST_ARUBA_TICKET_DIVISOR;
                        int citrixOpenTickets = TEST_CITRIX_TICKET_BASE + citrix / TEST_CITRIX_TICKET_DIVISOR;
                        int microsoft365OpenTickets =
                                        TEST_MICROSOFT365_TICKET_BASE
                                                        + microsoft365 / TEST_MICROSOFT365_TICKET_DIVISOR;
                        int glpiOpenTickets =
                                        arubaOpenTickets
                                                        + citrixOpenTickets
                                                        + microsoft365OpenTickets;
                        int glpiCreatedToday =
                                        Math.max(0, 20 + glpiPressure / 4);
                        int glpiClosedToday =
                                        Math.max(0, glpiCreatedToday - Math.max(0, glpiPressure / 12));
                        int glpiCreatedThisWeek =
                                        Math.max(glpiCreatedToday, 120 + glpiOpenTickets);
                        int glpiClosedThisWeek =
                                        Math.max(0, glpiCreatedThisWeek - glpiOpenTickets);

                        snapshot.setTimestamp(
                                        LocalDate.now()
                                                        .minusDays(dayIndex)
                                                        .atStartOfDay()
                                                        .plusHours((long) bucket * TEST_BUCKET_HOURS));
                        snapshot.setArubaHealth(aruba);
                        snapshot.setCitrixHealth(citrix);
                        snapshot.setMicrosoft365Health(microsoft365);
                        snapshot.setGlpiHealth(glpiPressure);
                        snapshot.setGlpiOperationalPressure(glpiPressure);
                        snapshot.setTechnicalDegradation(technicalDegradation);
                        snapshot.setUserImpact(userImpact);
                        snapshot.setGlobalStatus(global);
                        int arubaWifiClients = Math.max(
                                        0,
                                        TEST_BASE_WIFI_CLIENTS - aruba * TEST_ARUBA_CLIENT_LOSS_FACTOR);
                        int citrixActiveSessions = Math.min(
                                        arubaWifiClients,
                                        Math.max(
                                                        0,
                                                        TEST_BASE_CITRIX_SESSIONS
                                                                        - aruba
                                                                                        * TEST_CITRIX_SESSION_LOSS_FACTOR));
                        int microsoft365ActiveUsers = Math.max(
                                        0,
                                        TEST_BASE_MICROSOFT365_ACTIVE_USERS
                                                        - aruba * TEST_MICROSOFT365_USER_LOSS_FACTOR);
                        int citrixAvailableDeliveryControllers = Math.max(
                                        0,
                                        TEST_AVAILABLE_DELIVERY_CONTROLLERS
                                                        - citrix / TEST_CITRIX_DELIVERY_CONTROLLER_AFFECTION_DIVISOR);

                        snapshot.setArubaWifiClients(arubaWifiClients);
                        snapshot.setArubaInactiveAps(aruba / TEST_ARUBA_INACTIVE_APS_DIVISOR);
                        snapshot.setArubaDownSwitches(aruba / TEST_ARUBA_DOWN_SWITCHES_DIVISOR);
                        snapshot.setArubaDownAps(aruba / TEST_ARUBA_DOWN_APS_DIVISOR);
                        snapshot.setCitrixAverageLogonDurationSeconds(
                                        TEST_LOGON_BASE_SECONDS + citrix / TEST_LOGON_AFFECTION_DIVISOR);
                        snapshot.setCitrixActiveSessions(citrixActiveSessions);
                        snapshot.setCitrixAvailableDeliveryControllers(citrixAvailableDeliveryControllers);
                        snapshot.setCitrixServerLoadPercent(kpiScoringService.clampToInt(
                                        TEST_SERVER_LOAD_BASE_PERCENT
                                                        + citrix / TEST_LOGON_AFFECTION_DIVISOR));
                        snapshot.setCitrixFailedLogons(Math.max(
                                        0,
                                        citrix / TEST_FAILED_LOGON_CITRIX_DIVISOR
                                                        + glpiPressure / TEST_FAILED_LOGON_GLPI_DIVISOR));
                        snapshot.setGlpiOpenTickets(glpiOpenTickets);
                        snapshot.setGlpiCreatedToday(glpiCreatedToday);
                        snapshot.setGlpiClosedToday(glpiClosedToday);
                        snapshot.setGlpiCreatedThisWeek(glpiCreatedThisWeek);
                        snapshot.setGlpiClosedThisWeek(glpiClosedThisWeek);
                        snapshot.setGlpiOperationalBacklog(glpiOpenTickets);
                        snapshot.setArubaOpenTickets(arubaOpenTickets);
                        snapshot.setCitrixOpenTickets(citrixOpenTickets);
                        snapshot.setMicrosoft365OpenTickets(microsoft365OpenTickets);
                        snapshot.setMicrosoft365ActiveUsers(microsoft365ActiveUsers);
                        snapshot.setMicrosoft365NonCompliantDevices(TEST_BASE_NON_COMPLIANT_DEVICES + microsoft365);
                        snapshot.setMicrosoft365UsersWithoutMfa(Math.min(
                                        10,
                                        Math.max(0, microsoft365 / TEST_MFA_USERS_DIVISOR)));
                        snapshot.setMicrosoft365FailedSignIns(Math.min(
                                        30,
                                        Math.max(0, microsoft365 / TEST_M365_FAILED_SIGN_INS_DIVISOR)));
                        snapshot.setAffectedServicesPercent(affectedServicesPercent);
                        snapshot.setArubaStatus(kpiScoringService.statusFromAffection(aruba));
                        snapshot.setCitrixStatus(kpiScoringService.statusFromAffection(citrix));
                        snapshot.setMicrosoft365Status(kpiScoringService.statusFromAffection(microsoft365));
                        snapshot.setGlpiStatus(kpiScoringService.statusFromAffection(glpiPressure));
                        snapshot.setGeneratedScenario(true);

                        analysisSnapshotRepository.save(snapshot);
                        }
                }
        }

        private double baseValue(Map<String, Double> values, String code) {

                Double value = values.get(code);

                // Los escenarios de prueba necesitan una base numerica para poder
                // dibujarse. Si falta el dato real, se parte del minimo de
                // afección y el snapshot queda marcado como generatedScenario.
                return value != null
                                ? value
                                : kpiProperties.getAffection().getGreen();
        }

        private boolean hasSpecificRelationData(AnalysisSnapshot snapshot) {

                return snapshot.getArubaHealth() != null
                                && snapshot.getArubaInactiveAps() != null
                                && snapshot.getArubaDownSwitches() != null
                                && snapshot.getArubaDownAps() != null
                                && snapshot.getCitrixAverageLogonDurationSeconds() != null
                                && snapshot.getCitrixAvailableDeliveryControllers() != null
                                && snapshot.getCitrixServerLoadPercent() != null
                                && snapshot.getArubaWifiClients() != null
                                && snapshot.getCitrixActiveSessions() != null
                                && snapshot.getCitrixFailedLogons() != null
                                && snapshot.getGlpiCreatedToday() != null
                                && snapshot.getGlpiClosedToday() != null
                                && snapshot.getGlpiCreatedThisWeek() != null
                                && snapshot.getGlpiClosedThisWeek() != null
                                && snapshot.getGlpiOperationalBacklog() != null
                                && snapshot.getCitrixOpenTickets() != null
                                && snapshot.getMicrosoft365NonCompliantDevices() != null
                                && snapshot.getMicrosoft365ActiveUsers() != null
                                && snapshot.getMicrosoft365UsersWithoutMfa() != null
                                && snapshot.getMicrosoft365FailedSignIns() != null
                                && snapshot.getMicrosoft365OpenTickets() != null
                                && snapshot.getAffectedServicesPercent() != null
                                && snapshot.getTechnicalDegradation() != null
                                && snapshot.getGlpiOperationalPressure() != null;
        }

        private int affectedServicesPercent(
                        int aruba,
                        int citrix,
                        int microsoft365,
                        int glpiPressure) {

                int affectedPlatforms = 0;
                int threshold = kpiProperties.getStatus().getYellowMin();

                if (aruba >= threshold) {
                        affectedPlatforms++;
                }

                if (citrix >= threshold) {
                        affectedPlatforms++;
                }

                if (microsoft365 >= threshold) {
                        affectedPlatforms++;
                }

                if (glpiPressure >= threshold) {
                        affectedPlatforms++;
                }

                return affectedPlatforms * (kpiProperties.getStatus().getMax() / 4);
        }

}

