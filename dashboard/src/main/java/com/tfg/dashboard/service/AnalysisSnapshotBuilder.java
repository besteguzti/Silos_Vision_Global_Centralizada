package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

/**
 * Construye objetos AnalysisSnapshot a partir del estado actual del dashboard.
 */
@Service
public class AnalysisSnapshotBuilder {

    private final TransversalKpiHistoryService historyService;
    private final MainDashboardService mainDashboardService;
    private final KpiScoringService kpiScoringService;
    private final ArubaService arubaService;
    private final CitrixMetricsHistoryRepository citrixRepository;
    private final Microsoft365MetricsHistoryRepository microsoft365Repository;
    private final GlpiMetricsHistoryRepository glpiRepository;
    private final KpiProperties kpiProperties;

    public AnalysisSnapshotBuilder(
                    TransversalKpiHistoryService historyService,
                    MainDashboardService mainDashboardService,
                    KpiScoringService kpiScoringService,
                    ArubaService arubaService,
                    CitrixMetricsHistoryRepository citrixRepository,
                    Microsoft365MetricsHistoryRepository microsoft365Repository,
                    GlpiMetricsHistoryRepository glpiRepository,
                    KpiProperties kpiProperties) {

        this.historyService = historyService;
        this.mainDashboardService = mainDashboardService;
        this.kpiScoringService = kpiScoringService;
        this.arubaService = arubaService;
        this.citrixRepository = citrixRepository;
        this.microsoft365Repository = microsoft365Repository;
        this.glpiRepository = glpiRepository;
        this.kpiProperties = kpiProperties;
    }

    public AnalysisSnapshot buildAnalysisSnapshot(LocalDateTime collectedAt, boolean generatedScenario) {

        Map<String, Double> values = historyService.calculateCurrentValues();
        MainDashboardSummary summary = mainDashboardService.getSummary();
        ArubaSummary arubaSummary = arubaService.getSummary();
        CitrixMetricsHistory citrixSnapshot =
                        citrixRepository.findTopByOrderByCollectedAtDesc().orElse(null);
        Microsoft365MetricsHistory microsoft365Snapshot =
                        microsoft365Repository.findTopByOrderByCollectedAtDesc().orElse(null);
        GlpiMetricsHistory glpiSnapshot =
                        glpiRepository.findTopByOrderByCollectedAtDesc().orElse(null);
        AnalysisSnapshot snapshot = new AnalysisSnapshot();

        int aruba = safeCurrentScore(values, TransversalKpiHistoryService.ARUBA_NETWORK_AFFECTATION);
        int citrix = safeCurrentScore(values, TransversalKpiHistoryService.CITRIX_TECHNICAL_AFFECTION);
        int microsoft365 = safeCurrentScore(values,
                        TransversalKpiHistoryService.MICROSOFT365_TECHNICAL_AFFECTION);
        int glpi = kpiScoringService.clampToInt(
                        safeCurrentScore(
                                        values,
                                        TransversalKpiHistoryService.GLPI_OPERATIONAL_PRESSURE));

        snapshot.setTimestamp(collectedAt);
        /*
         * La entidad mantiene sufijo Health por compatibilidad historica con la
         * tabla analysis_snapshots. Los valores guardados son afecciones
         * normalizadas: mayor valor implica peor estado.
         */
        snapshot.setArubaHealth(aruba);
        snapshot.setCitrixHealth(citrix);
        snapshot.setMicrosoft365Health(microsoft365);
        snapshot.setGlpiHealth(glpi);
        snapshot.setGlpiOperationalPressure(
                        safeCurrentScore(
                                        values,
                                        TransversalKpiHistoryService.GLPI_OPERATIONAL_PRESSURE));
        snapshot.setTechnicalDegradation(summary.getTechnicalDegradation());
        snapshot.setUserImpact(summary.getUserImpact());
        snapshot.setGlobalStatus(summary.getGlobalHealthPercentage());
        snapshot.setArubaWifiClients(
                        "NO_DATA".equalsIgnoreCase(arubaSummary.getDataStatus())
                                        ? null
                                        : arubaSummary.getTotalWifiClients());
        snapshot.setArubaInactiveAps(
                        "NO_DATA".equalsIgnoreCase(arubaSummary.getDataStatus())
                                        ? null
                                        : arubaSummary.getInactiveAps());
        snapshot.setArubaDownSwitches(
                        "NO_DATA".equalsIgnoreCase(arubaSummary.getDataStatus())
                                        ? null
                                        : arubaSummary.getDownSwitches());
        snapshot.setArubaDownAps(
                        "NO_DATA".equalsIgnoreCase(arubaSummary.getDataStatus())
                                        ? null
                                        : arubaSummary.getDownAps());
        snapshot.setCitrixAverageLogonDurationSeconds(
                        citrixSnapshot == null
                                        ? null
                                        : citrixSnapshot.getAverageLogonDurationSeconds());
        snapshot.setCitrixActiveSessions(
                        citrixSnapshot == null ? null : citrixSnapshot.getActiveSessions());
        snapshot.setCitrixAvailableDeliveryControllers(
                        citrixSnapshot == null
                                        ? null
                                        : citrixSnapshot.getAvailableDeliveryControllers());
        snapshot.setCitrixServerLoadPercent(
                        citrixSnapshot == null ? null : citrixSnapshot.getServerLoadPercent());
        snapshot.setCitrixFailedLogons(
                        citrixSnapshot == null ? null : citrixSnapshot.getFailedLogons());
        snapshot.setGlpiOpenTickets(
                        glpiSnapshot == null ? null : glpiSnapshot.getOpenTickets());
        snapshot.setGlpiCreatedToday(
                        glpiSnapshot == null ? null : glpiSnapshot.getCreatedToday());
        snapshot.setGlpiClosedToday(
                        glpiSnapshot == null ? null : glpiSnapshot.getClosedToday());
        snapshot.setGlpiCreatedThisWeek(
                        glpiSnapshot == null ? null : glpiSnapshot.getCreatedThisWeek());
        snapshot.setGlpiClosedThisWeek(
                        glpiSnapshot == null ? null : glpiSnapshot.getClosedThisWeek());
        snapshot.setGlpiOperationalBacklog(
                        glpiSnapshot == null ? null : glpiSnapshot.getOperationalBacklog());
        snapshot.setArubaOpenTickets(
                        glpiSnapshot == null ? null : glpiSnapshot.getArubaOpenTicketsRaw());
        snapshot.setCitrixOpenTickets(
                        glpiSnapshot == null ? null : glpiSnapshot.getCitrixOpenTicketsRaw());
        snapshot.setMicrosoft365OpenTickets(
                        glpiSnapshot == null ? null : glpiSnapshot.getMicrosoft365OpenTicketsRaw());
        snapshot.setMicrosoft365ActiveUsers(
                        microsoft365Snapshot == null
                                        ? null
                                        : microsoft365Snapshot.getActiveUsers());
        snapshot.setMicrosoft365NonCompliantDevices(
                        microsoft365Snapshot == null
                                        ? null
                                        : microsoft365Snapshot.getNonCompliantDevices());
        snapshot.setMicrosoft365UsersWithoutMfa(
                        microsoft365Snapshot == null
                                        ? null
                                        : microsoft365Snapshot.getUsersWithoutMfa());
        snapshot.setMicrosoft365FailedSignIns(
                        microsoft365Snapshot == null
                                        ? null
                                        : microsoft365Snapshot.getFailedSignIns());
        snapshot.setAffectedServicesPercent(summary.getAffectedServicesPercent());
        snapshot.setArubaStatus(kpiScoringService.statusFromAffection(aruba));
        snapshot.setCitrixStatus(kpiScoringService.statusFromAffection(citrix));
        snapshot.setMicrosoft365Status(kpiScoringService.statusFromAffection(microsoft365));
        snapshot.setGlpiStatus(kpiScoringService.statusFromAffection(glpi));
        snapshot.setGeneratedScenario(generatedScenario);

        return snapshot;
    }

    private int safeCurrentScore(Map<String, Double> values, String code) {

        Double value = values.get(code);

        return value != null
                        ? kpiScoringService.clampToInt(value)
                        : minimumAffection();
    }

    private int minimumAffection() {

        return kpiProperties.getAffection().getGreen();
    }
}
