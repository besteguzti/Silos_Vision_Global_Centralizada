package com.tfg.dashboard.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ExecutiveSummaryDto;
import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.PlatformFindingDto;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.dto.summary.MainDashboardSummary;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

/**
 * Construye el diagnóstico operativo del dashboard principal.
 *
 * Resume qué servicio está afectado, qué plataforma contribuye más al estado
 * global, qué prioridad tiene la situación y qué primera acción revisar. Es una
 * lectura heurística para orientar la operación, no un motor de causa raíz.
 */
@Service
public class ExecutiveSummaryService {

        private static final String NOT_ESTIMABLE = "No estimable con los datos actuales";
        private final MainDashboardService mainDashboardService;
        private final ArubaService arubaService;
        private final CitrixMetricsHistoryRepository citrixRepository;
        private final Microsoft365MetricsHistoryRepository microsoft365Repository;
        private final GlpiMetricsHistoryRepository glpiRepository;
        private final AnalysisSnapshotRepository analysisSnapshotRepository;
        private final KpiProperties kpiProperties;

        public ExecutiveSummaryService(
                        MainDashboardService mainDashboardService,
                        ArubaService arubaService,
                        CitrixMetricsHistoryRepository citrixRepository,
                        Microsoft365MetricsHistoryRepository microsoft365Repository,
                        GlpiMetricsHistoryRepository glpiRepository,
                        AnalysisSnapshotRepository analysisSnapshotRepository,
                        KpiProperties kpiProperties) {

                this.mainDashboardService = mainDashboardService;
                this.arubaService = arubaService;
                this.citrixRepository = citrixRepository;
                this.microsoft365Repository = microsoft365Repository;
                this.glpiRepository = glpiRepository;
                this.analysisSnapshotRepository = analysisSnapshotRepository;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Genera la tarjeta ejecutiva que se muestra encima de los KPIs del
         * dashboard principal.
         */
        public ExecutiveSummaryDto getExecutiveSummary() {

                MainDashboardSummary dashboardSummary = mainDashboardService.getSummary();
                return buildOperationalSummary(
                                dashboardSummary,
                                resolveTrend(dashboardSummary.getGlobalHealthPercentage()),
                                null,
                                true,
                                currentArubaSummary(),
                                latestCitrixSnapshot(),
                                latestMicrosoft365Snapshot(),
                                latestGlpiSnapshot());
        }

        /**
         * Genera un resumen ejecutivo para el Banco de pruebas.
         *
         * Usa los mismos criterios de plataforma afectada, impacto y prioridad
         * que el dashboard real, pero no consulta históricos ni repositorios:
         * un escenario manual no tiene tendencia temporal fiable ni usuarios
         * reales estimables.
         */
        public ExecutiveSummaryDto buildScenarioSummary(MainDashboardSummary dashboardSummary) {

                return buildScenarioSummary(dashboardSummary, null, null, null, null);
        }

        /**
         * Genera el resumen ejecutivo del Banco de pruebas con los mismos datos
         * manuales usados para calcular los KPIs del escenario.
         */
        public ExecutiveSummaryDto buildScenarioSummary(
                        MainDashboardSummary dashboardSummary,
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                return buildOperationalSummary(
                                dashboardSummary,
                                "Tendencia no disponible en escenario manual",
                                "No estimable en escenario manual",
                                false,
                                aruba,
                                citrix,
                                microsoft365,
                                glpi);
        }

        private ExecutiveSummaryDto buildOperationalSummary(
                        MainDashboardSummary dashboardSummary,
                        String trend,
                        String estimatedUsersOverride,
                        boolean useRealObservedData,
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                Map<String, Integer> platformAffectations = extractPlatformAffectations(dashboardSummary);
                Map<String, String> platformStatuses = extractPlatformStatuses(dashboardSummary);
                List<String> affectedServices = resolveAffectedServices(platformAffectations, platformStatuses);
                List<String> criticalPlatforms = resolveCriticalPlatforms(platformAffectations, platformStatuses);
                String mainAffectedPlatform = resolveMainAffectedPlatform(platformAffectations);
                String probableOrigin = resolveProbableOrigin(mainAffectedPlatform);
                String impactLevel = resolveImpactLevel(dashboardSummary.getUserImpact(), criticalPlatforms.size());
                String priority = resolvePriority(dashboardSummary, affectedServices.size(), criticalPlatforms.size());
                String estimatedAffectedUsers = useRealObservedData
                                ? estimateAffectedUsers(mainAffectedPlatform)
                                : estimatedUsersOverride;
                List<PlatformFindingDto> platformFindings =
                                buildPlatformFindings(platformAffectations, aruba, citrix, microsoft365, glpi);
                ExecutiveSummaryDto summary = new ExecutiveSummaryDto();

                summary.setGlobalStatus(dashboardSummary.getGlobalHealth());
                summary.setAffectedServices(affectedServices);
                summary.setMainAffectedPlatform(mainAffectedPlatform);
                summary.setProbableOrigin(probableOrigin);
                summary.setImpactLevel(impactLevel);
                summary.setEstimatedAffectedUsers(estimatedAffectedUsers);
                summary.setPriority(priority);
                summary.setFirstAction(firstAction(mainAffectedPlatform));
                summary.setTrend(trend);
                summary.setPlatformFindings(platformFindings);
                summary.setSummaryText(
                                useRealObservedData
                                                ? buildSummaryText(
                                                                dashboardSummary,
                                                                mainAffectedPlatform,
                                                                affectedServices,
                                                                criticalPlatforms,
                                                                priority,
                                                                trend)
                                                : buildScenarioSummaryText(
                                                                dashboardSummary,
                                                                mainAffectedPlatform,
                                                                affectedServices,
                                                                criticalPlatforms,
                                                                priority));

                return summary;
        }

        private List<PlatformFindingDto> buildPlatformFindings(
                        Map<String, Integer> platformAffectations,
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                List<PlatformFindingDto> findings = new ArrayList<>();

                addPlatformFindings(findings, buildArubaFindings(platformAffectations, aruba, glpi));
                addPlatformFindings(findings, buildCitrixFindings(platformAffectations, citrix, glpi));
                addPlatformFindings(findings, buildMicrosoft365Findings(platformAffectations, microsoft365, glpi));
                addPlatformFindings(findings, buildGlpiFindings(platformAffectations, glpi));

                return findings;
        }

        private void addPlatformFindings(
                        List<PlatformFindingDto> result,
                        PlatformFindingAccumulator accumulator) {

                if (!accumulator.findings().isEmpty()) {

                        result.add(new PlatformFindingDto(
                                        accumulator.platform(),
                                        accumulator.status(),
                                        accumulator.findings()));
                }
        }

        private PlatformFindingAccumulator buildArubaFindings(
                        Map<String, Integer> platformAffectations,
                        ArubaSummary aruba,
                        GlpiMetricsHistory glpi) {

                PlatformFindingAccumulator findings =
                                new PlatformFindingAccumulator("Aruba", statusFromAffectation(platformAffectations.getOrDefault("Aruba", 0)));

                if (aruba == null) {

                        return findings;
                }

                if (aruba.getTotalAps() > 0) {

                        int downApsPercent = aruba.getDownAps() * 100 / aruba.getTotalAps();

                        if (downApsPercent >= kpiProperties.getAruba().getAccessPointDownRedPercent()) {

                                findings.note("El porcentaje de APs caidos alcanza el umbral rojo.");
                        } else if (downApsPercent >= kpiProperties.getAruba().getAccessPointDownYellowPercent()) {

                                findings.note("El porcentaje de APs caidos alcanza el umbral amarillo.");
                        }
                }

                if (aruba.getInactiveAps() >= kpiProperties.getAruba().getInactiveApsYellowMin()) {

                        findings.note("Hay " + aruba.getInactiveAps() + " APs inactivos.");
                }

                if (aruba.getFirmwareOutdated() >= kpiProperties.getAruba().getPendingFirmwareApsYellowMin()) {

                        findings.note("Hay firmware pendiente en puntos de acceso.");
                }

                if (aruba.getTotalSwitches() > 0 && aruba.getDownSwitches() >= aruba.getTotalSwitches()) {

                        findings.note("Los switches apagados alcanzan el umbral rojo.");
                } else if (aruba.getDownSwitches() > kpiProperties.getAruba().getSwitchDownYellowAbove()) {

                        findings.note("Hay " + aruba.getDownSwitches() + " switches caidos.");
                }

                if (aruba.getSwitchesFirmwareUpgradeRequired() >= kpiProperties.getAruba().getSwitchUpgradeYellowMin()) {

                        findings.note("Hay " + aruba.getSwitchesFirmwareUpgradeRequired() + " switches con upgrade pendiente.");
                }

                int underusedSwitches = aruba.getUnderusedSwitches() == null ? 0 : aruba.getUnderusedSwitches().size();

                if (underusedSwitches > kpiProperties.getAruba().getUnderusedSwitchesRedAbove()) {

                        findings.note("Los switches infrautilizados alcanzan el umbral rojo.");
                } else if (underusedSwitches > kpiProperties.getAruba().getUnderusedSwitchesYellowAbove()) {

                        findings.note("Hay " + underusedSwitches + " switches infrautilizados.");
                }

                if (aruba.getTotalWifiClients() == 0) {

                        findings.note("No hay clientes WiFi conectados.");
                }

                if (aruba.getMutualiaApsClients() == 0) {

                        findings.note("No hay clientes Mutualia-APS conectados.");
                }

                if (aruba.getMutualiaWifiClients() == 0) {

                        findings.note("No hay clientes Mutualia-WIFI conectados.");
                }

                int arubaOpenTickets = arubaOpenTickets(aruba, glpi);

                if (arubaOpenTickets >= kpiProperties.getAruba().getArubaOpenTicketsRedMin()) {

                        findings.note("Los tickets abiertos asociados a Aruba alcanzan el umbral rojo.");
                } else if (arubaOpenTickets >= kpiProperties.getAruba().getArubaOpenTicketsYellowMin()) {

                        findings.note("Hay " + arubaOpenTickets + " tickets abiertos asociados a Aruba.");
                }

                return findings;
        }

        private PlatformFindingAccumulator buildCitrixFindings(
                        Map<String, Integer> platformAffectations,
                        CitrixMetricsHistory citrix,
                        GlpiMetricsHistory glpi) {

                PlatformFindingAccumulator findings =
                                new PlatformFindingAccumulator("Citrix", statusFromAffectation(platformAffectations.getOrDefault("Citrix", 0)));

                if (citrix == null) {

                        addCitrixTicketFinding(findings, glpi);
                        return findings;
                }

                if (citrix.getDisconnectedSessions() > Math.max(10, citrix.getActiveSessions() / 4)) {

                        findings.yellow("Hay " + citrix.getDisconnectedSessions() + " sesiones desconectadas en Citrix.");
                }

                if (citrix.getActiveSessions() <= 0) {

                        findings.red("No hay sesiones activas observadas en Citrix.");
                }

                if (citrix.getAvailableDeliveryControllers() == 0) {

                        findings.red("No hay Delivery Controllers disponibles.");
                } else if (citrix.getTotalDeliveryControllers() > 0
                                && deliveryControllersAvailablePercent(citrix) < kpiProperties.getStatus().getYellowMin()) {

                        findings.red("Hay "
                                        + citrix.getAvailableDeliveryControllers()
                                        + " de "
                                        + citrix.getTotalDeliveryControllers()
                                        + " Delivery Controllers disponibles.");
                } else if (citrix.getTotalDeliveryControllers() > 0
                                && deliveryControllersAvailablePercent(citrix)
                                                < kpiProperties.getCitrix().getDeliveryControllerYellowBelowPercent()) {

                        findings.yellow("Hay "
                                        + citrix.getAvailableDeliveryControllers()
                                        + " de "
                                        + citrix.getTotalDeliveryControllers()
                                        + " Delivery Controllers disponibles.");
                }

                if (citrix.getAverageLogonDurationSeconds() > kpiProperties.getCitrix().getLogonDurationRedAboveSeconds()) {

                        findings.red("El tiempo medio de logon es critico: "
                                        + citrix.getAverageLogonDurationSeconds()
                                        + " segundos.");
                } else if (citrix.getAverageLogonDurationSeconds() > kpiProperties.getCitrix().getLogonDurationYellowAboveSeconds()) {

                        findings.yellow("El tiempo medio de logon es elevado: "
                                        + citrix.getAverageLogonDurationSeconds()
                                        + " segundos.");
                }

                if (citrix.getServerLoadPercent() >= kpiProperties.getCitrix().getServerLoadRedMin()) {

                        findings.red("La carga media de servidores Citrix es critica: "
                                        + citrix.getServerLoadPercent()
                                        + " %.");
                } else if (citrix.getServerLoadPercent() >= kpiProperties.getCitrix().getServerLoadYellowMin()) {

                        findings.yellow("La carga media de servidores Citrix es alta: "
                                        + citrix.getServerLoadPercent()
                                        + " %.");
                }

                if (citrix.getFailedLogons() > kpiProperties.getCitrix().getFailedLogonsRedAbove()) {

                        findings.red("Hay " + citrix.getFailedLogons() + " errores de inicio de sesion.");
                } else if (citrix.getFailedLogons() > kpiProperties.getCitrix().getFailedLogonsYellowAbove()) {

                        findings.yellow("Hay " + citrix.getFailedLogons() + " errores de inicio de sesion.");
                }

                addCitrixTicketFinding(findings, glpi);

                return findings;
        }

        private int deliveryControllersAvailablePercent(CitrixMetricsHistory citrix) {
                if (citrix.getTotalDeliveryControllers() <= 0) {
                        return 0;
                }

                return citrix.getAvailableDeliveryControllers() * 100 / citrix.getTotalDeliveryControllers();
        }

        private PlatformFindingAccumulator buildMicrosoft365Findings(
                        Map<String, Integer> platformAffectations,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                PlatformFindingAccumulator findings =
                                new PlatformFindingAccumulator("Microsoft 365", statusFromAffectation(platformAffectations.getOrDefault("Microsoft 365", 0)));

                if (microsoft365 == null) {

                        addMicrosoft365TicketFinding(findings, glpi);
                        return findings;
                }

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
                                                                glpi == null ? 0 : glpi.getMicrosoft365OpenTickets(),
                                                                microsoft365.getOutdatedWindowsDevices(),
                                                                microsoft365.getDevicesWithoutEncryption(),
                                                                microsoft365.getStaleDevices()),
                                                kpiProperties);

                result.reasons().forEach(findings::note);

                if (microsoft365.getActiveUsers() > 0
                                && microsoft365.getUsersWithoutMfa() > microsoft365.getActiveUsers()) {

                        findings.note("Los usuarios sin MFA superan los usuarios activos observados.");
                }

                return findings;
        }

        private PlatformFindingAccumulator buildGlpiFindings(
                        Map<String, Integer> platformAffectations,
                        GlpiMetricsHistory glpi) {

                PlatformFindingAccumulator findings =
                                new PlatformFindingAccumulator("GLPI", statusFromAffectation(platformAffectations.getOrDefault("GLPI", 0)));

                if (glpi == null) {

                        return findings;
                }

                if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsRedMin()) {

                        findings.red("Hay " + glpi.getOpenTickets() + " tickets abiertos.");
                } else if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsYellowMin()) {

                        findings.yellow("Hay " + glpi.getOpenTickets() + " tickets abiertos.");
                }

                if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsRedAbove()) {

                        findings.red("Hay " + glpi.getCriticalOpenTickets() + " tickets criticos abiertos.");
                } else if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsYellowAbove()) {

                        findings.yellow("Hay " + glpi.getCriticalOpenTickets() + " tickets criticos abiertos.");
                }

                addClosureFinding(findings, "diario", glpi.getCreatedToday(), glpi.getClosedToday());
                addClosureFinding(findings, "semanal", glpi.getCreatedThisWeek(), glpi.getClosedThisWeek());

                if (glpi.getCreatedThisWeek() > glpi.getClosedThisWeek()) {

                        findings.yellow("Se han creado mas tickets de los que se han cerrado durante la semana.");
                }

                if (glpi.getOperationalBacklog() >= kpiProperties.getGlpi().getOpenTicketsRedMin()) {

                        findings.red("El backlog operativo es alto: " + glpi.getOperationalBacklog() + " elementos.");
                } else if (glpi.getOperationalBacklog() >= kpiProperties.getGlpi().getOpenTicketsYellowMin()) {

                        findings.yellow("El backlog operativo es elevado: " + glpi.getOperationalBacklog() + " elementos.");
                }

                return findings;
        }

        private void addClosureFinding(PlatformFindingAccumulator findings, String period, int created, int closed) {

                if (created <= 0) {

                        return;
                }

                int closurePercent = closed * 100 / created;

                if (closurePercent < kpiProperties.getGlpi().getClosedPercentGreenMin()) {

                        findings.yellow("El porcentaje de cierre " + period + " es bajo: " + closurePercent + " %.");
                }
        }

        private void addCitrixTicketFinding(PlatformFindingAccumulator findings, GlpiMetricsHistory glpi) {

                Integer tickets = glpi == null ? null : glpi.getCitrixOpenTicketsRaw();

                if (tickets == null) {

                        return;
                }

                if (tickets >= kpiProperties.getCitrix().getCitrixOpenTicketsRedMin()) {

                        findings.red("Los tickets abiertos asociados a Citrix alcanzan el umbral rojo.");
                } else if (tickets >= kpiProperties.getCitrix().getCitrixOpenTicketsYellowMin()) {

                        findings.yellow("Hay " + tickets + " tickets abiertos asociados a Citrix.");
                }
        }

        private void addMicrosoft365TicketFinding(PlatformFindingAccumulator findings, GlpiMetricsHistory glpi) {

                Integer tickets = glpi == null ? null : glpi.getMicrosoft365OpenTicketsRaw();

                if (tickets == null) {

                        return;
                }

                if (tickets >= kpiProperties.getMicrosoft365().getMicrosoft365OpenTicketsRedMin()) {

                        findings.red("Los tickets abiertos asociados a Microsoft 365 alcanzan el umbral rojo.");
                } else if (tickets >= kpiProperties.getMicrosoft365().getMicrosoft365OpenTicketsYellowMin()) {

                        findings.yellow("Hay " + tickets + " tickets abiertos asociados a Microsoft 365.");
                }
        }

        private int arubaOpenTickets(ArubaSummary aruba, GlpiMetricsHistory glpi) {

                if (aruba != null && aruba.getArubaOpenTickets() > 0) {

                        return aruba.getArubaOpenTickets();
                }

                Integer tickets = glpi == null ? null : glpi.getArubaOpenTicketsRaw();

                return tickets == null ? 0 : tickets;
        }

        private String statusFromAffectation(int value) {

                if (value >= kpiProperties.getStatus().getRedMin()) {

                        return "RED";
                }

                if (value >= kpiProperties.getStatus().getYellowMin()) {

                        return "YELLOW";
                }

                return "GREEN";
        }

        private ArubaSummary currentArubaSummary() {

                if (arubaService == null) {

                        return null;
                }

                return arubaService.getSummary();
        }

        private CitrixMetricsHistory latestCitrixSnapshot() {

                if (citrixRepository == null) {

                        return null;
                }

                Optional<CitrixMetricsHistory> latest = citrixRepository.findTopByOrderByCollectedAtDesc();

                return latest == null ? null : latest.orElse(null);
        }

        private Microsoft365MetricsHistory latestMicrosoft365Snapshot() {

                if (microsoft365Repository == null) {

                        return null;
                }

                Optional<Microsoft365MetricsHistory> latest = microsoft365Repository.findTopByOrderByCollectedAtDesc();

                return latest == null ? null : latest.orElse(null);
        }

        private GlpiMetricsHistory latestGlpiSnapshot() {

                if (glpiRepository == null) {

                        return null;
                }

                Optional<GlpiMetricsHistory> latest = glpiRepository.findTopByOrderByCollectedAtDesc();

                return latest == null ? null : latest.orElse(null);
        }

        private Map<String, Integer> extractPlatformAffectations(MainDashboardSummary summary) {

                Map<String, Integer> values = new HashMap<>();

                values.put("Aruba", 0);
                values.put("Citrix", 0);
                values.put("Microsoft 365", 0);
                values.put("GLPI", 0);

                if (summary.getKpis() == null) {

                        return values;
                }

                for (KpiResultDto kpi : summary.getKpis()) {

                        if (kpi.getComponents() == null) {

                                continue;
                        }

                        for (KpiResultDto component : kpi.getComponents()) {

                                if ("aruba_network_affectation".equals(component.getId())) {

                                        values.put("Aruba", numericValue(component));
                                }

                                if ("citrix_health".equals(component.getId())) {

                                        values.put("Citrix", numericValue(component));
                                }

                                if ("microsoft365_health".equals(component.getId())) {

                                        values.put("Microsoft 365", numericValue(component));
                                }

                                if ("glpi_health".equals(component.getId())) {

                                        values.put("GLPI", numericValue(component));
                                }
                        }
                }

                return values;
        }

        private Map<String, String> extractPlatformStatuses(MainDashboardSummary summary) {

                Map<String, String> statuses = new HashMap<>();

                statuses.put("Aruba", "GREEN");
                statuses.put("Citrix", "GREEN");
                statuses.put("Microsoft 365", "GREEN");
                statuses.put("GLPI", "GREEN");

                if (summary.getKpis() == null) {

                        return statuses;
                }

                for (KpiResultDto kpi : summary.getKpis()) {

                        if (kpi.getComponents() == null) {

                                continue;
                        }

                        for (KpiResultDto component : kpi.getComponents()) {

                                String status =
                                                component.getStatus() == null
                                                                ? "GREEN"
                                                                : component.getStatus().name();

                                if ("aruba_network_affectation".equals(component.getId())) {

                                        statuses.put("Aruba", status);
                                }

                                if ("citrix_health".equals(component.getId())) {

                                        statuses.put("Citrix", status);
                                }

                                if ("microsoft365_health".equals(component.getId())) {

                                        statuses.put("Microsoft 365", status);
                                }

                                if ("glpi_health".equals(component.getId())) {

                                        statuses.put("GLPI", status);
                                }
                        }
                }

                return statuses;
        }

        private int numericValue(KpiResultDto component) {

                if (component.getScore() != null) {

                        return component.getScore();
                }

                if (component.getValue() instanceof Number number) {

                        return number.intValue();
                }

                return 0;
        }

        private List<String> resolveAffectedServices(
                        Map<String, Integer> platformAffectations,
                        Map<String, String> platformStatuses) {

                List<String> services = new ArrayList<>();

                if (isAffected("Aruba", platformAffectations, platformStatuses)) {

                        services.add("Red corporativa / conectividad");
                }

                if (isAffected("Citrix", platformAffectations, platformStatuses)) {

                        services.add("Acceso a aplicaciones corporativas");
                }

                if (isAffected("Microsoft 365", platformAffectations, platformStatuses)) {

                        services.add("Servicios cloud / identidad / colaboracion");
                }

                if (isAffected("GLPI", platformAffectations, platformStatuses)) {

                        services.add("Soporte IT / gestion de incidencias");
                }

                return services;
        }

        private List<String> resolveCriticalPlatforms(
                        Map<String, Integer> platformAffectations,
                        Map<String, String> platformStatuses) {

                return platformAffectations.entrySet().stream()
                                .filter(entry -> isCritical(entry.getKey(), platformAffectations, platformStatuses))
                                .map(Map.Entry::getKey)
                                .sorted()
                                .toList();
        }

        private boolean isAffected(
                        String platform,
                        Map<String, Integer> platformAffectations,
                        Map<String, String> platformStatuses) {

                return platformAffectations.getOrDefault(platform, 0) >= kpiProperties.getStatus().getYellowMin()
                                || "YELLOW".equalsIgnoreCase(platformStatuses.get(platform))
                                || "RED".equalsIgnoreCase(platformStatuses.get(platform));
        }

        private boolean isCritical(
                        String platform,
                        Map<String, Integer> platformAffectations,
                        Map<String, String> platformStatuses) {

                return platformAffectations.getOrDefault(platform, 0) >= kpiProperties.getStatus().getRedMin()
                                || "RED".equalsIgnoreCase(platformStatuses.get(platform));
        }

        private String resolveMainAffectedPlatform(Map<String, Integer> platformAffectations) {

                Map<String, Double> weightedContributions = Map.of(
                                "Aruba",
                                platformAffectations.getOrDefault("Aruba", 0)
                                                * kpiProperties.getWeights().getGlobalStatus().getAruba(),
                                "Citrix",
                                platformAffectations.getOrDefault("Citrix", 0)
                                                * kpiProperties.getWeights().getGlobalStatus().getCitrix(),
                                "Microsoft 365",
                                platformAffectations.getOrDefault("Microsoft 365", 0)
                                                * kpiProperties.getWeights().getGlobalStatus().getMicrosoft365(),
                                "GLPI",
                                platformAffectations.getOrDefault("GLPI", 0)
                                                * kpiProperties.getWeights().getGlobalStatus().getGlpi());

                return weightedContributions.entrySet().stream()
                                .max(Comparator.comparingDouble(Map.Entry::getValue))
                                .filter(entry -> entry.getValue() > 0)
                                .map(Map.Entry::getKey)
                                .orElse("Sin plataforma afectada");
        }

        private String resolveProbableOrigin(String mainAffectedPlatform) {

                if ("Sin plataforma afectada".equals(mainAffectedPlatform)) {

                        return "No se detecta origen probable con los datos actuales";
                }

                return mainAffectedPlatform;
        }

        private String levelFromAffectation(int value) {

                if (value >= kpiProperties.getStatus().getRedMin()) {

                        return "HIGH";
                }

                if (value >= kpiProperties.getStatus().getYellowMin()) {

                        return "MODERATE";
                }

                return "LOW";
        }

        private String resolveImpactLevel(int userImpact, int criticalPlatformCount) {

                if (criticalPlatformCount >= 2) {

                        return "HIGH";
                }

                return levelFromAffectation(userImpact);
        }

        private String resolvePriority(
                        MainDashboardSummary summary,
                        int affectedServicesCount,
                        int criticalPlatformCount) {

                if (criticalPlatformCount >= 2) {

                        return "HIGH";
                }

                if (summary.getUserImpact() >= kpiProperties.getStatus().getRedMin()
                                || summary.getSlaRisk() >= kpiProperties.getStatus().getRedMin()
                                || summary.getGlobalCriticality() >= kpiProperties.getStatus().getRedMin()) {

                        return "HIGH";
                }

                if (criticalPlatformCount >= 1) {

                        return "MEDIUM";
                }

                if (summary.getGlobalHealthPercentage() >= kpiProperties.getStatus().getYellowMin()
                                || affectedServicesCount >= 2) {

                        return "MEDIUM";
                }

                return "LOW";
        }

        /**
         * Compara el estado global actual con los últimos snapshots de análisis
         * para clasificar la tendencia como WORSENING, IMPROVING o STABLE.
         */
        private String resolveTrend(int currentGlobalStatus) {

                List<AnalysisSnapshot> snapshots = analysisSnapshotRepository.findTop5ByOrderByTimestampDesc();

                List<Integer> previousValues = snapshots.stream().map(AnalysisSnapshot::getGlobalStatus).filter(value -> value != null).toList();

                if (previousValues.isEmpty()) {

                        return "STABLE";
                }

                double average = previousValues.stream()
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(currentGlobalStatus);

                double difference = currentGlobalStatus - average;

                if (difference >= kpiProperties.getExecutive().getTrendDifferenceThreshold()) {

                        return "WORSENING";
                }

                if (difference <= -kpiProperties.getExecutive().getTrendDifferenceThreshold()) {

                        return "IMPROVING";
                }

                return "STABLE";
        }

        /**
         * Estima usuarios o elementos potencialmente afectados solo con datos
         * observables. Si no hay señal suficiente, no inventa cifras.
         */
        private String estimateAffectedUsers(String mainAffectedPlatform) {

                if ("Aruba".equals(mainAffectedPlatform)) {

                        ArubaSummary aruba = arubaService.getSummary();

                        if (aruba.getTotalWifiClients() > 0) {

                                return aruba.getTotalWifiClients() + " clientes WiFi observados";
                        }
                }

                if ("Citrix".equals(mainAffectedPlatform)) {

                        Optional<CitrixMetricsHistory> citrix = citrixRepository.findTopByOrderByCollectedAtDesc();

                        if (citrix.isPresent() && citrix.get().getActiveSessions() > 0) {

                                return citrix.get().getActiveSessions() + " sesiones activas observadas";
                        }

                        if (citrix.isPresent() && citrix.get().getFailedLogons() > 0) {

                                return citrix.get().getFailedLogons() + " errores de inicio observados";
                        }
                }

                if ("Microsoft 365".equals(mainAffectedPlatform)) {

                        Optional<Microsoft365MetricsHistory> microsoft365 = microsoft365Repository.findTopByOrderByCollectedAtDesc();

                        if (microsoft365.isPresent() && microsoft365.get().getActiveUsers() > 0) {

                                return microsoft365.get().getActiveUsers() + " usuarios activos observados";
                        }

                        int affectedDevices = microsoft365.map(this::microsoftAffectedDevices).orElse(0);

                        if (affectedDevices > 0) {

                                return affectedDevices + " dispositivos con senales de afección";
                        }
                }

                if ("GLPI".equals(mainAffectedPlatform)) {

                        Optional<GlpiMetricsHistory> glpi = glpiRepository.findTopByOrderByCollectedAtDesc();

                        if (glpi.isPresent() && glpi.get().getCriticalOpenTickets() > 0) {

                                return glpi.get().getCriticalOpenTickets() + " tickets críticos como senal indirecta";
                        }

                        if (glpi.isPresent() && glpi.get().getOpenTickets() > 0) {

                                return glpi.get().getOpenTickets() + " tickets abiertos como senal indirecta";
                        }
                }

                return NOT_ESTIMABLE;
        }

        private int microsoftAffectedDevices(Microsoft365MetricsHistory microsoft365) {

                return microsoft365.getNonCompliantDevices()
                                + microsoft365.getOutdatedWindowsDevices()
                                + microsoft365.getDevicesWithoutEncryption()
                                + microsoft365.getUsersWithoutMfa();
        }

        private String firstAction(String mainAffectedPlatform) {

                return switch (mainAffectedPlatform) {
                        case "Aruba" -> "Revisar APs inactivos, switches caidos, clientes WiFi y firmware pendiente";
                        case "Citrix" -> "Revisar Delivery Controllers, sesiones activas, logon duration y errores de inicio";
                        case "Microsoft 365" -> "Revisar SharePoint, usuarios sin MFA, dispositivos no conformes, secretos proximos a caducar y cifrado";
                        case "GLPI" -> "Revisar tickets críticos, tickets abiertos y tasa de cierre";
                        default -> "Mantener seguimiento de KPIs y frescura de datos";
                };
        }

        /**
         * Compone una explicación breve del diagnóstico operativo.
         */
        private String buildSummaryText(
                        MainDashboardSummary summary,
                        String mainAffectedPlatform,
                        List<String> affectedServices,
                        List<String> criticalPlatforms,
                        String priority,
                        String trend) {

                String serviceText = affectedServices.isEmpty()
                                ? "no se detectan servicios afectados"
                                : "servicios afectados: " + String.join(", ", affectedServices);

                if ("Sin plataforma afectada".equals(mainAffectedPlatform)) {

                        return "El estado global se encuentra en "
                                        + statusText(summary.getGlobalHealth())
                                        + " y " + serviceText
                                        + ". La prioridad operativa es " + priority
                                        + " y la tendencia es " + trend + ".";
                }

                String criticalText = criticalPlatformText(summary, criticalPlatforms);

                return "El estado global se encuentra en "
                                + statusText(summary.getGlobalHealth())
                                + criticalText
                                + ". La principal contribucion procede de "
                                + mainAffectedPlatform
                                + ", con " + serviceText
                                + ". La prioridad operativa es " + priority
                                + " y la tendencia es " + trend
                                + ". Se recomienda revisar primero: "
                                + firstAction(mainAffectedPlatform)
                                + ".";
        }

        private String buildScenarioSummaryText(
                        MainDashboardSummary summary,
                        String mainAffectedPlatform,
                        List<String> affectedServices,
                        List<String> criticalPlatforms,
                        String priority) {

                String serviceText = affectedServices.isEmpty()
                                ? "no se detectan servicios afectados"
                                : "servicios afectados: " + String.join(", ", affectedServices);

                if ("Sin plataforma afectada".equals(mainAffectedPlatform)) {

                        return "Este escenario manual muestra un estado global "
                                        + statusText(summary.getGlobalHealth())
                                        + "; " + serviceText
                                        + ". La prioridad operativa es " + priority
                                        + ". No se calcula tendencia porque no hay histórico asociado al banco de pruebas.";
                }

                String criticalText = criticalPlatformText(summary, criticalPlatforms);

                return "Este escenario manual sugiere revisar primero "
                                + mainAffectedPlatform
                                + ". El estado global aparece en "
                                + statusText(summary.getGlobalHealth())
                                + criticalText
                                + ", con " + serviceText
                                + ". La prioridad operativa es " + priority
                                + ". La lectura orienta la revision, pero no demuestra causalidad ni modifica los datos reales.";
        }

        private String criticalPlatformText(MainDashboardSummary summary, List<String> criticalPlatforms) {

                if (criticalPlatforms.isEmpty()) {

                        return "";
                }

                String platforms = String.join(", ", criticalPlatforms);

                if (criticalPlatforms.size() >= 2) {

                        return ", aunque varias plataformas presentan estado critico: " + platforms;
                }

                if ("GREEN".equalsIgnoreCase(summary.getGlobalHealth())) {

                        return ", aunque " + platforms + " presenta estado critico";
                }

                return ", con estado critico en " + platforms;
        }

        private String statusText(String status) {

                if ("GREEN".equalsIgnoreCase(status)) {

                        return "verde";
                }

                if ("YELLOW".equalsIgnoreCase(status)) {

                        return "amarillo";
                }

                if ("RED".equalsIgnoreCase(status)) {

                        return "rojo";
                }

                return status == null ? "desconocido" : status.toLowerCase();
        }

        private static class PlatformFindingAccumulator {

                private final String platform;
                private final List<String> findings = new ArrayList<>();
                private String status;

                PlatformFindingAccumulator(String platform, String status) {

                        this.platform = platform;
                        this.status = status == null ? "GREEN" : status;
                }

                String platform() {

                        return platform;
                }

                String status() {

                        return status;
                }

                List<String> findings() {

                        return findings;
                }

                void yellow(String finding) {

                        add("YELLOW", finding);
                }

                void red(String finding) {

                        add("RED", finding);
                }

                void note(String finding) {

                        if (finding == null || finding.isBlank()) {

                                return;
                        }

                        findings.add(finding);
                }

                private void add(String findingStatus, String finding) {

                        if (finding == null || finding.isBlank()) {

                                return;
                        }

                        findings.add(finding);

                        if (severity(findingStatus) > severity(status)) {

                                status = findingStatus;
                        }
                }

                private int severity(String value) {

                        if ("RED".equalsIgnoreCase(value)) {

                                return 3;
                        }

                        if ("YELLOW".equalsIgnoreCase(value)) {

                                return 2;
                        }

                        return 1;
                }
        }
}

