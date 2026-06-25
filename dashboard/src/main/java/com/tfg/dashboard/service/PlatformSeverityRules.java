package com.tfg.dashboard.service;

import java.util.Arrays;
import java.util.List;

import com.tfg.dashboard.config.properties.KpiProperties;

final class PlatformSeverityRules {

    private PlatformSeverityRules() {
    }

    static int applyInternalSeverityFloorFromScores(
            int aggregateAffection,
            KpiProperties kpiProperties,
            int... indicatorScores
    ) {
        List<String> statuses =
                Arrays.stream(indicatorScores)
                        .mapToObj(score -> statusFromIndicatorScore(score, kpiProperties))
                        .toList();

        return applyInternalSeverityFloor(aggregateAffection, kpiProperties, statuses);
    }

    static int applyInternalSeverityFloor(
            int aggregateAffection,
            KpiProperties kpiProperties,
            List<String> indicatorStatuses
    ) {
        long redCount =
                indicatorStatuses.stream()
                        .filter(status -> "RED".equalsIgnoreCase(status))
                        .count();
        long yellowCount =
                indicatorStatuses.stream()
                        .filter(status -> "YELLOW".equalsIgnoreCase(status))
                        .count();

        int minimumAffection = 0;

        if (redCount >= 1 || yellowCount >= 2) {
            minimumAffection = kpiProperties.getStatus().getRedMin();
        } else if (yellowCount == 1) {
            minimumAffection = kpiProperties.getStatus().getYellowMin();
        }

        return clamp(Math.max(aggregateAffection, minimumAffection), kpiProperties);
    }

    static String statusFromAffection(int value, KpiProperties kpiProperties) {
        if (value >= kpiProperties.getStatus().getRedMin()) {
            return "RED";
        }

        if (value >= kpiProperties.getStatus().getYellowMin()) {
            return "YELLOW";
        }

        return "GREEN";
    }

    private static String statusFromIndicatorScore(int score, KpiProperties kpiProperties) {
        if (score >= kpiProperties.getAffection().getRed()) {
            return "RED";
        }

        if (score >= kpiProperties.getAffection().getYellow()) {
            return "YELLOW";
        }

        return "GREEN";
    }

    private static int clamp(int value, KpiProperties kpiProperties) {
        if (value < 0) {
            return 0;
        }

        if (value > kpiProperties.getStatus().getMax()) {
            return kpiProperties.getStatus().getMax();
        }

        return value;
    }
}
