package com.tfg.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.CitrixHealthStatusDto;
import com.tfg.dashboard.dto.Microsoft365HealthStatusDto;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;

@Service
public class SimulationConsistencyService {

    private static final String RED = "RED";
    private static final String YELLOW = "YELLOW";
    private static final int WEEK_DAYS = 7;
    private static final int GREEN_TICKET_WEIGHT = 10;
    private static final int YELLOW_TICKET_WEIGHT = 30;
    private static final int RED_TICKET_WEIGHT = 60;
    private static final int TICKET_WEIGHT_VARIATION = 6;
    private static final int MINIMUM_WEEKLY_CLOSED_TICKETS = 20;

    private final ArubaSummaryService arubaSummaryService;
    private final GlobalKpiCalculationService globalKpiCalculationService;
    private final KpiScoringService kpiScoringService;
    private final GlpiMetricsHistoryRepository glpiRepository;

    public SimulationConsistencyService(
            ArubaSummaryService arubaSummaryService,
            GlobalKpiCalculationService globalKpiCalculationService,
            KpiScoringService kpiScoringService,
            GlpiMetricsHistoryRepository glpiRepository) {

        this.arubaSummaryService = arubaSummaryService;
        this.globalKpiCalculationService = globalKpiCalculationService;
        this.kpiScoringService = kpiScoringService;
        this.glpiRepository = glpiRepository;
    }

    public void applyToGeneratedSummaries(
            CitrixSummary citrix,
            Microsoft365Summary microsoft365,
            GlpiSummary glpi,
            LocalDateTime collectedAt) {

        ArubaSummary aruba = arubaSummaryService.getSummary();

        applyBasicRangeConsistency(aruba, citrix, microsoft365);

        String arubaStatus = kpiScoringService.statusFromAffection(
                globalKpiCalculationService.calculateArubaNetworkAffection(aruba));
        String citrixStatus =
                globalKpiCalculationService.calculateCitrixHealthStatus(toCitrixHistory(citrix, collectedAt));
        String microsoft365Status = kpiScoringService.statusFromAffection(
                globalKpiCalculationService.calculateMicrosoft365HealthAffection(
                        toMicrosoft365History(microsoft365, collectedAt)));

        if (citrix.getCitrixHealthDetails() == null) {
            citrix.setCitrixHealthDetails(new CitrixHealthStatusDto());
        }
        citrix.getCitrixHealthDetails().setColor(citrixStatus);
        if (microsoft365.getMicrosoft365HealthDetails() == null) {
            microsoft365.setMicrosoft365HealthDetails(new Microsoft365HealthStatusDto());
        }
        microsoft365.getMicrosoft365HealthDetails().setColor(microsoft365Status);

        applyGlpiConsistency(glpi, arubaStatus, citrixStatus, microsoft365Status, collectedAt);
    }

    public void applyBasicRangeConsistency(
            ArubaSummary aruba,
            CitrixSummary citrix,
            Microsoft365Summary microsoft365) {

        int wifiClients = nonNegative(aruba.getTotalWifiClients());
        int activeSessions = nonNegative(citrix.getActiveSessions());
        int disconnectedSessions = nonNegative(citrix.getDisconnectedSessions());

        if (wifiClients == 0) {
            activeSessions = 0;
            disconnectedSessions = 0;
        } else {
            activeSessions = Math.min(activeSessions, wifiClients);
            disconnectedSessions = Math.min(disconnectedSessions, wifiClients - activeSessions);
        }

        int totalDeliveryControllers = nonNegative(citrix.getTotalDeliveryControllers());
        int availableDeliveryControllers = Math.min(
                nonNegative(citrix.getAvailableDeliveryControllers()),
                totalDeliveryControllers);

        citrix.setActiveSessions(activeSessions);
        citrix.setDisconnectedSessions(disconnectedSessions);
        citrix.setTotalDeliveryControllers(totalDeliveryControllers);
        citrix.setAvailableDeliveryControllers(availableDeliveryControllers);
        citrix.setAverageLogonDurationSeconds(nonNegative(citrix.getAverageLogonDurationSeconds()));
        citrix.setServerLoadPercent(percent(citrix.getServerLoadPercent()));
        citrix.setFailedLogons(nonNegative(citrix.getFailedLogons()));

        int activeUsers = nonNegative(microsoft365.getActiveUsers());
        int nonCompliantDevices = nonNegative(microsoft365.getNonCompliantDevices());

        microsoft365.setActiveUsers(activeUsers);
        microsoft365.setSharePointStoragePercent(percent(microsoft365.getSharePointStoragePercent()));
        microsoft365.setRiskyUsers(Math.min(nonNegative(microsoft365.getRiskyUsers()), activeUsers));
        microsoft365.setUsersWithoutMfa(Math.min(nonNegative(microsoft365.getUsersWithoutMfa()), activeUsers));
        microsoft365.setAppsSecretsExpiringSoon(nonNegative(microsoft365.getAppsSecretsExpiringSoon()));
        microsoft365.setNonCompliantDevices(nonCompliantDevices);
        microsoft365.setOutdatedWindowsDevices(nonNegative(microsoft365.getOutdatedWindowsDevices()));
        microsoft365.setDevicesWithoutEncryption(
                Math.min(nonNegative(microsoft365.getDevicesWithoutEncryption()), nonCompliantDevices));
        microsoft365.setStaleDevices(nonNegative(microsoft365.getStaleDevices()));
    }

    public void applyGlpiConsistency(
            GlpiSummary glpi,
            String arubaStatus,
            String citrixStatus,
            String microsoft365Status,
            LocalDateTime collectedAt) {

        WeeklyTicketActivity weeklyActivity = buildWeeklyActivity(glpi, collectedAt);
        int openTickets = weeklyActivity.openTickets();
        TicketDistribution distribution = distributeTickets(
                openTickets,
                arubaStatus,
                citrixStatus,
                microsoft365Status,
                glpi.getArubaOpenTickets(),
                glpi.getCitrixOpenTickets(),
                glpi.getMicrosoft365OpenTickets());

        glpi.setArubaOpenTickets(distribution.aruba());
        glpi.setCitrixOpenTickets(distribution.citrix());
        glpi.setMicrosoft365OpenTickets(distribution.microsoft365());
        glpi.setOpenTickets(openTickets);
        glpi.setCreatedToday(weeklyActivity.createdToday());
        glpi.setClosedToday(weeklyActivity.closedToday());
        glpi.setCreatedThisWeek(weeklyActivity.createdThisWeek());
        glpi.setClosedThisWeek(weeklyActivity.closedThisWeek());
        glpi.setCriticalOpenTickets(criticalTickets(glpi.getCriticalOpenTickets(), glpi.getOpenTickets()));
        glpi.setSlaBreachedTickets(Math.min(nonNegative(glpi.getSlaBreachedTickets()), glpi.getOpenTickets()));
        glpi.setOperationalBacklog(glpi.getOpenTickets());
    }

    WeeklyTicketActivity buildWeeklyActivity(
            GlpiSummary current,
            LocalDateTime collectedAt) {

        LocalDateTime safeCollectedAt = collectedAt == null ? LocalDateTime.now() : collectedAt;
        LocalDate today = safeCollectedAt.toLocalDate();
        LocalDate firstDay = today.minusDays(WEEK_DAYS - 1L);
        Map<LocalDate, GlpiMetricsHistory> latestSnapshotsByDay = latestSnapshotsByDay(firstDay, today);

        int previousCreated = 0;
        int previousClosed = 0;

        for (Map.Entry<LocalDate, GlpiMetricsHistory> entry : latestSnapshotsByDay.entrySet()) {
            if (!entry.getKey().isBefore(firstDay) && entry.getKey().isBefore(today)) {
                int created = nonNegative(entry.getValue().getCreatedToday());
                int closed = Math.min(nonNegative(entry.getValue().getClosedToday()), created);
                previousCreated += created;
                previousClosed += closed;
            }
        }

        int createdToday = nonNegative(current.getCreatedToday());
        int closedToday = Math.min(nonNegative(current.getClosedToday()), createdToday);

        int createdThisWeek = previousCreated + createdToday;
        int closedThisWeek = previousClosed + closedToday;

        if (closedThisWeek > createdThisWeek) {
            closedThisWeek = createdThisWeek;
        }

        if (createdThisWeek == closedThisWeek) {
            createdThisWeek += MINIMUM_WEEKLY_CLOSED_TICKETS;
            closedThisWeek += MINIMUM_WEEKLY_CLOSED_TICKETS;
            createdToday += MINIMUM_WEEKLY_CLOSED_TICKETS;
            closedToday += MINIMUM_WEEKLY_CLOSED_TICKETS;
        }

        return new WeeklyTicketActivity(
                createdToday,
                closedToday,
                createdThisWeek,
                closedThisWeek,
                Math.max(0, createdThisWeek - closedThisWeek));
    }

    private Map<LocalDate, GlpiMetricsHistory> latestSnapshotsByDay(LocalDate firstDay, LocalDate today) {
        LocalDateTime since = firstDay.atStartOfDay();
        Map<LocalDate, GlpiMetricsHistory> snapshotsByDay = new LinkedHashMap<>();

        List<GlpiMetricsHistory> snapshots = glpiRepository.findByCollectedAtAfterOrderByCollectedAtAsc(since);

        snapshots.stream()
                .filter(snapshot -> snapshot.getCollectedAt() != null)
                .filter(snapshot -> !snapshot.getCollectedAt().toLocalDate().isAfter(today))
                .sorted(Comparator.comparing(GlpiMetricsHistory::getCollectedAt))
                .forEach(snapshot -> snapshotsByDay.put(snapshot.getCollectedAt().toLocalDate(), snapshot));

        return snapshotsByDay;
    }

    private TicketDistribution distributeTickets(
            int totalTickets,
            String arubaStatus,
            String citrixStatus,
            String microsoft365Status,
            int arubaSeed,
            int citrixSeed,
            int microsoft365Seed) {

        int arubaWeight = platformTicketWeight(arubaStatus, arubaSeed);
        int citrixWeight = platformTicketWeight(citrixStatus, citrixSeed);
        int microsoft365Weight = platformTicketWeight(microsoft365Status, microsoft365Seed);
        int totalWeight = arubaWeight + citrixWeight + microsoft365Weight;

        if (totalTickets <= 0 || totalWeight <= 0) {
            return new TicketDistribution(0, 0, 0);
        }

        int arubaTickets = totalTickets * arubaWeight / totalWeight;
        int citrixTickets = totalTickets * citrixWeight / totalWeight;
        int microsoft365Tickets = totalTickets * microsoft365Weight / totalWeight;
        int assignedTickets = arubaTickets + citrixTickets + microsoft365Tickets;
        int remainder = totalTickets - assignedTickets;

        List<TicketRemainderTarget> targets = new ArrayList<>(List.of(
                new TicketRemainderTarget(0, arubaWeight, fractionalPart(totalTickets, arubaWeight, totalWeight)),
                new TicketRemainderTarget(1, citrixWeight, fractionalPart(totalTickets, citrixWeight, totalWeight)),
                new TicketRemainderTarget(2, microsoft365Weight, fractionalPart(totalTickets, microsoft365Weight, totalWeight))));

        targets.sort(Comparator
                .comparingDouble(TicketRemainderTarget::fraction)
                .thenComparingInt(TicketRemainderTarget::weight)
                .reversed());

        for (int i = 0; i < remainder; i++) {
            int targetIndex = targets.get(i % targets.size()).index();

            if (targetIndex == 0) {
                arubaTickets++;
            } else if (targetIndex == 1) {
                citrixTickets++;
            } else {
                microsoft365Tickets++;
            }
        }

        return new TicketDistribution(arubaTickets, citrixTickets, microsoft365Tickets);
    }

    private int platformTicketWeight(String status, int generatedTicketsSeed) {
        int variation = Math.abs(generatedTicketsSeed) % TICKET_WEIGHT_VARIATION;

        if (RED.equalsIgnoreCase(status)) {
            return RED_TICKET_WEIGHT + variation;
        }

        if (YELLOW.equalsIgnoreCase(status)) {
            return YELLOW_TICKET_WEIGHT + variation;
        }

        return GREEN_TICKET_WEIGHT + variation;
    }

    private double fractionalPart(int totalTickets, int weight, int totalWeight) {
        double exactShare = (double) totalTickets * weight / totalWeight;

        return exactShare - Math.floor(exactShare);
    }

    private int criticalTickets(int currentCriticalTickets, int openTickets) {
        if (openTickets <= 0) {
            return 0;
        }

        return Math.min(nonNegative(currentCriticalTickets), openTickets - 1);
    }

    private CitrixMetricsHistory toCitrixHistory(CitrixSummary summary, LocalDateTime collectedAt) {
        CitrixMetricsHistory history = new CitrixMetricsHistory();
        history.setActiveSessions(summary.getActiveSessions());
        history.setActiveLicenses(summary.getActiveLicenses());
        history.setAvailableDeliveryControllers(summary.getAvailableDeliveryControllers());
        history.setTotalDeliveryControllers(summary.getTotalDeliveryControllers());
        history.setDisconnectedSessions(summary.getDisconnectedSessions());
        history.setAverageLogonDurationSeconds(summary.getAverageLogonDurationSeconds());
        history.setServerLoadPercent(summary.getServerLoadPercent());
        history.setFailedLogons(summary.getFailedLogons());
        history.setCollectedAt(collectedAt);
        return history;
    }

    private Microsoft365MetricsHistory toMicrosoft365History(Microsoft365Summary summary, LocalDateTime collectedAt) {
        Microsoft365MetricsHistory history = new Microsoft365MetricsHistory();
        history.setActiveUsers(summary.getActiveUsers());
        history.setUnassignedLicenses(summary.getUnassignedLicenses());
        history.setOutlookStatus(summary.getOutlookStatus());
        history.setTeamsStatus(summary.getTeamsStatus());
        history.setSharePointStatus(summary.getSharePointStatus());
        history.setNearlyFullMailboxes(summary.getNearlyFullMailboxes());
        history.setEmailsQuarantined(summary.getEmailsQuarantined());
        history.setSharePointStoragePercent(summary.getSharePointStoragePercent());
        history.setRiskyUsers(summary.getRiskyUsers());
        history.setFailedSignIns(summary.getFailedSignIns());
        history.setUsersWithoutMfa(summary.getUsersWithoutMfa());
        history.setAppsSecretsExpiringSoon(summary.getAppsSecretsExpiringSoon());
        history.setUnusedApplications(summary.getUnusedApplications());
        history.setHighPrivilegeApplications(summary.getHighPrivilegeApplications());
        history.setNonCompliantDevices(summary.getNonCompliantDevices());
        history.setOutdatedWindowsDevices(summary.getOutdatedWindowsDevices());
        history.setDevicesWithoutEncryption(summary.getDevicesWithoutEncryption());
        history.setStaleDevices(summary.getStaleDevices());
        history.setCollectedAt(collectedAt);
        return history;
    }

    private int percent(int value) {
        return Math.min(100, nonNegative(value));
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }

    record WeeklyTicketActivity(
            int createdToday,
            int closedToday,
            int createdThisWeek,
            int closedThisWeek,
            int openTickets) {
    }

    private record TicketDistribution(
            int aruba,
            int citrix,
            int microsoft365) {
    }

    private record TicketRemainderTarget(
            int index,
            int weight,
            double fraction) {
    }
}
