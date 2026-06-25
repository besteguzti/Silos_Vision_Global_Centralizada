package com.tfg.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tfg.dashboard.config.properties.KpiProperties;

/**
 * Aplica la tabla de afecciones parcial de Aruba.
 *
 * El resultado final no es una media ni un reparto por bloques: cada indicador
 * aporta puntos de afeccion y la suma se limita a 100.
 */
final class ArubaAffectationCalculator {

        private static final String GREEN = "GREEN";
        private static final String YELLOW = "YELLOW";
        private static final String RED = "RED";
        private static final String NEUTRAL = "NEUTRAL";

        private ArubaAffectationCalculator() {
        }

        static Result calculate(Input input,KpiProperties properties) {

                KpiProperties.Aruba aruba = properties.getAruba();
                List<String> allReasons = new ArrayList<>();
                List<String> accessPointReasons = new ArrayList<>();
                List<String> switchReasons = new ArrayList<>();

                Indicator tickets = ticketIndicator(input.arubaOpenTickets(), aruba);
                addReason(allReasons, tickets);

                Indicator downAps = downApsIndicator(input.totalAps(), input.downAps(), aruba);
                addReason(accessPointReasons, downAps);
                addReason(allReasons, downAps);

                Indicator pendingFirmwareAps = pendingFirmwareApsIndicator(input.pendingFirmwareAps(), aruba);
                addReason(accessPointReasons, pendingFirmwareAps);
                addReason(allReasons, pendingFirmwareAps);

                Indicator inactiveAps = inactiveApsIndicator(input.inactiveAps(), aruba);
                addReason(accessPointReasons, inactiveAps);
                addReason(allReasons, inactiveAps);

                Indicator wifiClients = wifiClientsIndicator(input.totalWifiClients(), "No hay clientes WiFi", aruba);
                Indicator mutualiaApsClients =
                                wifiClientsIndicator(input.mutualiaApsClients(), "No hay clientes Mutualia-APS", aruba);
                Indicator mutualiaWifiClients =
                                wifiClientsIndicator(input.mutualiaWifiClients(), "No hay clientes Mutualia-WIFI", aruba);
                addReason(accessPointReasons, wifiClients);
                addReason(accessPointReasons, mutualiaApsClients);
                addReason(accessPointReasons, mutualiaWifiClients);
                addReason(allReasons, wifiClients);
                addReason(allReasons, mutualiaApsClients);
                addReason(allReasons, mutualiaWifiClients);

                Indicator downSwitches = downSwitchesIndicator(input.totalSwitches(), input.downSwitches(), aruba);
                addReason(switchReasons, downSwitches);
                addReason(allReasons, downSwitches);

                Indicator switchUpgrade = switchUpgradeIndicator(input.switchesFirmwareUpgradeRequired(), aruba);
                addReason(switchReasons, switchUpgrade);
                addReason(allReasons, switchUpgrade);

                Indicator underusedSwitches = underusedSwitchesIndicator(input.underusedSwitches(), aruba);
                addReason(switchReasons, underusedSwitches);
                addReason(allReasons, underusedSwitches);
                Map<String, String> indicatorStatuses = indicatorStatuses(
                                tickets,
                                downAps,
                                pendingFirmwareAps,
                                inactiveAps,
                                wifiClients,
                                mutualiaApsClients,
                                mutualiaWifiClients,
                                downSwitches,
                                switchUpgrade,
                                underusedSwitches);

                int accessPointAffection = clamp(
                                downAps.affection()
                                                + pendingFirmwareAps.affection()
                                                + inactiveAps.affection()
                                                + clientAffection(wifiClients, mutualiaApsClients, mutualiaWifiClients),
                                properties);
                int switchAffection = clamp(
                                downSwitches.affection()
                                                + switchUpgrade.affection()
                                                + underusedSwitches.affection(),
                                properties);
                int totalAffection = clamp(
                                tickets.affection()
                                                + accessPointAffection
                                                + switchAffection,
                                properties);
                String color = statusFromAffection(totalAffection, properties);

                return new Result(
                                totalAffection,
                                color,
                                accessPointAffection,
                                strongestStatus(downAps, pendingFirmwareAps, inactiveAps, wifiClients, mutualiaApsClients, mutualiaWifiClients),
                                accessPointReasons,
                                switchAffection,
                                strongestStatus(downSwitches, switchUpgrade, underusedSwitches),
                                switchReasons,
                                allReasons,
                                indicatorStatuses,
                                hasRed(tickets, downAps, pendingFirmwareAps, inactiveAps, wifiClients, mutualiaApsClients, mutualiaWifiClients, downSwitches, switchUpgrade, underusedSwitches));
        }

        private static Indicator ticketIndicator(int tickets,KpiProperties.Aruba aruba) {

                if (tickets >= aruba.getArubaOpenTicketsRedMin()) {

                        return new Indicator(RED, aruba.getArubaOpenTicketsRedAffection(), "Tickets abiertos Aruba en rojo (" + tickets + ")");
                }

                if (tickets >= aruba.getArubaOpenTicketsYellowMin()) {

                        return new Indicator(YELLOW, aruba.getArubaOpenTicketsYellowAffection(), "Tickets abiertos Aruba en amarillo (" + tickets + ")");
                }

                return Indicator.green();
        }

        private static Indicator downApsIndicator(int totalAps,int downAps,KpiProperties.Aruba aruba) {

                if (totalAps <= 0) {

                        return Indicator.green();
                }

                int downPercent = downAps * 100 / totalAps;

                if (downPercent >= aruba.getAccessPointDownRedPercent()) {

                        return new Indicator(RED, aruba.getAccessPointDownRedAffection(), "APs caidos en rojo (" + downPercent + "%)");
                }

                if (downPercent >= aruba.getAccessPointDownYellowPercent()) {

                        return new Indicator(YELLOW, aruba.getAccessPointDownYellowAffection(), "APs caidos en amarillo (" + downPercent + "%)");
                }

                return Indicator.green();
        }

        private static Map<String, String> indicatorStatuses(
                        Indicator tickets,
                        Indicator downAps,
                        Indicator pendingFirmwareAps,
                        Indicator inactiveAps,
                        Indicator wifiClients,
                        Indicator mutualiaApsClients,
                        Indicator mutualiaWifiClients,
                        Indicator downSwitches,
                        Indicator switchUpgrade,
                        Indicator underusedSwitches) {

                Map<String, String> statuses = new LinkedHashMap<>();

                statuses.put("arubaOpenTickets", tickets.status());
                statuses.put("totalAps", NEUTRAL);
                statuses.put("upAps", NEUTRAL);
                statuses.put("downAps", downAps.status());
                statuses.put("firmwareOutdated", pendingFirmwareAps.status());
                statuses.put("inactiveAps", inactiveAps.status());
                statuses.put("totalWifiClients", wifiClients.status());
                statuses.put("mutualiaApsClients", mutualiaApsClients.status());
                statuses.put("mutualiaWifiClients", mutualiaWifiClients.status());
                statuses.put("totalSwitches", NEUTRAL);
                statuses.put("downSwitches", downSwitches.status());
                statuses.put("switchesFirmwareUpgradeRequired", switchUpgrade.status());
                statuses.put("underusedSwitches", underusedSwitches.status());

                return statuses;
        }

        private static Indicator pendingFirmwareApsIndicator(int pendingFirmwareAps,KpiProperties.Aruba aruba) {

                if (pendingFirmwareAps >= aruba.getPendingFirmwareApsYellowMin()) {

                        return new Indicator(YELLOW, aruba.getPendingFirmwareApsYellowAffection(), "Firmware pendiente en APs en amarillo (" + pendingFirmwareAps + ")");
                }

                return Indicator.green();
        }

        private static Indicator inactiveApsIndicator(int inactiveAps,KpiProperties.Aruba aruba) {

                if (inactiveAps >= aruba.getInactiveApsYellowMin()) {

                        return new Indicator(YELLOW, aruba.getInactiveApsYellowAffection(), "APs inactivos en amarillo (" + inactiveAps + ")");
                }

                return Indicator.green();
        }

        private static Indicator wifiClientsIndicator(int totalWifiClients,String reason,KpiProperties.Aruba aruba) {

                if (totalWifiClients <= aruba.getCriticalClientsGreenAbove()) {

                        return new Indicator(RED, aruba.getTotalWifiClientsRedAffection(), reason);
                }

                return Indicator.neutral();
        }

        private static Indicator downSwitchesIndicator(int totalSwitches,int downSwitches,KpiProperties.Aruba aruba) {

                if (totalSwitches > 0 && downSwitches >= totalSwitches) {

                        return new Indicator(RED, aruba.getSwitchDownRedAffection(), "Switches apagados en rojo (" + downSwitches + ")");
                }

                if (downSwitches > aruba.getSwitchDownYellowAbove()) {

                        return new Indicator(YELLOW, aruba.getSwitchDownYellowAffection(), "Switches apagados en amarillo (" + downSwitches + ")");
                }

                return Indicator.green();
        }

        private static Indicator switchUpgradeIndicator(int switchesFirmwareUpgradeRequired,KpiProperties.Aruba aruba) {

                if (switchesFirmwareUpgradeRequired >= aruba.getSwitchUpgradeYellowMin()) {

                        return new Indicator(YELLOW, aruba.getSwitchUpgradeYellowAffection(), "Switches con upgrade pendiente en amarillo (" + switchesFirmwareUpgradeRequired + ")");
                }

                return Indicator.green();
        }

        private static Indicator underusedSwitchesIndicator(int underusedSwitches,KpiProperties.Aruba aruba) {

                if (underusedSwitches > aruba.getUnderusedSwitchesRedAbove()) {

                        return new Indicator(RED, aruba.getUnderusedSwitchesRedAffection(), "Switches infrautilizados en rojo (" + underusedSwitches + ")");
                }

                if (underusedSwitches > aruba.getUnderusedSwitchesYellowAbove()) {

                        return new Indicator(YELLOW, aruba.getUnderusedSwitchesYellowAffection(), "Switches infrautilizados en amarillo (" + underusedSwitches + ")");
                }

                return Indicator.neutral();
        }

        private static int clientAffection(Indicator... indicators) {

                int max = 0;

                for (Indicator indicator : indicators) {
                        max = Math.max(max, indicator.affection());
                }

                return max;
        }

        private static void addReason(List<String> reasons,Indicator indicator) {

                if (!GREEN.equals(indicator.status()) && !indicator.reason().isBlank()) {

                        reasons.add(indicator.reason());
                }
        }

        private static boolean hasRed(Indicator... indicators) {

                for (Indicator indicator : indicators) {

                        if (RED.equals(indicator.status())) {

                                return true;
                        }
                }

                return false;
        }

        private static String strongestStatus(Indicator... indicators) {

                boolean hasYellow = false;

                for (Indicator indicator : indicators) {

                        if (RED.equals(indicator.status())) {

                                return RED;
                        }

                        if (YELLOW.equals(indicator.status())) {

                                hasYellow = true;
                        }
                }

                return hasYellow ? YELLOW : GREEN;
        }

        private static String statusFromAffection(int value,KpiProperties properties) {

                if (value >= properties.getStatus().getRedMin()) {

                        return RED;
                }

                if (value >= properties.getStatus().getYellowMin()) {

                        return YELLOW;
                }

                return GREEN;
        }

        private static int clamp(int value,KpiProperties properties) {

                return Math.max(0, Math.min(properties.getStatus().getMax(), value));
        }

        record Input(
                        int totalAps,
                        int downAps,
                        int inactiveAps,
                        int pendingFirmwareAps,
                        int totalWifiClients,
                        int mutualiaApsClients,
                        int mutualiaWifiClients,
                        int totalSwitches,
                        int downSwitches,
                        int switchesFirmwareUpgradeRequired,
                        int underusedSwitches,
                        int arubaOpenTickets) {
        }

        record Result(
                        int totalAffection,
                        String color,
                        int accessPointAffection,
                        String accessPointColor,
                        List<String> accessPointReasons,
                        int switchAffection,
                        String switchColor,
                        List<String> switchReasons,
                        List<String> reasons,
                        Map<String, String> indicatorStatuses,
                        boolean criticalCondition) {
        }

        private record Indicator(String status,int affection,String reason) {

                private static Indicator green() {

                        return new Indicator(GREEN, 0, "");
                }

                private static Indicator neutral() {

                        return new Indicator(NEUTRAL, 0, "");
                }
        }
}
