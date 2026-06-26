package com.tfg.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.report.MonthlyDailySummaryDto;
import com.tfg.dashboard.dto.report.MonthlyKpiAverageDto;
import com.tfg.dashboard.dto.report.MonthlyPlatformAffectationDto;
import com.tfg.dashboard.dto.report.MonthlyReportContextDto;
import com.tfg.dashboard.dto.report.MonthlyReportSummaryDto;
import com.tfg.dashboard.dto.report.MonthlyWorstDayDto;
import com.tfg.dashboard.model.AnalysisSnapshot;
import com.tfg.dashboard.model.TransversalKpiHistory;
import com.tfg.dashboard.repository.AnalysisSnapshotRepository;
import com.tfg.dashboard.repository.TransversalKpiHistoryRepository;

/**
 * Construye un contexto mensual estructurado a partir de historicos ya
 * persistidos.
 *
 * No recalcula los KPIs del dashboard: prepara una fotografia agregada del
 * periodo para revisar tendencias, peores dias y hallazgos repetidos.
 */
@Service
public class MonthlyReportContextService {

    private static final String LAST_30_DAYS = "LAST_30_DAYS";
    private static final int LAST_30_DAYS_COUNT = 30;
    private static final int WORST_DAYS_LIMIT = 5;

    private final AnalysisSnapshotRepository analysisSnapshotRepository;
    private final TransversalKpiHistoryRepository transversalKpiHistoryRepository;
    private final KpiProperties kpiProperties;

    public MonthlyReportContextService(
            AnalysisSnapshotRepository analysisSnapshotRepository,
            TransversalKpiHistoryRepository transversalKpiHistoryRepository,
            KpiProperties kpiProperties) {

        this.analysisSnapshotRepository = analysisSnapshotRepository;
        this.transversalKpiHistoryRepository = transversalKpiHistoryRepository;
        this.kpiProperties = kpiProperties;
    }

    public MonthlyReportContextDto buildMonthlyContext(String period) {
        return buildMonthlyContext(period, false);
    }

    /**
     * Genera el contexto del periodo solicitado.
     *
     * includeGenerated permite decidir si los snapshots generados para pruebas
     * entran o no en el informe. Por defecto se excluyen para que el contexto
     * mensual represente solo historico operativo.
     */
    public MonthlyReportContextDto buildMonthlyContext(String period, boolean includeGenerated) {

        String normalizedPeriod = normalizePeriod(period);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(LAST_30_DAYS_COUNT - 1L);
        LocalDateTime startBoundary = startDate.atStartOfDay().minusNanos(1);

        List<AnalysisSnapshot> allSnapshots =
                analysisSnapshotRepository.findByTimestampAfterOrderByTimestampAsc(startBoundary)
                        .stream()
                        .filter(snapshot -> isInsidePeriod(snapshot.getTimestamp(), startDate, endDate))
                        .toList();
        int excludedGeneratedScenarioSnapshots =
                includeGenerated
                        ? 0
                        : (int) allSnapshots.stream()
                                .filter(snapshot -> Boolean.TRUE.equals(snapshot.isGeneratedScenario()))
                                .count();
        List<AnalysisSnapshot> snapshots =
                includeGenerated
                        ? allSnapshots
                        : allSnapshots.stream()
                                .filter(snapshot -> !Boolean.TRUE.equals(snapshot.isGeneratedScenario()))
                                .toList();

        List<TransversalKpiHistory> transversalHistory =
                transversalKpiHistoryRepository.findByCollectedAtAfterOrderByCollectedAtAsc(startBoundary)
                        .stream()
                        .filter(history -> isInsidePeriod(history.getCollectedAt(), startDate, endDate))
                        .toList();

        Map<LocalDate, List<AnalysisSnapshot>> snapshotsByDay =
                snapshots.stream()
                        .filter(snapshot -> snapshot.getTimestamp() != null)
                        .collect(Collectors.groupingBy(
                                snapshot -> snapshot.getTimestamp().toLocalDate(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        List<MonthlyDailySummaryDto> dailySummaries =
                IntStream.range(0, LAST_30_DAYS_COUNT)
                        .mapToObj(dayOffset -> buildDailySummary(
                                startDate.plusDays(dayOffset),
                                snapshotsByDay.getOrDefault(
                                        startDate.plusDays(dayOffset),
                                        List.of())))
                        .toList();

        MonthlyReportSummaryDto monthlySummary =
                buildMonthlySummary(
                        normalizedPeriod,
                        startDate,
                        endDate,
                        snapshots,
                        dailySummaries,
                        excludedGeneratedScenarioSnapshots,
                        includeGenerated);

        List<MonthlyKpiAverageDto> transversalAverages =
                buildTransversalKpiAverages(transversalHistory, dailySummaries);

        List<MonthlyPlatformAffectationDto> platformAffectations =
                buildPlatformAffectations(dailySummaries);

        List<MonthlyWorstDayDto> worstDays =
                buildWorstDays(dailySummaries);

        List<String> frequentFindings =
                buildFrequentFindings(dailySummaries);

        List<String> limitations =
                buildLimitations(
                        snapshots,
                        transversalHistory,
                        monthlySummary,
                        excludedGeneratedScenarioSnapshots,
                        includeGenerated,
                        dailySummaries);

        return new MonthlyReportContextDto(
                normalizedPeriod,
                monthlySummary,
                transversalAverages,
                platformAffectations,
                dailySummaries,
                worstDays,
                frequentFindings,
                limitations);
    }

    private String normalizePeriod(String period) {

        String normalized =
                period == null || period.isBlank()
                        ? LAST_30_DAYS
                        : period.trim().toUpperCase(Locale.ROOT);

        if (!LAST_30_DAYS.equals(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Periodo no soportado: " + period);
        }

        return normalized;
    }

    private boolean isInsidePeriod(LocalDateTime timestamp, LocalDate startDate, LocalDate endDate) {

        if (timestamp == null) {
            return false;
        }

        LocalDate date = timestamp.toLocalDate();
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private MonthlyDailySummaryDto buildDailySummary(
            LocalDate date,
            List<AnalysisSnapshot> snapshots) {

        if (snapshots.isEmpty()) {
            return new MonthlyDailySummaryDto(
                    date,
                    0,
                    false,
                    false,
                    null,
                    "NO_DATA",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "NO_DATA",
                    List.of("Sin snapshots disponibles para este dia."));
        }

        Integer globalStatusValue = roundedAverage(snapshots, AnalysisSnapshot::getGlobalStatus);
        Integer arubaAffectation = roundedAverage(snapshots, AnalysisSnapshot::getArubaHealth);
        Integer citrixAffectation = roundedAverage(snapshots, AnalysisSnapshot::getCitrixHealth);
        Integer microsoft365Affectation =
                roundedAverage(snapshots, AnalysisSnapshot::getMicrosoft365Health);
        Integer glpiAffectation = roundedAverage(snapshots, AnalysisSnapshot::getGlpiHealth);
        Integer glpiOperationalPressure =
                roundedAverage(snapshots, AnalysisSnapshot::getGlpiOperationalPressure);
        Integer technicalDegradation =
                roundedAverage(snapshots, AnalysisSnapshot::getTechnicalDegradation);
        Integer userImpact = roundedAverage(snapshots, AnalysisSnapshot::getUserImpact);
        Integer affectedServicesPercent =
                roundedAverage(snapshots, AnalysisSnapshot::getAffectedServicesPercent);

        Integer severityScore =
                maxValue(
                        globalStatusValue,
                        arubaAffectation,
                        citrixAffectation,
                        microsoft365Affectation,
                        glpiAffectation,
                        glpiOperationalPressure,
                        technicalDegradation,
                        userImpact,
                        affectedServicesPercent);

        boolean containsGeneratedScenario =
                snapshots.stream()
                        .anyMatch(snapshot -> Boolean.TRUE.equals(snapshot.isGeneratedScenario()));

        List<String> findings =
                buildDailyFindings(
                        snapshots,
                        arubaAffectation,
                        citrixAffectation,
                        microsoft365Affectation,
                        glpiAffectation,
                        glpiOperationalPressure,
                        technicalDegradation,
                        userImpact,
                        affectedServicesPercent,
                        containsGeneratedScenario);

        return new MonthlyDailySummaryDto(
                date,
                snapshots.size(),
                true,
                containsGeneratedScenario,
                globalStatusValue,
                statusFromAffection(globalStatusValue),
                arubaAffectation,
                citrixAffectation,
                microsoft365Affectation,
                glpiAffectation,
                glpiOperationalPressure,
                technicalDegradation,
                userImpact,
                affectedServicesPercent,
                statusFromAffection(severityScore),
                findings);
    }

    private List<String> buildDailyFindings(
            List<AnalysisSnapshot> snapshots,
            Integer arubaAffectation,
            Integer citrixAffectation,
            Integer microsoft365Affectation,
            Integer glpiAffectation,
            Integer glpiOperationalPressure,
            Integer technicalDegradation,
            Integer userImpact,
            Integer affectedServicesPercent,
            boolean containsGeneratedScenario) {

        List<String> findings = new ArrayList<>();

        addConcreteArubaFindings(findings, snapshots);
        addConcreteCitrixFindings(findings, snapshots);
        addConcreteMicrosoft365Findings(findings, snapshots);
        addConcreteGlpiFindings(findings, snapshots);

        if (isRed(glpiOperationalPressure)) {
            findings.add("Presion operativa GLPI alta.");
        } else if (isYellow(glpiOperationalPressure)) {
            findings.add("Presion operativa GLPI en advertencia.");
        }

        if (isRed(technicalDegradation)) {
            findings.add("Degradacion tecnica alta.");
        }

        if (isRed(userImpact)) {
            findings.add("Impacto en usuarios alto.");
        }

        if (isRed(affectedServicesPercent)) {
            findings.add("Servicios afectados en nivel alto.");
        }

        if (findings.stream().noneMatch(this::isConcreteFinding)) {
            addPlatformFinding(findings, "Aruba", arubaAffectation);
            addPlatformFinding(findings, "Citrix", citrixAffectation);
            addPlatformFinding(findings, "Microsoft 365", microsoft365Affectation);
            addPlatformFinding(findings, "GLPI", glpiAffectation);
        }

        if (containsGeneratedScenario) {
            findings.add("Incluye escenarios generados.");
        }

        if (findings.isEmpty()) {
            findings.add("Sin hallazgos relevantes.");
        }

        return findings;
    }

    private void addConcreteArubaFindings(List<String> findings, List<AnalysisSnapshot> snapshots) {

        Integer wifiClients =
                roundedAverage(snapshots, AnalysisSnapshot::getArubaWifiClients);

        if (wifiClients != null && wifiClients == 0) {
            findings.add("Clientes WiFi Aruba a 0.");
        }
    }

    private void addConcreteCitrixFindings(List<String> findings, List<AnalysisSnapshot> snapshots) {

        Integer logonDuration =
                roundedAverage(snapshots, AnalysisSnapshot::getCitrixAverageLogonDurationSeconds);
        Integer failedLogons =
                roundedAverage(snapshots, AnalysisSnapshot::getCitrixFailedLogons);

        if (logonDuration != null
                && logonDuration > kpiProperties.getCitrix().getLogonDurationRedAboveSeconds()) {
            findings.add("Duracion media de logon Citrix critica.");
        } else if (logonDuration != null
                && logonDuration > kpiProperties.getCitrix().getLogonDurationYellowAboveSeconds()) {
            findings.add("Duracion media de logon Citrix elevada.");
        }

        if (failedLogons != null
                && failedLogons > kpiProperties.getCitrix().getFailedLogonsRedAbove()) {
            findings.add("Errores de inicio Citrix criticos.");
        } else if (failedLogons != null
                && failedLogons > kpiProperties.getCitrix().getFailedLogonsYellowAbove()) {
            findings.add("Errores de inicio Citrix elevados.");
        }
    }

    private void addConcreteMicrosoft365Findings(
            List<String> findings,
            List<AnalysisSnapshot> snapshots) {

        Integer nonCompliantDevices =
                roundedAverage(snapshots, AnalysisSnapshot::getMicrosoft365NonCompliantDevices);

        if (nonCompliantDevices != null
                && nonCompliantDevices > kpiProperties.getMicrosoft365().getNonCompliantDevicesRedAbove()) {
            findings.add("Equipos no conformes Microsoft 365 criticos.");
        } else if (nonCompliantDevices != null
                && nonCompliantDevices > kpiProperties.getMicrosoft365().getNonCompliantDevicesYellowAbove()) {
            findings.add("Equipos no conformes Microsoft 365 elevados.");
        }
    }

    private void addConcreteGlpiFindings(List<String> findings, List<AnalysisSnapshot> snapshots) {

        Integer openTickets =
                roundedAverage(snapshots, AnalysisSnapshot::getGlpiOpenTickets);

        if (openTickets != null
                && openTickets >= kpiProperties.getGlpi().getOpenTicketsRedMin()) {
            findings.add("Tickets abiertos GLPI en nivel critico.");
        } else if (openTickets != null
                && openTickets >= kpiProperties.getGlpi().getOpenTicketsYellowMin()) {
            findings.add("Tickets abiertos GLPI elevados.");
        }
    }

    private boolean isConcreteFinding(String finding) {

        return finding.contains("Clientes WiFi")
                || finding.contains("logon Citrix")
                || finding.contains("Errores de inicio")
                || finding.contains("Equipos no conformes")
                || finding.contains("Tickets abiertos GLPI")
                || finding.contains("Presion operativa GLPI")
                || finding.contains("Degradacion tecnica")
                || finding.contains("Impacto en usuarios")
                || finding.contains("Servicios afectados");
    }

    private void addPlatformFinding(List<String> findings, String platform, Integer value) {

        if (isRed(value)) {
            findings.add("Afeccion critica en " + platform + ".");
            return;
        }

        if (isYellow(value)) {
            findings.add("Advertencia en " + platform + ".");
        }
    }

    private MonthlyReportSummaryDto buildMonthlySummary(
            String period,
            LocalDate startDate,
            LocalDate endDate,
            List<AnalysisSnapshot> snapshots,
            List<MonthlyDailySummaryDto> dailySummaries,
            int excludedGeneratedScenarioSnapshots,
            boolean includeGenerated) {

        int daysWithData =
                (int) dailySummaries.stream()
                        .filter(day -> Boolean.TRUE.equals(day.hasData()))
                        .count();
        int generatedScenarioSnapshots =
                (int) snapshots.stream()
                        .filter(snapshot -> Boolean.TRUE.equals(snapshot.isGeneratedScenario()))
                        .count();
        Double globalStatusValue =
                averageFromDailySummaries(
                        dailySummaries,
                        MonthlyDailySummaryDto::globalStatusValue);
        String globalStatusLevel =
                statusFromAffection(globalStatusValue);
        String worstDailyStatus =
                worstStatusFromDailySummaries(dailySummaries);
        String summaryText =
                buildSummaryText(
                        period,
                        daysWithData,
                        snapshots.size(),
                        generatedScenarioSnapshots,
                        excludedGeneratedScenarioSnapshots,
                        includeGenerated,
                        globalStatusValue,
                        globalStatusLevel,
                        worstDailyStatus);

        return new MonthlyReportSummaryDto(
                startDate,
                endDate,
                LAST_30_DAYS_COUNT,
                daysWithData,
                snapshots.size(),
                generatedScenarioSnapshots,
                excludedGeneratedScenarioSnapshots,
                includeGenerated,
                globalStatusValue,
                globalStatusLevel,
                worstDailyStatus,
                summaryText);
    }

    private String buildSummaryText(
            String period,
            int daysWithData,
            int totalSnapshots,
            int generatedScenarioSnapshots,
            int excludedGeneratedScenarioSnapshots,
            boolean includeGenerated,
            Double globalStatusValue,
            String globalStatusLevel,
            String worstDailyStatus) {

        if (totalSnapshots == 0) {
            return "No hay snapshots suficientes en " + period
                    + " para elaborar un resumen mensual fiable.";
        }

        String generatedText = "";

        if (includeGenerated && generatedScenarioSnapshots > 0) {
            generatedText =
                    " Incluye " + generatedScenarioSnapshots + " snapshots generados.";
        } else if (!includeGenerated && excludedGeneratedScenarioSnapshots > 0) {
            generatedText =
                    " Se han excluido " + excludedGeneratedScenarioSnapshots
                            + " snapshots generados.";
        }

        String globalText =
                globalStatusValue == null
                        ? "sin dato global medio"
                        : "estado global medio "
                                + globalStatusValue
                                + "% ("
                                + globalStatusLevel
                                + ")";

        return "El periodo " + period
                + " contiene " + totalSnapshots
                + " snapshots distribuidos en " + daysWithData
                + " de " + LAST_30_DAYS_COUNT
                + " dias. Presenta " + globalText
                + " y peor estado diario " + worstDailyStatus
                + "."
                + generatedText;
    }

    private List<MonthlyKpiAverageDto> buildTransversalKpiAverages(
            List<TransversalKpiHistory> transversalHistory,
            List<MonthlyDailySummaryDto> dailySummaries) {

        Map<String, MonthlyKpiAverageDto> averages = new LinkedHashMap<>();

        transversalHistory.stream()
                .filter(history -> history.getKpiCode() != null)
                .filter(history -> history.getValue() != null)
                .filter(history -> history.getCollectedAt() != null)
                .collect(Collectors.groupingBy(
                        TransversalKpiHistory::getKpiCode,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .forEach((code, histories) -> averages.put(
                        code,
                        buildHistoryAverage(code, histories)));

        addDailySummaryAverageIfMissing(
                averages,
                "global_status",
                "Estado global",
                "%",
                dailySummaries,
                MonthlyDailySummaryDto::globalStatusValue);
        addDailySummaryAverageIfMissing(
                averages,
                "glpi_operational_pressure",
                "Presion operativa GLPI",
                "%",
                dailySummaries,
                MonthlyDailySummaryDto::glpiOperationalPressure);
        addDailySummaryAverageIfMissing(
                averages,
                "technical_degradation",
                "Degradacion tecnica",
                "%",
                dailySummaries,
                MonthlyDailySummaryDto::technicalDegradation);
        addDailySummaryAverageIfMissing(
                averages,
                "user_impact",
                "Impacto en usuarios",
                "%",
                dailySummaries,
                MonthlyDailySummaryDto::userImpact);
        addDailySummaryAverageIfMissing(
                averages,
                "affected_services",
                "Servicios afectados",
                "%",
                dailySummaries,
                MonthlyDailySummaryDto::affectedServicesPercent);

        return List.copyOf(averages.values());
    }

    private MonthlyKpiAverageDto buildHistoryAverage(
            String code,
            List<TransversalKpiHistory> histories) {

        Map<LocalDate, Double> dailyValues =
                histories.stream()
                        .collect(Collectors.groupingBy(
                                history -> history.getCollectedAt().toLocalDate(),
                                LinkedHashMap::new,
                                Collectors.averagingDouble(TransversalKpiHistory::getValue)));
        List<Double> values =
                List.copyOf(dailyValues.values());
        TransversalKpiHistory first = histories.get(0);

        return new MonthlyKpiAverageDto(
                code,
                first.getKpiName(),
                roundOneDecimal(values.stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                roundOneDecimal(values.stream().mapToDouble(Double::doubleValue).max().orElse(0)),
                values.size(),
                first.getUnit());
    }

    private void addDailySummaryAverageIfMissing(
            Map<String, MonthlyKpiAverageDto> averages,
            String code,
            String name,
            String unit,
            List<MonthlyDailySummaryDto> dailySummaries,
            Function<MonthlyDailySummaryDto, Integer> extractor) {

        if (averages.containsKey(code)) {
            return;
        }

        List<Integer> values =
                dailySummaries.stream()
                        .filter(day -> Boolean.TRUE.equals(day.hasData()))
                        .map(extractor)
                        .filter(Objects::nonNull)
                        .toList();

        if (values.isEmpty()) {
            return;
        }

        averages.put(
                code,
                new MonthlyKpiAverageDto(
                        code,
                        name,
                        roundOneDecimal(values.stream().mapToInt(Integer::intValue).average().orElse(0)),
                        (double) values.stream().mapToInt(Integer::intValue).max().orElse(0),
                        values.size(),
                        unit));
    }

    private List<MonthlyPlatformAffectationDto> buildPlatformAffectations(
            List<MonthlyDailySummaryDto> dailySummaries) {

        return List.of(
                buildPlatformAffectation(
                        "Aruba",
                        dailySummaries.stream().map(MonthlyDailySummaryDto::arubaAffectation).toList()),
                buildPlatformAffectation(
                        "Citrix",
                        dailySummaries.stream().map(MonthlyDailySummaryDto::citrixAffectation).toList()),
                buildPlatformAffectation(
                        "Microsoft 365",
                        dailySummaries.stream().map(MonthlyDailySummaryDto::microsoft365Affectation).toList()),
                buildPlatformAffectation(
                        "GLPI",
                        dailySummaries.stream().map(MonthlyDailySummaryDto::glpiAffectation).toList()));
    }

    private MonthlyPlatformAffectationDto buildPlatformAffectation(
            String platform,
            List<Integer> dailyValues) {

        List<Integer> availableDailyValues =
                dailyValues.stream()
                        .filter(Objects::nonNull)
                        .toList();

        return new MonthlyPlatformAffectationDto(
                platform,
                availableDailyValues.isEmpty()
                        ? null
                        : roundOneDecimal(
                                availableDailyValues.stream()
                                        .mapToInt(Integer::intValue)
                                        .average()
                                        .orElse(0)),
                availableDailyValues.stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .stream()
                        .boxed()
                        .findFirst()
                        .orElse(null),
                countStatus(availableDailyValues, "GREEN"),
                countStatus(availableDailyValues, "YELLOW"),
                countStatus(availableDailyValues, "RED"),
                availableDailyValues.size());
    }

    private List<MonthlyWorstDayDto> buildWorstDays(List<MonthlyDailySummaryDto> dailySummaries) {

        return dailySummaries.stream()
                .filter(day -> Boolean.TRUE.equals(day.hasData()))
                .map(day -> new MonthlyWorstDayDto(
                        day.date(),
                        dailySeverity(day),
                        buildWorstDayReason(day),
                        day.containsGeneratedScenario()))
                .sorted(Comparator.comparing(
                                MonthlyWorstDayDto::severityScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MonthlyWorstDayDto::date))
                .limit(WORST_DAYS_LIMIT)
                .toList();
    }

    private String buildWorstDayReason(MonthlyDailySummaryDto day) {

        List<String> parts = new ArrayList<>();
        parts.add("Peor estado diario " + day.worstDailyStatus());

        if (day.globalStatusValue() != null) {
            parts.add(
                    "estado global="
                            + day.globalStatusValue()
                            + "% ("
                            + day.globalStatusLevel()
                            + ")");
        }

        if (day.affectedServicesPercent() != null) {
            parts.add("servicios afectados=" + day.affectedServicesPercent() + "%");
        }

        List<String> platforms = affectedPlatforms(day);

        if (!platforms.isEmpty()) {
            parts.add("plataformas afectadas: " + String.join(", ", platforms));
        }

        if (Boolean.TRUE.equals(day.containsGeneratedScenario())) {
            parts.add("incluye snapshots generados");
        }

        return String.join("; ", parts) + ".";
    }

    private List<String> affectedPlatforms(MonthlyDailySummaryDto day) {

        List<String> platforms = new ArrayList<>();

        if (isYellowOrRed(day.arubaAffectation())) {
            platforms.add("Aruba");
        }

        if (isYellowOrRed(day.citrixAffectation())) {
            platforms.add("Citrix");
        }

        if (isYellowOrRed(day.microsoft365Affectation())) {
            platforms.add("Microsoft 365");
        }

        if (isYellowOrRed(day.glpiAffectation())) {
            platforms.add("GLPI");
        }

        return platforms;
    }

    private List<String> buildFrequentFindings(List<MonthlyDailySummaryDto> dailySummaries) {

        List<String> findings =
                dailySummaries.stream()
                        .filter(day -> Boolean.TRUE.equals(day.hasData()))
                        .flatMap(day -> day.findings().stream())
                        .filter(this::isConcreteFinding)
                        .collect(Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(5)
                        .map(entry -> entry.getKey() + " (" + entry.getValue() + " dias)")
                        .toList();

        if (findings.isEmpty()) {
            return List.of("No hay hallazgos concretos repetidos en el periodo.");
        }

        return findings;
    }

    private List<String> buildLimitations(
            List<AnalysisSnapshot> snapshots,
            List<TransversalKpiHistory> transversalHistory,
            MonthlyReportSummaryDto monthlySummary,
            int excludedGeneratedScenarioSnapshots,
            boolean includeGenerated,
            List<MonthlyDailySummaryDto> dailySummaries) {

        List<String> limitations = new ArrayList<>();

        limitations.add(
                "El contexto mensual solo agrega KPIs e historicos calculados por el sistema.");
        limitations.add(
                "El contexto mensual no demuestra causalidad directa entre plataformas; las relaciones deben interpretarse como senales operativas o co-ocurrencias.");
        limitations.add(
                "Los resultados dependen de la cobertura y calidad de los snapshots e historicos persistidos.");

        if (monthlySummary.daysWithData() < LAST_30_DAYS_COUNT) {
            limitations.add(
                    "Hay " + (LAST_30_DAYS_COUNT - monthlySummary.daysWithData())
                            + " dias sin snapshots en el periodo.");
        }

        int generatedScenarios =
                (int) snapshots.stream()
                        .filter(snapshot -> Boolean.TRUE.equals(snapshot.isGeneratedScenario()))
                        .count();

        if (includeGenerated && generatedScenarios > 0) {
            limitations.add(
                    "El periodo incluye " + generatedScenarios
                            + " snapshots generados; deben diferenciarse de datos reales.");
        }

        if (!includeGenerated && excludedGeneratedScenarioSnapshots > 0) {
            limitations.add(
                    "Se han excluido " + excludedGeneratedScenarioSnapshots
                            + " snapshots generados del banco de pruebas.");
        }

        if (transversalHistory.isEmpty()) {
            limitations.add(
                    "No hay muestras en el historico transversal para el periodo; algunas medias se derivan de snapshots de analisis si existen.");
        }

        if (snapshots.isEmpty()) {
            limitations.add(
                    "No hay snapshots de analisis suficientes para detectar peores dias o hallazgos frecuentes.");
        }

        boolean hasConcreteFindings =
                dailySummaries.stream()
                        .flatMap(day -> day.findings().stream())
                        .anyMatch(this::isConcreteFinding);

        if (!hasConcreteFindings && !snapshots.isEmpty()) {
            limitations.add(
                    "Los snapshots disponibles no contienen detalle suficiente para listar hallazgos concretos por indicador.");
        }

        return limitations;
    }

    private Integer roundedAverage(
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> extractor) {

        List<Integer> values =
                snapshots.stream()
                        .map(extractor)
                        .filter(Objects::nonNull)
                        .toList();

        return roundedAverage(values);
    }

    private Integer roundedAverage(List<Integer> values) {

        if (values.isEmpty()) {
            return null;
        }

        return (int) Math.round(
                values.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0));
    }

    private Double averageFromDailySummaries(
            List<MonthlyDailySummaryDto> dailySummaries,
            Function<MonthlyDailySummaryDto, Integer> extractor) {

        List<Integer> values =
                dailySummaries.stream()
                        .filter(day -> Boolean.TRUE.equals(day.hasData()))
                        .map(extractor)
                        .filter(Objects::nonNull)
                        .toList();

        if (values.isEmpty()) {
            return null;
        }

        return roundOneDecimal(
                values.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0));
    }

    private Double roundOneDecimal(double value) {

        return Math.round(value * 10.0) / 10.0;
    }

    private Integer maxValue(Integer... values) {

        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer dailySeverity(MonthlyDailySummaryDto day) {

        return maxValue(
                day.globalStatusValue(),
                day.arubaAffectation(),
                day.citrixAffectation(),
                day.microsoft365Affectation(),
                day.glpiAffectation(),
                day.glpiOperationalPressure(),
                day.technicalDegradation(),
                day.userImpact(),
                day.affectedServicesPercent());
    }

    private String worstStatusFromDailySummaries(List<MonthlyDailySummaryDto> dailySummaries) {

        return dailySummaries.stream()
                .map(this::dailySeverity)
                .filter(Objects::nonNull)
                .map(this::statusFromAffection)
                .max(Comparator.comparingInt(this::statusSeverity))
                .orElse("NO_DATA");
    }

    private int countStatus(List<Integer> values, String status) {

        return (int) values.stream()
                .filter(value -> status.equals(statusFromAffection(value)))
                .count();
    }

    private boolean isYellow(Integer value) {

        return value != null
                && value >= kpiProperties.getStatus().getYellowMin()
                && value < kpiProperties.getStatus().getRedMin();
    }

    private boolean isRed(Integer value) {

        return value != null
                && value >= kpiProperties.getStatus().getRedMin();
    }

    private boolean isYellowOrRed(Integer value) {

        return value != null
                && value >= kpiProperties.getStatus().getYellowMin();
    }

    private String statusFromAffection(Integer value) {

        if (value == null) {
            return "NO_DATA";
        }

        return statusFromAffection(value.doubleValue());
    }

    private String statusFromAffection(Double value) {

        if (value == null) {
            return "NO_DATA";
        }

        if (value >= kpiProperties.getStatus().getRedMin()) {
            return "RED";
        }

        if (value >= kpiProperties.getStatus().getYellowMin()) {
            return "YELLOW";
        }

        return "GREEN";
    }

    private int statusSeverity(String status) {

        return switch (status) {
            case "RED" -> 3;
            case "YELLOW" -> 2;
            case "GREEN" -> 1;
            default -> 0;
        };
    }
}
