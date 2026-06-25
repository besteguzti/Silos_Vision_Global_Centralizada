package com.tfg.dashboard.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Prepara la evolución temporal conjunta del periodo de análisis.
 *
 * Agrega los snapshots por día para que escenarios de 30 o 90 días se lean como
 * una secuencia cronológica clara: normalidad, degradación, pico y recuperación.
 */
@Service
public class AnalysisTimelineService {

    /**
     * Genera puntos diarios en escala 0-100 a partir de snapshots persistidos.
     */
    public List<TimelinePointDto> buildPlatformEvolution(
            List<AnalysisSnapshot> snapshots
    ) {

        return snapshots.stream()
                .filter(snapshot -> snapshot.getTimestamp() != null)
                .collect(Collectors.groupingBy(snapshot -> snapshot.getTimestamp().toLocalDate()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> dailyPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TimelinePointDto dailyPoint(
            LocalDate day,
            List<AnalysisSnapshot> snapshots) {

        List<AnalysisSnapshot> orderedSnapshots =
                snapshots.stream()
                        .sorted(Comparator.comparing(AnalysisSnapshot::getTimestamp))
                        .toList();

        TimelinePointDto point = new TimelinePointDto();
        point.setTimestamp(day.atStartOfDay());
        point.setAruba(average(orderedSnapshots, AnalysisSnapshot::getArubaHealth));
        point.setCitrix(average(orderedSnapshots, AnalysisSnapshot::getCitrixHealth));
        point.setMicrosoft365(average(orderedSnapshots, AnalysisSnapshot::getMicrosoft365Health));
        point.setTechnicalDegradation(average(orderedSnapshots, AnalysisSnapshot::getTechnicalDegradation));
        point.setGlpiOperationalPressure(average(orderedSnapshots, AnalysisSnapshot::getGlpiOperationalPressure));
        point.setUserImpact(average(orderedSnapshots, AnalysisSnapshot::getUserImpact));
        point.setGlobalStatus(average(orderedSnapshots, AnalysisSnapshot::getGlobalStatus));
        point.setGeneratedScenario(
                orderedSnapshots.stream().anyMatch(snapshot -> Boolean.TRUE.equals(snapshot.isGeneratedScenario())));

        return point;
    }

    private Double average(
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> extractor) {

        return snapshots.stream()
                .map(extractor)
                .filter(value -> value != null)
                .mapToDouble(Integer::doubleValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }
}

