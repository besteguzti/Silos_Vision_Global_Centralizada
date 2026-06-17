package com.tfg.dashboard.dto;

public enum KpiStatus {

    GREEN,
    YELLOW,
    RED,
    NO_DATA,
    STALE;

    public static KpiStatus from(String status) {

        if (status == null || status.isBlank()) {

            return NO_DATA;
        }

        if ("OK".equalsIgnoreCase(status) || "HEALTHY".equalsIgnoreCase(status)) {

            return GREEN;
        }

        if ("WARNING".equalsIgnoreCase(status) || "WARN".equalsIgnoreCase(status) || "DEGRADED".equalsIgnoreCase(status)) {

            return YELLOW;
        }

        if ("DANGER".equalsIgnoreCase(status) || "INCIDENT".equalsIgnoreCase(status)) {

            return RED;
        }

        try {
            return KpiStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return NO_DATA;
        }
    }
}
