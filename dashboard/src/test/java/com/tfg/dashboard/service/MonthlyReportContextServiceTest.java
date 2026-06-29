package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.report.MonthlyDailySummaryDto;
import com.tfg.dashboard.dto.report.MonthlyReportContextDto;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.model.TransversalKpiHistory;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;
import com.tfg.dashboard.repository.TransversalKpiHistoryRepository;

@ExtendWith(MockitoExtension.class)
class MonthlyReportContextServiceTest {

    @Mock
    private AnalysisSnapshotRepository analysisSnapshotRepository;

    @Mock
    private TransversalKpiHistoryRepository transversalKpiHistoryRepository;

    private MonthlyReportContextService service;

    @BeforeEach
    void setUp() {
        service =
                new MonthlyReportContextService(
                        analysisSnapshotRepository,
                        transversalKpiHistoryRepository,
                        new KpiProperties());
    }

    @Test
    void excludesGeneratedScenariosByDefault() {

        LocalDate today = LocalDate.now();

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        snapshot(today.minusDays(1), 8, 30, 20, 30, 10, false),
                        snapshot(today.minusDays(1), 12, 90, 90, 90, 80, true)
                ));
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        MonthlyReportContextDto context =
                service.buildMonthlyContext("LAST_30_DAYS");

        assertThat(context.monthlySummary().includeGenerated())
                .isFalse();
        assertThat(context.monthlySummary().totalSnapshots())
                .isEqualTo(1);
        assertThat(context.monthlySummary().generatedScenarioSnapshots())
                .isZero();
        assertThat(context.monthlySummary().excludedGeneratedScenarioSnapshots())
                .isEqualTo(1);
        assertThat(context.monthlySummary().globalStatusValue())
                .isEqualTo(30.0);
        assertThat(context.limitations())
                .anyMatch(limitation -> limitation.contains("Se han excluido 1 snapshots generados"));
    }

    @Test
    void includesGeneratedScenariosWhenRequestedAndMarksThem() {

        LocalDate today = LocalDate.now();

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        snapshot(today.minusDays(1), 8, 30, 20, 30, 10, false),
                        snapshot(today.minusDays(1), 12, 90, 90, 90, 80, true)
                ));
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        MonthlyReportContextDto context =
                service.buildMonthlyContext("LAST_30_DAYS", true);

        assertThat(context.monthlySummary().includeGenerated())
                .isTrue();
        assertThat(context.monthlySummary().totalSnapshots())
                .isEqualTo(2);
        assertThat(context.monthlySummary().generatedScenarioSnapshots())
                .isEqualTo(1);
        assertThat(context.monthlySummary().summaryText())
                .contains("Incluye 1 snapshots generados");
        assertThat(context.dailySummaries())
                .filteredOn(day -> Boolean.TRUE.equals(day.containsGeneratedScenario()))
                .hasSize(1);
        assertThat(context.limitations())
                .anyMatch(limitation -> limitation.contains("incluye 1 snapshots generados"));
    }

    @Test
    void separatesGlobalStatusLevelFromWorstDailyStatus() {

        LocalDate today = LocalDate.now();

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        snapshot(today.minusDays(1), 8, 48, 20, 80, 10, false)
                ));
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        MonthlyReportContextDto context =
                service.buildMonthlyContext("LAST_30_DAYS");

        MonthlyDailySummaryDto day =
                context.dailySummaries().stream()
                        .filter(MonthlyDailySummaryDto::hasData)
                        .findFirst()
                        .orElseThrow();

        assertThat(day.globalStatusValue())
                .isEqualTo(48);
        assertThat(day.globalStatusLevel())
                .isEqualTo("YELLOW");
        assertThat(day.worstDailyStatus())
                .isEqualTo("RED");
        assertThat(context.monthlySummary().globalStatusValue())
                .isEqualTo(48.0);
        assertThat(context.monthlySummary().globalStatusLevel())
                .isEqualTo("YELLOW");
        assertThat(context.monthlySummary().worstDailyStatus())
                .isEqualTo("RED");
    }

    @Test
    void monthlyAveragesUseDailySummariesInsteadOfRawSnapshotCount() {

        LocalDate today = LocalDate.now();

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        snapshot(today.minusDays(2), 0, 100, 100, 100, 100, false),
                        snapshot(today.minusDays(2), 6, 100, 100, 100, 100, false),
                        snapshot(today.minusDays(2), 12, 100, 100, 100, 100, false),
                        snapshot(today.minusDays(2), 18, 100, 100, 100, 100, false),
                        snapshot(today.minusDays(1), 8, 0, 0, 0, 0, false)
                ));
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        MonthlyReportContextDto context =
                service.buildMonthlyContext("LAST_30_DAYS");

        assertThat(context.monthlySummary().globalStatusValue())
                .isEqualTo(50.0);
        assertThat(context.transversalKpiAverages())
                .filteredOn(kpi -> "global_status".equals(kpi.code()))
                .singleElement()
                .satisfies(kpi -> {
                    assertThat(kpi.average()).isEqualTo(50.0);
                    assertThat(kpi.samples()).isEqualTo(2);
                });
        assertThat(context.platformAffectations())
                .filteredOn(platform -> "Aruba".equals(platform.platform()))
                .singleElement()
                .satisfies(platform -> assertThat(platform.averageAffectation()).isEqualTo(50.0));
    }

    @Test
    void transversalHistoryAveragesAreGroupedByDay() {

        LocalDate today = LocalDate.now();

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        history("operational_pressure", "Presion operativa", 100.0, today.minusDays(2).atTime(0, 0)),
                        history("operational_pressure", "Presion operativa", 100.0, today.minusDays(2).atTime(12, 0)),
                        history("operational_pressure", "Presion operativa", 0.0, today.minusDays(1).atTime(12, 0))
                ));

        MonthlyReportContextDto context =
                service.buildMonthlyContext("LAST_30_DAYS");

        assertThat(context.transversalKpiAverages())
                .filteredOn(kpi -> "operational_pressure".equals(kpi.code()))
                .singleElement()
                .satisfies(kpi -> {
                    assertThat(kpi.average()).isEqualTo(50.0);
                    assertThat(kpi.samples()).isEqualTo(2);
                });
    }

    @Test
    void dailyDtoDoesNotExposeHealthFieldsForAffectationValues() {

        List<String> componentNames =
                Arrays.stream(MonthlyDailySummaryDto.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList();

        assertThat(componentNames)
                .doesNotContain(
                        "arubaHealth",
                        "citrixHealth",
                        "microsoft365Health",
                        "glpiHealth");
        assertThat(componentNames)
                .contains(
                        "arubaAffectation",
                        "citrixAffectation",
                        "microsoft365Affectation",
                        "glpiAffectation");
    }

    @Test
    void returnsExplicitLimitationsWhenThereIsNoMonthlyData() {

        when(analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        MonthlyReportContextDto context =
                service.buildMonthlyContext(null);

        assertThat(context.period())
                .isEqualTo("LAST_30_DAYS");
        assertThat(context.monthlySummary().globalStatusLevel())
                .isEqualTo("NO_DATA");
        assertThat(context.monthlySummary().worstDailyStatus())
                .isEqualTo("NO_DATA");
        assertThat(context.monthlySummary().summaryText())
                .contains("No hay snapshots suficientes");
        assertThat(context.dailySummaries())
                .hasSize(30)
                .allSatisfy(day -> assertThat(day.hasData()).isFalse());
        assertThat(context.limitations())
                .anyMatch(limitation -> limitation.contains("No hay snapshots"));
    }

    @Test
    void rejectsUnsupportedPeriodsWithBadRequest() {

        assertThatThrownBy(() -> service.buildMonthlyContext("LAST_90_DAYS"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private AnalysisSnapshot snapshot(
            LocalDate date,
            int hour,
            int globalStatus,
            int aruba,
            int citrix,
            int glpiPressure,
            boolean generatedScenario) {

        AnalysisSnapshot snapshot =
                new AnalysisSnapshot();
        snapshot.setTimestamp(date.atTime(hour, 0));
        snapshot.setGlobalStatus(globalStatus);
        snapshot.setArubaHealth(aruba);
        snapshot.setCitrixHealth(citrix);
        snapshot.setMicrosoft365Health(25);
        snapshot.setGlpiHealth(30);
        snapshot.setGlpiOperationalPressure(glpiPressure);
        snapshot.setTechnicalDegradation(Math.max(aruba, citrix));
        snapshot.setUserImpact(glpiPressure);
        snapshot.setAffectedServicesPercent(citrix >= 67 ? 50 : 25);
        snapshot.setArubaWifiClients(aruba >= 67 ? 0 : 120);
        snapshot.setCitrixAverageLogonDurationSeconds(citrix >= 67 ? 70 : 15);
        snapshot.setCitrixFailedLogons(citrix >= 67 ? 25 : 0);
        snapshot.setMicrosoft365NonCompliantDevices(20);
        snapshot.setGlpiOpenTickets(glpiPressure >= 67 ? 210 : 20);
        snapshot.setGeneratedScenario(generatedScenario);
        return snapshot;
    }

    private TransversalKpiHistory history(
            String code,
            String name,
            Double value,
            LocalDateTime collectedAt) {

        TransversalKpiHistory history =
                new TransversalKpiHistory();
        history.setKpiCode(code);
        history.setKpiName(name);
        history.setValue(value);
        history.setUnit("%");
        history.setCollectedAt(collectedAt);
        return history;
    }
}
