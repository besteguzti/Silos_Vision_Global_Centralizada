package com.tfg.dashboard.service;

import java.util.List;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.Microsoft365IndicatorStatusDto;

/**
 * Calcula la afeccion Microsoft 365 mediante suma de afecciones parciales.
 *
 * La clase no calcula una media uniforme: cada indicador aporta un peso fijo
 * segun la regla funcional definida para el panel Microsoft 365 y el resultado
 * final se limita a la escala comun 0-100.
 */
final class Microsoft365AffectationCalculator {

    private static final String GREEN = "GREEN";
    private static final String YELLOW = "YELLOW";
    private static final String RED = "RED";
    private static final String NEUTRAL = "NEUTRAL";

    private static final int LICENSE_YELLOW_AFFECTION = 2;
    private static final int LICENSE_RED_AFFECTION = 5;
    private static final int SERVICE_YELLOW_AFFECTION = 20;
    private static final int SERVICE_RED_AFFECTION = 30;
    private static final int MAILBOX_YELLOW_AFFECTION = 5;
    private static final int QUARANTINE_YELLOW_AFFECTION = 5;
    private static final int SHAREPOINT_YELLOW_AFFECTION = 10;
    private static final int SHAREPOINT_RED_AFFECTION = 50;
    private static final int RISKY_USERS_YELLOW_AFFECTION = 2;
    private static final int RISKY_USERS_RED_AFFECTION = 5;
    private static final int FAILED_SIGN_INS_YELLOW_AFFECTION = 2;
    private static final int FAILED_SIGN_INS_RED_AFFECTION = 10;
    private static final int MFA_YELLOW_AFFECTION = 5;
    private static final int MFA_RED_AFFECTION = 10;
    private static final int SECRETS_YELLOW_AFFECTION = 2;
    private static final int UNUSED_APPS_YELLOW_AFFECTION = 2;
    private static final int ELEVATED_APPS_YELLOW_AFFECTION = 2;
    private static final int NON_COMPLIANT_YELLOW_AFFECTION = 5;
    private static final int NON_COMPLIANT_RED_AFFECTION = 10;
    private static final int TICKETS_YELLOW_AFFECTION = 2;
    private static final int TICKETS_RED_AFFECTION = 5;
    private static final int OUTDATED_WINDOWS_YELLOW_AFFECTION = 2;
    private static final int ENCRYPTION_RED_AFFECTION = 30;
    private static final int STALE_DEVICES_RED_AFFECTION = 30;

    private Microsoft365AffectationCalculator() {
    }

    static Result calculate(Input input, KpiProperties properties) {
        KpiProperties.Microsoft365 microsoft365 = properties.getMicrosoft365();

        List<Microsoft365IndicatorStatusDto> indicators = List.of(
                activeUsersIndicator(input.activeUsers()),
                unassignedLicensesIndicator(input.unassignedLicenses(), microsoft365),
                serviceIndicator("Outlook", input.outlookStatus()),
                serviceIndicator("Teams", input.teamsStatus()),
                serviceIndicator("Servicio SharePoint", input.sharePointStatus()),
                positiveYellowIndicator(
                        "Buzones casi llenos",
                        input.nearlyFullMailboxes(),
                        MAILBOX_YELLOW_AFFECTION,
                        "Hay buzones proximos a quedarse sin capacidad"),
                positiveYellowIndicator(
                        "Emails en cuarentena",
                        input.emailsQuarantined(),
                        QUARANTINE_YELLOW_AFFECTION,
                        "Hay correos retenidos en cuarentena"),
                sharePointStorageIndicator(input.sharePointStoragePercent(), microsoft365),
                riskyUsersIndicator(input.riskyUsers(), microsoft365),
                failedSignInsIndicator(input.failedSignIns(), microsoft365),
                usersWithoutMfaIndicator(input.usersWithoutMfa(), microsoft365),
                positiveYellowAboveIndicator(
                        "Secretos proximos a caducar",
                        input.appsSecretsExpiringSoon(),
                        microsoft365.getSecretsYellowAbove(),
                        SECRETS_YELLOW_AFFECTION,
                        "Hay secretos proximos a caducar"),
                positiveYellowAboveIndicator(
                        "Aplicaciones sin uso",
                        input.unusedApplications(),
                        microsoft365.getUnusedApplicationsYellowAbove(),
                        UNUSED_APPS_YELLOW_AFFECTION,
                        "Hay aplicaciones sin uso"),
                positiveYellowAboveIndicator(
                        "Apps permisos elevados",
                        input.highPrivilegeApplications(),
                        microsoft365.getHighPrivilegeApplicationsYellowAbove(),
                        ELEVATED_APPS_YELLOW_AFFECTION,
                        "Hay aplicaciones con permisos elevados"),
                nonCompliantDevicesIndicator(input.nonCompliantDevices(), microsoft365),
                openTicketsIndicator(input.microsoft365OpenTickets(), microsoft365),
                positiveYellowAboveIndicator(
                        "Windows desactualizados",
                        input.outdatedWindowsDevices(),
                        microsoft365.getOutdatedWindowsYellowAbove(),
                        OUTDATED_WINDOWS_YELLOW_AFFECTION,
                        "Hay equipos Windows desactualizados"),
                redAboveIndicator(
                        "Equipos sin cifrado",
                        input.devicesWithoutEncryption(),
                        microsoft365.getDevicesWithoutEncryptionRedAbove(),
                        ENCRYPTION_RED_AFFECTION,
                        "Hay equipos sin cifrado"),
                redAboveIndicator(
                        "Sin check-in >90 dias",
                        input.staleDevices(),
                        microsoft365.getStaleDevicesRedAbove(),
                        STALE_DEVICES_RED_AFFECTION,
                        "Hay dispositivos sin check-in durante mas de 90 dias"));

        int percentage = clamp(
                indicators.stream()
                        .mapToInt(Microsoft365IndicatorStatusDto::getAffectionPercent)
                        .sum(),
                properties);
        String color = PlatformSeverityRules.statusFromAffection(percentage, properties);
        List<String> reasons = indicators.stream()
                .filter(indicator -> !GREEN.equals(indicator.getColor()) && !NEUTRAL.equals(indicator.getColor()))
                .map(Microsoft365IndicatorStatusDto::getReason)
                .toList();
        boolean affectedService = indicators.stream()
                .anyMatch(indicator -> YELLOW.equals(indicator.getColor()) || RED.equals(indicator.getColor()));
        boolean criticalCondition = indicators.stream()
                .anyMatch(indicator -> RED.equals(indicator.getColor()));

        return new Result(percentage, color, indicators, reasons, affectedService, criticalCondition);
    }

    private static Microsoft365IndicatorStatusDto activeUsersIndicator(int activeUsers) {
        return indicator(
                "Usuarios activos",
                NEUTRAL,
                "Usuarios activos Microsoft 365 observados",
                0);
    }

    private static Microsoft365IndicatorStatusDto unassignedLicensesIndicator(
            int unassignedLicenses,
            KpiProperties.Microsoft365 microsoft365) {

        if (unassignedLicenses <= microsoft365.getUnassignedLicensesRedBelowOrEqual()) {
            return indicator(
                    "Licencias no asignadas",
                    RED,
                    "Quedan " + unassignedLicenses + " licencias no asignadas",
                    LICENSE_RED_AFFECTION);
        }

        if (unassignedLicenses < microsoft365.getUnassignedLicensesYellowBelow()) {
            return indicator(
                    "Licencias no asignadas",
                    YELLOW,
                    "Quedan " + unassignedLicenses + " licencias no asignadas",
                    LICENSE_YELLOW_AFFECTION);
        }

        return indicator(
                "Licencias no asignadas",
                GREEN,
                "Hay licencias no asignadas suficientes",
                0);
    }

    private static Microsoft365IndicatorStatusDto serviceIndicator(String name, String serviceStatus) {
        if ("INCIDENT".equalsIgnoreCase(serviceStatus) || RED.equalsIgnoreCase(serviceStatus)) {
            return indicator(name, RED, name + " presenta incidencia", SERVICE_RED_AFFECTION);
        }

        if ("DEGRADED".equalsIgnoreCase(serviceStatus) || YELLOW.equalsIgnoreCase(serviceStatus)) {
            return indicator(name, YELLOW, name + " presenta degradacion", SERVICE_YELLOW_AFFECTION);
        }

        return indicator(name, GREEN, name + " sin degradacion", 0);
    }

    private static Microsoft365IndicatorStatusDto sharePointStorageIndicator(
            int sharePointStoragePercent,
            KpiProperties.Microsoft365 microsoft365) {

        if (sharePointStoragePercent >= microsoft365.getSharePointRedAbove()) {
            return indicator(
                    "Almacenamiento de SharePoint",
                    RED,
                    "SharePoint supera el "
                            + microsoft365.getSharePointRedAbove()
                            + " % de almacenamiento",
                    SHAREPOINT_RED_AFFECTION);
        }

        if (sharePointStoragePercent >= microsoft365.getSharePointYellowMin()) {
            return indicator(
                    "Almacenamiento de SharePoint",
                    YELLOW,
                    "SharePoint supera el "
                            + microsoft365.getSharePointYellowMin()
                            + " % de almacenamiento",
                    SHAREPOINT_YELLOW_AFFECTION);
        }

        return indicator(
                "Almacenamiento de SharePoint",
                GREEN,
                "SharePoint esta por debajo del "
                        + microsoft365.getSharePointYellowMin()
                        + " % de almacenamiento",
                0);
    }

    private static Microsoft365IndicatorStatusDto riskyUsersIndicator(
            int riskyUsers,
            KpiProperties.Microsoft365 microsoft365) {

        if (riskyUsers > microsoft365.getRiskyUsersRedAbove()) {
            return indicator(
                    "Usuarios en riesgo",
                    RED,
                    "Hay " + riskyUsers + " usuarios en riesgo",
                    RISKY_USERS_RED_AFFECTION);
        }

        if (riskyUsers > microsoft365.getRiskyUsersYellowAbove()) {
            return indicator(
                    "Usuarios en riesgo",
                    YELLOW,
                    "Hay " + riskyUsers + " usuarios en riesgo",
                    RISKY_USERS_YELLOW_AFFECTION);
        }

        return indicator("Usuarios en riesgo", GREEN, "No hay usuarios en riesgo", 0);
    }

    private static Microsoft365IndicatorStatusDto failedSignInsIndicator(
            int failedSignIns,
            KpiProperties.Microsoft365 microsoft365) {

        if (failedSignIns >= microsoft365.getFailedSignInsRedMin()) {
            return indicator(
                    "Inicios fallidos",
                    RED,
                    "Hay " + failedSignIns + " inicios de sesion fallidos",
                    FAILED_SIGN_INS_RED_AFFECTION);
        }

        if (failedSignIns >= microsoft365.getFailedSignInsYellowMin()) {
            return indicator(
                    "Inicios fallidos",
                    YELLOW,
                    "Hay " + failedSignIns + " inicios de sesion fallidos",
                    FAILED_SIGN_INS_YELLOW_AFFECTION);
        }

        return indicator("Inicios fallidos", GREEN, "Menos de 10 inicios de sesion fallidos", 0);
    }

    private static Microsoft365IndicatorStatusDto usersWithoutMfaIndicator(
            int usersWithoutMfa,
            KpiProperties.Microsoft365 microsoft365) {

        if (usersWithoutMfa > microsoft365.getUsersWithoutMfaRedAbove()) {
            return indicator(
                    "Usuarios sin MFA",
                    RED,
                    "Hay usuarios sin MFA",
                    MFA_RED_AFFECTION);
        }

        if (usersWithoutMfa > microsoft365.getUsersWithoutMfaYellowAbove()) {
            return indicator(
                    "Usuarios sin MFA",
                    YELLOW,
                    "Hay usuarios sin MFA",
                    MFA_YELLOW_AFFECTION);
        }

        return indicator("Usuarios sin MFA", GREEN, "No hay usuarios sin MFA", 0);
    }

    private static Microsoft365IndicatorStatusDto nonCompliantDevicesIndicator(
            int nonCompliantDevices,
            KpiProperties.Microsoft365 microsoft365) {

        if (nonCompliantDevices > microsoft365.getNonCompliantDevicesRedAbove()) {
            return indicator(
                    "Equipos no conformes",
                    RED,
                    "Hay " + nonCompliantDevices + " equipos no conformes",
                    NON_COMPLIANT_RED_AFFECTION);
        }

        if (nonCompliantDevices > microsoft365.getNonCompliantDevicesYellowAbove()) {
            return indicator(
                    "Equipos no conformes",
                    YELLOW,
                    "Hay " + nonCompliantDevices + " equipos no conformes",
                    NON_COMPLIANT_YELLOW_AFFECTION);
        }

        return indicator("Equipos no conformes", GREEN, "Equipos no conformes en rango verde", 0);
    }

    private static Microsoft365IndicatorStatusDto openTicketsIndicator(
            int microsoft365OpenTickets,
            KpiProperties.Microsoft365 microsoft365) {

        if (microsoft365OpenTickets >= microsoft365.getMicrosoft365OpenTicketsRedMin()) {
            return indicator(
                    "Tickets abiertos Microsoft 365",
                    RED,
                    "Hay " + microsoft365OpenTickets + " tickets abiertos Microsoft 365",
                    TICKETS_RED_AFFECTION);
        }

        if (microsoft365OpenTickets >= microsoft365.getMicrosoft365OpenTicketsYellowMin()) {
            return indicator(
                    "Tickets abiertos Microsoft 365",
                    YELLOW,
                    "Hay " + microsoft365OpenTickets + " tickets abiertos Microsoft 365",
                    TICKETS_YELLOW_AFFECTION);
        }

        return indicator(
                "Tickets abiertos Microsoft 365",
                GREEN,
                "Tickets abiertos Microsoft 365 en rango verde",
                0);
    }

    private static Microsoft365IndicatorStatusDto positiveYellowIndicator(
            String name,
            int value,
            int affection,
            String reason) {

        if (value > 0) {
            return indicator(name, YELLOW, reason, affection);
        }

        return indicator(name, GREEN, name + " sin incidencias", 0);
    }

    private static Microsoft365IndicatorStatusDto positiveYellowAboveIndicator(
            String name,
            int value,
            int threshold,
            int affection,
            String reason) {

        if (value > threshold) {
            return indicator(name, YELLOW, reason, affection);
        }

        return indicator(name, GREEN, name + " sin incidencias", 0);
    }

    private static Microsoft365IndicatorStatusDto redAboveIndicator(
            String name,
            int value,
            int threshold,
            int affection,
            String reason) {

        if (value > threshold) {
            return indicator(name, RED, reason, affection);
        }

        return indicator(name, GREEN, name + " sin incidencias", 0);
    }

    private static Microsoft365IndicatorStatusDto indicator(
            String name,
            String color,
            String reason,
            int affectionPercent) {

        Microsoft365IndicatorStatusDto indicator = new Microsoft365IndicatorStatusDto();
        indicator.setName(name);
        indicator.setColor(color);
        indicator.setAffectionPercent(affectionPercent);
        indicator.setReason(reason);
        return indicator;
    }

    private static int clamp(int value, KpiProperties properties) {
        return Math.max(0, Math.min(properties.getStatus().getMax(), value));
    }

    record Input(
            int activeUsers,
            int unassignedLicenses,
            String outlookStatus,
            String teamsStatus,
            String sharePointStatus,
            int nearlyFullMailboxes,
            int emailsQuarantined,
            int sharePointStoragePercent,
            int riskyUsers,
            int failedSignIns,
            int usersWithoutMfa,
            int appsSecretsExpiringSoon,
            int unusedApplications,
            int highPrivilegeApplications,
            int nonCompliantDevices,
            int microsoft365OpenTickets,
            int outdatedWindowsDevices,
            int devicesWithoutEncryption,
            int staleDevices) {
    }

    record Result(
            int percentage,
            String color,
            List<Microsoft365IndicatorStatusDto> indicators,
            List<String> reasons,
            boolean affectedService,
            boolean criticalCondition) {
    }
}
