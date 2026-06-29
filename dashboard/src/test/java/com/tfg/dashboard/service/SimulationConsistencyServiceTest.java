package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.dto.summary.CitrixSummary;
import com.tfg.dashboard.dto.summary.GlpiSummary;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;

class SimulationConsistencyServiceTest {

    @Test
    void citrixSessionsNeverExceedArubaWifiClients() {
        SimulationConsistencyService service = serviceWithEmptyGlpiHistory();
        ArubaSummary aruba = new ArubaSummary();
        CitrixSummary citrix = new CitrixSummary();
        Microsoft365Summary microsoft365 = new Microsoft365Summary();

        aruba.setTotalWifiClients(100);
        citrix.setActiveSessions(90);
        citrix.setDisconnectedSessions(30);
        microsoft365.setActiveUsers(150);

        service.applyBasicRangeConsistency(aruba, citrix, microsoft365);

        assertThat(citrix.getActiveSessions() + citrix.getDisconnectedSessions())
                .isLessThanOrEqualTo(aruba.getTotalWifiClients());
    }

    @Test
    void zeroWifiClientsForcesCitrixSessionsToZero() {
        SimulationConsistencyService service = serviceWithEmptyGlpiHistory();
        ArubaSummary aruba = new ArubaSummary();
        CitrixSummary citrix = new CitrixSummary();
        Microsoft365Summary microsoft365 = new Microsoft365Summary();

        aruba.setTotalWifiClients(0);
        citrix.setActiveSessions(25);
        citrix.setDisconnectedSessions(5);
        microsoft365.setActiveUsers(25);

        service.applyBasicRangeConsistency(aruba, citrix, microsoft365);

        assertThat(citrix.getActiveSessions()).isZero();
        assertThat(citrix.getDisconnectedSessions()).isZero();
        assertThat(microsoft365.getActiveUsers()).isEqualTo(25);
    }

    @Test
    void keepsMicrosoft365AndCitrixMetricsIndependent() {
        SimulationConsistencyService service = serviceWithEmptyGlpiHistory();
        ArubaSummary aruba = new ArubaSummary();
        CitrixSummary citrix = new CitrixSummary();
        Microsoft365Summary microsoft365 = new Microsoft365Summary();

        aruba.setTotalWifiClients(80);
        citrix.setActiveSessions(60);
        citrix.setDisconnectedSessions(10);
        microsoft365.setActiveUsers(120);
        microsoft365.setRiskyUsers(180);
        microsoft365.setUsersWithoutMfa(90);
        microsoft365.setNonCompliantDevices(12);
        microsoft365.setDevicesWithoutEncryption(40);
        microsoft365.setSharePointStoragePercent(130);

        service.applyBasicRangeConsistency(aruba, citrix, microsoft365);

        assertThat(microsoft365.getActiveUsers()).isEqualTo(120);
        assertThat(microsoft365.getRiskyUsers()).isLessThanOrEqualTo(microsoft365.getActiveUsers());
        assertThat(microsoft365.getUsersWithoutMfa()).isLessThanOrEqualTo(microsoft365.getActiveUsers());
        assertThat(microsoft365.getDevicesWithoutEncryption())
                .isLessThanOrEqualTo(microsoft365.getNonCompliantDevices());
        assertThat(microsoft365.getSharePointStoragePercent()).isEqualTo(100);
    }

    @Test
    void microsoft365ActiveUsersCanBePositiveWhenCitrixHasNoActiveSessions() {
        SimulationConsistencyService service = serviceWithEmptyGlpiHistory();
        ArubaSummary aruba = new ArubaSummary();
        CitrixSummary citrix = new CitrixSummary();
        Microsoft365Summary microsoft365 = new Microsoft365Summary();

        aruba.setTotalWifiClients(200);
        citrix.setActiveSessions(0);
        citrix.setDisconnectedSessions(0);
        microsoft365.setActiveUsers(150);
        microsoft365.setUsersWithoutMfa(20);

        service.applyBasicRangeConsistency(aruba, citrix, microsoft365);

        assertThat(citrix.getActiveSessions()).isZero();
        assertThat(microsoft365.getActiveUsers()).isEqualTo(150);
        assertThat(microsoft365.getUsersWithoutMfa()).isEqualTo(20);
    }

    @Test
    void glpiPlatformTicketsIncreaseWithPlatformSeverityAndKeepTotalCoherent() {
        SimulationConsistencyService service = serviceWithEmptyGlpiHistory();
        GlpiSummary glpi = new GlpiSummary();

        glpi.setCreatedToday(30);
        glpi.setClosedToday(10);
        glpi.setCriticalOpenTickets(500);
        glpi.setArubaOpenTickets(2);
        glpi.setCitrixOpenTickets(2);
        glpi.setMicrosoft365OpenTickets(2);

        service.applyGlpiConsistency(glpi, "GREEN", "YELLOW", "RED", LocalDateTime.now());

        assertThat(glpi.getMicrosoft365OpenTickets()).isGreaterThan(glpi.getCitrixOpenTickets());
        assertThat(glpi.getCitrixOpenTickets()).isGreaterThan(glpi.getArubaOpenTickets());
        assertThat(glpi.getOpenTickets())
                .isEqualTo(glpi.getArubaOpenTickets()
                        + glpi.getCitrixOpenTickets()
                        + glpi.getMicrosoft365OpenTickets());
        assertThat(glpi.getCreatedThisWeek() - glpi.getClosedThisWeek())
                .isEqualTo(glpi.getOpenTickets());
        assertThat(glpi.getCriticalOpenTickets()).isLessThan(glpi.getOpenTickets());
    }

    @Test
    void citrixRedReceivesMostTicketsWhenOtherPlatformsAreGreen() {
        GlpiSummary glpi = glpiWithWeeklyBacklog(120);

        serviceWithEmptyGlpiHistory()
                .applyGlpiConsistency(glpi, "GREEN", "RED", "GREEN", LocalDateTime.now());

        assertThat(glpi.getCitrixOpenTickets()).isGreaterThan(glpi.getArubaOpenTickets());
        assertThat(glpi.getCitrixOpenTickets()).isGreaterThan(glpi.getMicrosoft365OpenTickets());
        assertThat(platformTicketTotal(glpi)).isEqualTo(glpi.getOpenTickets());
    }

    @Test
    void microsoft365RedReceivesMostTicketsWhenOtherPlatformsAreGreen() {
        GlpiSummary glpi = glpiWithWeeklyBacklog(120);

        serviceWithEmptyGlpiHistory()
                .applyGlpiConsistency(glpi, "GREEN", "GREEN", "RED", LocalDateTime.now());

        assertThat(glpi.getMicrosoft365OpenTickets()).isGreaterThan(glpi.getArubaOpenTickets());
        assertThat(glpi.getMicrosoft365OpenTickets()).isGreaterThan(glpi.getCitrixOpenTickets());
        assertThat(platformTicketTotal(glpi)).isEqualTo(glpi.getOpenTickets());
    }

    @Test
    void arubaRedReceivesMostTicketsWhenOtherPlatformsAreGreen() {
        GlpiSummary glpi = glpiWithWeeklyBacklog(120);

        serviceWithEmptyGlpiHistory()
                .applyGlpiConsistency(glpi, "RED", "GREEN", "GREEN", LocalDateTime.now());

        assertThat(glpi.getArubaOpenTickets()).isGreaterThan(glpi.getCitrixOpenTickets());
        assertThat(glpi.getArubaOpenTickets()).isGreaterThan(glpi.getMicrosoft365OpenTickets());
        assertThat(platformTicketTotal(glpi)).isEqualTo(glpi.getOpenTickets());
    }

    @Test
    void zeroOpenTicketsClearsPlatformAndCriticalTickets() {
        GlpiSummary glpi = glpiWithWeeklyBacklog(0);

        glpi.setCriticalOpenTickets(10);

        serviceWithEmptyGlpiHistory()
                .applyGlpiConsistency(glpi, "RED", "RED", "RED", LocalDateTime.now());

        assertThat(glpi.getOpenTickets()).isZero();
        assertThat(glpi.getArubaOpenTickets()).isZero();
        assertThat(glpi.getCitrixOpenTickets()).isZero();
        assertThat(glpi.getMicrosoft365OpenTickets()).isZero();
        assertThat(glpi.getCriticalOpenTickets()).isZero();
    }

    @Test
    void weeklyTotalsUseOneDailyValuePerDayPlusCurrentSummary() {
        GlpiMetricsHistoryRepository repository = mock(GlpiMetricsHistoryRepository.class);
        LocalDateTime collectedAt = LocalDate.of(2026, 6, 3).atTime(12, 0);
        List<GlpiMetricsHistory> previousDays = new ArrayList<>();
        int previousCreated = 0;
        int previousClosed = 0;

        for (int i = 6; i >= 1; i--) {
            int created = 10 + i;
            int closed = 5 + i;
            previousCreated += created + 100;
            previousClosed += closed + 100;
            previousDays.add(glpiHistory(collectedAt.minusDays(i), created, closed));
            previousDays.add(glpiHistory(collectedAt.minusDays(i).plusHours(1), created + 100, closed + 100));
        }

        when(repository.findByCollectedAtAfterOrderByCollectedAtAsc(any()))
                .thenReturn(previousDays);

        SimulationConsistencyService service = new SimulationConsistencyService(
                mock(ArubaSummaryService.class),
                mock(GlobalKpiCalculationService.class),
                new KpiScoringService(new KpiProperties()),
                repository);
        GlpiSummary current = new GlpiSummary();
        current.setCreatedToday(20);
        current.setClosedToday(10);

        SimulationConsistencyService.WeeklyTicketActivity activity =
                service.buildWeeklyActivity(current, collectedAt);

        assertThat(activity.createdThisWeek()).isEqualTo(previousCreated + activity.createdToday());
        assertThat(activity.closedThisWeek()).isEqualTo(previousClosed + activity.closedToday());
        assertThat(activity.openTickets()).isEqualTo(activity.createdThisWeek() - activity.closedThisWeek());
    }

    private SimulationConsistencyService serviceWithEmptyGlpiHistory() {
        GlpiMetricsHistoryRepository repository = mock(GlpiMetricsHistoryRepository.class);

        when(repository.findByCollectedAtAfterOrderByCollectedAtAsc(any()))
                .thenReturn(List.of());

        return new SimulationConsistencyService(
                mock(ArubaSummaryService.class),
                mock(GlobalKpiCalculationService.class),
                new KpiScoringService(new KpiProperties()),
                repository);
    }

    private GlpiSummary glpiWithWeeklyBacklog(int openTickets) {
        GlpiSummary glpi = new GlpiSummary();

        glpi.setCreatedToday(openTickets);
        glpi.setClosedToday(0);
        glpi.setArubaOpenTickets(2);
        glpi.setCitrixOpenTickets(3);
        glpi.setMicrosoft365OpenTickets(4);
        glpi.setCriticalOpenTickets(openTickets > 0 ? openTickets / 2 : 0);

        return glpi;
    }

    private int platformTicketTotal(GlpiSummary glpi) {
        return glpi.getArubaOpenTickets()
                + glpi.getCitrixOpenTickets()
                + glpi.getMicrosoft365OpenTickets();
    }

    private GlpiMetricsHistory glpiHistory(
            LocalDateTime collectedAt,
            int createdToday,
            int closedToday) {

        GlpiMetricsHistory history = new GlpiMetricsHistory();
        history.setCollectedAt(collectedAt);
        history.setCreatedToday(createdToday);
        history.setClosedToday(closedToday);
        return history;
    }
}
