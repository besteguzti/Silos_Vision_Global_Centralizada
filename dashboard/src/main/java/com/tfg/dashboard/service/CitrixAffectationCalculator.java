package com.tfg.dashboard.service;

import java.util.List;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.CitrixIndicatorStatusDto;

/**
 * Calcula la afectación parcial de Citrix mediante sumatoria de indicadores.
 *
 * Cada indicador puede ser GREEN, YELLOW, RED o NEUTRAL. El valor final se
 * obtiene sumando las afecciones individuales y limitándolas al máximo.
 * El estado superior de Citrix se deriva de esa afección total; las tarjetas
 * internas mantienen su severidad propia sin forzar automáticamente el estado
 * global de la plataforma.
 */
final class CitrixAffectationCalculator {

        private static final String GREEN = "GREEN";
        private static final String YELLOW = "YELLOW";
        private static final String RED = "RED";
        private static final String NEUTRAL = "NEUTRAL";

        private static final int YELLOW_AFFECTION = 20;
        private static final int RED_AFFECTION = 100;
        private static final int LICENSE_YELLOW_AFFECTION = 2;
        private static final int LICENSE_RED_AFFECTION = 5;
        private static final int DC_YELLOW_AFFECTION = 15;
        private static final int DC_RED_AFFECTION = 70;
        private static final int DISCONNECTED_YELLOW_AFFECTION = 2;
        private static final int LOGON_YELLOW_AFFECTION = 15;
        private static final int LOGON_RED_AFFECTION = 70;
        private static final int SERVER_YELLOW_AFFECTION = 15;
        private static final int SERVER_RED_MIN_AFFECTION = 40;
        private static final int SERVER_RED_MAX_AFFECTION = 70;
        private static final int FAILED_YELLOW_AFFECTION = 15;
        private static final int FAILED_RED_AFFECTION = 70;
        private static final int TICKETS_YELLOW_AFFECTION = 2;
        private static final int TICKETS_RED_AFFECTION = 5;
        private static final int DELIVERY_CONTROLLERS_RED_THRESHOLD = 34;

        private CitrixAffectationCalculator() {
        }

        static Result calculate(Input input, KpiProperties properties) {
                KpiProperties.Citrix citrix = properties.getCitrix();

                List<CitrixIndicatorStatusDto> indicators = List.of(
                                activeSessionsIndicator(input.activeSessions()),
                                activeLicensesIndicator(input.activeLicenses(), citrix),
                                deliveryControllersIndicator(input.availableDeliveryControllers(), input.totalDeliveryControllers(), citrix),
                                disconnectedSessionsIndicator(input.disconnectedSessions()),
                                averageLogonDurationIndicator(input.averageLogonDurationSeconds(), citrix),
                                serverLoadIndicator(input.serverLoadPercent(), citrix),
                                failedLogonsIndicator(input.failedLogons(), citrix),
                                openTicketsIndicator(input.citrixOpenTickets(), citrix));

                int percentage = clamp(
                                indicators.stream().mapToInt(CitrixIndicatorStatusDto::getAffectionPercent).sum(),
                                properties);
                String color = finalColor(percentage, properties);
                List<String> reasons = indicators.stream()
                                .filter(indicator -> !GREEN.equals(indicator.getColor()) && !NEUTRAL.equals(indicator.getColor()))
                                .map(CitrixIndicatorStatusDto::getReason)
                                .toList();
                boolean affectedService = indicators.stream()
                                .anyMatch(indicator -> YELLOW.equals(indicator.getColor()) || RED.equals(indicator.getColor()));
                boolean criticalCondition = indicators.stream()
                                .anyMatch(indicator -> RED.equals(indicator.getColor()));

                return new Result(percentage, color, indicators, reasons, affectedService, criticalCondition);
        }

        private static CitrixIndicatorStatusDto activeSessionsIndicator(int activeSessions) {
                if (activeSessions <= 0) {
                        return indicator(
                                        "Sesiones activas",
                                        RED,
                                        "No hay sesiones activas observadas en Citrix",
                                        RED_AFFECTION);
                }

                return indicator(
                                "Sesiones activas",
                                GREEN,
                                activeSessions + " sesiones activas observadas en Citrix",
                                0);
        }

        private static CitrixIndicatorStatusDto activeLicensesIndicator(int activeLicenses, KpiProperties.Citrix citrix) {
                if (activeLicenses <= citrix.getActiveLicensesRedBelowOrEqual()) {
                        return indicator(
                                        "Licencias activas",
                                        RED,
                                        "Quedan " + activeLicenses + " licencias activas en Citrix",
                                        LICENSE_RED_AFFECTION);
                }

                if (activeLicenses < citrix.getActiveLicensesYellowBelow()) {
                        return indicator(
                                        "Licencias activas",
                                        YELLOW,
                                        "Quedan " + activeLicenses + " licencias activas en Citrix",
                                        LICENSE_YELLOW_AFFECTION);
                }

                return indicator(
                                "Licencias activas",
                                GREEN,
                                activeLicenses + " licencias activas disponibles en Citrix",
                                0);
        }

        private static CitrixIndicatorStatusDto deliveryControllersIndicator(
                        int availableDeliveryControllers,
                        int totalDeliveryControllers,
                        KpiProperties.Citrix citrix) {

                if (totalDeliveryControllers <= 0 || availableDeliveryControllers <= 0) {
                        return indicator("Delivery Controllers disponibles", RED, "No hay Delivery Controllers disponibles", RED_AFFECTION);
                }

                int availablePercent = availableDeliveryControllers * 100 / totalDeliveryControllers;
                if (availablePercent < DELIVERY_CONTROLLERS_RED_THRESHOLD) {
                        return indicator(
                                        "Delivery Controllers disponibles",
                                        RED,
                                        "Menos del " + DELIVERY_CONTROLLERS_RED_THRESHOLD
                                                        + " % de Delivery Controllers disponibles",
                                        DC_RED_AFFECTION);
                }

                if (availablePercent < citrix.getDeliveryControllerYellowBelowPercent()) {
                        return indicator(
                                        "Delivery Controllers disponibles",
                                        YELLOW,
                                        "Menos del " + citrix.getDeliveryControllerYellowBelowPercent()
                                                        + " % de Delivery Controllers disponibles",
                                        DC_YELLOW_AFFECTION);
                }

                return indicator(
                                "Delivery Controllers disponibles",
                                GREEN,
                                citrix.getDeliveryControllerYellowBelowPercent()
                                                + " % o mas de Delivery Controllers disponibles",
                                0);
        }

        private static CitrixIndicatorStatusDto disconnectedSessionsIndicator(int disconnectedSessions) {
                if (disconnectedSessions > 10) {
                        return indicator(
                                        "Sesiones desconectadas",
                                        YELLOW,
                                        "Hay " + disconnectedSessions + " sesiones desconectadas en Citrix",
                                        DISCONNECTED_YELLOW_AFFECTION);
                }

                return indicator(
                                "Sesiones desconectadas",
                                NEUTRAL,
                                "Hay " + disconnectedSessions + " sesiones desconectadas en Citrix",
                                0);
        }

        private static CitrixIndicatorStatusDto averageLogonDurationIndicator(int averageLogonDurationSeconds, KpiProperties.Citrix citrix) {
                if (averageLogonDurationSeconds > citrix.getLogonDurationRedAboveSeconds()) {
                        return indicator(
                                        "Average Logon Duration",
                                        RED,
                                        "Average Logon Duration superior a "
                                                        + citrix.getLogonDurationRedAboveSeconds()
                                                        + " segundos",
                                        LOGON_RED_AFFECTION);
                }

                if (averageLogonDurationSeconds > citrix.getLogonDurationYellowAboveSeconds()) {
                        return indicator(
                                        "Average Logon Duration",
                                        YELLOW,
                                        "Average Logon Duration entre "
                                                        + (citrix.getLogonDurationYellowAboveSeconds() + 1)
                                                        + " y "
                                                        + citrix.getLogonDurationRedAboveSeconds()
                                                        + " segundos",
                                        LOGON_YELLOW_AFFECTION);
                }

                return indicator(
                                "Average Logon Duration",
                                GREEN,
                                "Average Logon Duration entre 0 y "
                                                + citrix.getLogonDurationYellowAboveSeconds()
                                                + " segundos",
                                0);
        }

        private static CitrixIndicatorStatusDto serverLoadIndicator(int serverLoadPercent, KpiProperties.Citrix citrix) {
                if (serverLoadPercent >= citrix.getServerLoadRedMin()) {
                        return indicator(
                                        "Carga de servidores",
                                        RED,
                                        "Carga de servidores igual o superior a "
                                                        + citrix.getServerLoadRedMin()
                                                        + " %",
                                        serverRedAffection(serverLoadPercent, citrix));
                }

                if (serverLoadPercent >= citrix.getServerLoadYellowMin()) {
                        return indicator(
                                        "Carga de servidores",
                                        YELLOW,
                                        "Carga de servidores entre "
                                                        + citrix.getServerLoadYellowMin()
                                                        + " % y "
                                                        + (citrix.getServerLoadRedMin() - 1)
                                                        + " %",
                                        SERVER_YELLOW_AFFECTION);
                }

                return indicator(
                                "Carga de servidores",
                                GREEN,
                                "Carga de servidores inferior a "
                                                + citrix.getServerLoadYellowMin()
                                                + " %",
                                0);
        }

        private static CitrixIndicatorStatusDto failedLogonsIndicator(int failedLogons, KpiProperties.Citrix citrix) {
                if (failedLogons > citrix.getFailedLogonsRedAbove()) {
                        return indicator(
                                        "Errores de inicio",
                                        RED,
                                        "Mas de " + citrix.getFailedLogonsRedAbove()
                                                        + " errores de inicio",
                                        FAILED_RED_AFFECTION);
                }

                if (failedLogons > citrix.getFailedLogonsYellowAbove()) {
                        return indicator(
                                        "Errores de inicio",
                                        YELLOW,
                                        "Entre "
                                                        + (citrix.getFailedLogonsYellowAbove() + 1)
                                                        + " y "
                                                        + citrix.getFailedLogonsRedAbove()
                                                        + " errores de inicio",
                                        FAILED_YELLOW_AFFECTION);
                }

                return indicator(
                                "Errores de inicio",
                                GREEN,
                                "Entre 0 y "
                                                + citrix.getFailedLogonsYellowAbove()
                                                + " errores de inicio",
                                0);
        }

        private static CitrixIndicatorStatusDto openTicketsIndicator(int citrixOpenTickets, KpiProperties.Citrix citrix) {
                if (citrixOpenTickets >= citrix.getCitrixOpenTicketsRedMin()) {
                        return indicator(
                                        "Tickets abiertos Citrix",
                                        RED,
                                        "Hay " + citrixOpenTickets + " tickets Citrix abiertos",
                                        TICKETS_RED_AFFECTION);
                }

                if (citrixOpenTickets >= citrix.getCitrixOpenTicketsYellowMin()) {
                        return indicator(
                                        "Tickets abiertos Citrix",
                                        YELLOW,
                                        "Hay " + citrixOpenTickets + " tickets Citrix abiertos",
                                        TICKETS_YELLOW_AFFECTION);
                }

                return indicator(
                                "Tickets abiertos Citrix",
                                GREEN,
                                "Hay " + citrixOpenTickets + " tickets Citrix abiertos",
                                0);
        }

        private static CitrixIndicatorStatusDto indicator(String name, String color, String reason, int affectionPercent) {
                CitrixIndicatorStatusDto indicator = new CitrixIndicatorStatusDto();
                indicator.setName(name);
                indicator.setColor(color);
                indicator.setAffectionPercent(affectionPercent);
                indicator.setReason(reason);
                return indicator;
        }

        private static String finalColor(int percentage, KpiProperties properties) {

                return PlatformSeverityRules.statusFromAffection(percentage, properties);
        }

        private static int serverRedAffection(int serverLoadPercent, KpiProperties.Citrix citrix) {
                int redRange = Math.max(1, 100 - citrix.getServerLoadRedMin());
                int valueInsideRedRange = Math.max(0, serverLoadPercent - citrix.getServerLoadRedMin());
                int variableAffection =
                                (SERVER_RED_MAX_AFFECTION - SERVER_RED_MIN_AFFECTION)
                                                * valueInsideRedRange
                                                / redRange;

                return Math.min(
                                SERVER_RED_MAX_AFFECTION,
                                SERVER_RED_MIN_AFFECTION + variableAffection);
        }

        private static int clamp(int value, KpiProperties properties) {
                return Math.max(0, Math.min(properties.getStatus().getMax(), value));
        }

        record Input(
                        int activeSessions,
                        int activeLicenses,
                        int availableDeliveryControllers,
                        int totalDeliveryControllers,
                        int disconnectedSessions,
                        int averageLogonDurationSeconds,
                        int serverLoadPercent,
                        int failedLogons,
                        int citrixOpenTickets) {
        }

        record Result(
                        int percentage,
                        String color,
                        List<CitrixIndicatorStatusDto> indicators,
                        List<String> reasons,
                        boolean affectedService,
                        boolean criticalCondition) {
        }
}

