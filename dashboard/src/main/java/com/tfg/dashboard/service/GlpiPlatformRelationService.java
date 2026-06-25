package com.tfg.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.TechnicalPlatformRelationDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Calcula relaciónes operativas aparentes entre plataformas técnicas.
 *
 * El panel actual usa este servicio para construir la tabla de co-afección
 * técnica entre Aruba, Citrix y Microsoft 365. No interpreta causalidad:
 * solo compara si dos plataformas aparecen afectadas en los mismos snapshots.
 */
@Service
public class GlpiPlatformRelationService {

        private final KpiScoringService kpiScoringService;
        private final KpiProperties kpiProperties;

        public GlpiPlatformRelationService(KpiScoringService kpiScoringService, KpiProperties kpiProperties) {
                this.kpiScoringService = kpiScoringService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Calcula co-afección entre plataformas técnicas.
         */
        public List<TechnicalPlatformRelationDto> buildTechnicalRelations(List<AnalysisSnapshot> snapshots) {

                return List.of(
                                technicalRelation("Aruba", "Citrix", snapshots),
                                technicalRelation("Aruba", "Microsoft 365", snapshots),
                                technicalRelation("Citrix", "Aruba", snapshots),
                                technicalRelation("Citrix", "Microsoft 365", snapshots),
                                technicalRelation("Microsoft 365", "Aruba", snapshots),
                                technicalRelation("Microsoft 365", "Citrix", snapshots));
        }

        private TechnicalPlatformRelationDto technicalRelation(
                        String origin,
                        String target,
                        List<AnalysisSnapshot> snapshots) {

                List<AnalysisSnapshot> originAffected = snapshots.stream()
                                .filter(snapshot -> platformValue(snapshot, origin) >= kpiProperties.getStatus().getYellowMin())
                                .toList();

                List<AnalysisSnapshot> originNormal = snapshots.stream()
                                .filter(snapshot -> platformValue(snapshot, origin) < kpiProperties.getStatus().getYellowMin())
                                .toList();

                Integer cooccurrence = null;

                if (!originAffected.isEmpty()) {

                        long targetAlsoAffected = originAffected.stream()
                                        .filter(snapshot -> platformValue(
                                                        snapshot,
                                                        target) >= kpiProperties.getStatus().getYellowMin())
                                        .count();

                        cooccurrence = clampToInt(targetAlsoAffected * 100.0
                                        / originAffected.size());
                }

                Integer increase = null;

                if (!originAffected.isEmpty() && !originNormal.isEmpty()) {

                        increase = clampToInt(averagePlatform(originAffected, target) - averagePlatform(originNormal, target));
                }

                TechnicalPlatformRelationDto relation = new TechnicalPlatformRelationDto();

                relation.setOrigin(origin);
                relation.setTarget(target);
                relation.setRelation(origin + " -> " + target);
                relation.setCooccurrencePercentage(cooccurrence);
                relation.setAverageIncrease(increase);
                relation.setReading(kpiScoringService.relationReading(cooccurrence, increase));
                relation.setReadingStatus(relationStatus(relation.getReading()));
                relation.setOriginAffectedSnapshots(originAffected.size());
                relation.setOriginNormalSnapshots(originNormal.size());

                return relation;
        }

        private String relationStatus(String reading) {

                if ("Alta".equalsIgnoreCase(reading)) {

                        return KpiScoringService.RED;
                }

                if ("Moderada".equalsIgnoreCase(reading)) {

                        return KpiScoringService.YELLOW;
                }

                if ("Baja".equalsIgnoreCase(reading)) {

                        return KpiScoringService.GREEN;
                }

                return KpiScoringService.NO_DATA;
        }

        private double averagePlatform(List<AnalysisSnapshot> snapshots, String platform) {

                return snapshots.stream()
                                .mapToDouble(snapshot -> platformValue(snapshot, platform))
                                .average()
                                .orElse(0);
        }

        private double platformValue(AnalysisSnapshot snapshot, String platform) {

                if ("Citrix".equalsIgnoreCase(platform)) {

                        return safeInt(snapshot.getCitrixHealth());
                }

                if ("Microsoft 365".equalsIgnoreCase(platform)) {

                        return safeInt(snapshot.getMicrosoft365Health());
                }

                return safeInt(snapshot.getArubaHealth());
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

        private int safeInt(Integer value) {

                return value != null
                                ? value
                                : 0;
        }
}

