package com.tfg.dashboard.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.ArubaWifiClientInfo;

/**
 * Agrupa clientes WiFi de Aruba por grupos y redes relevantes para Mutualia.
 *
 * La sincronización usa estos contadores para alimentar el resumen Aruba y las
 * reglas del índice de salud Aruba.
 */
@Service
public class ArubaWifiClientAggregationService {

        private static final Logger log = LoggerFactory.getLogger(ArubaWifiClientAggregationService.class);

        /**
         * Calcula métricas agregadas de clientes WiFi a partir de la lista
         * recibida desde Aruba Central.
         */
        public WifiClientMetrics buildMetrics(List<ArubaWifiClientInfo> clients) {

                List<ArubaWifiClientInfo> safeClients = clients == null ? List.of() : clients;

                return new WifiClientMetrics(
                                safeClients.size(),
                                countClientsByGroup(safeClients, "MUTUALIA-APs"),
                                countClientsByGroup(safeClients, "MUTUALIA-WIFI"),
                                countClientsByWifiNetwork(safeClients, "MUTUALIA_LANGILEAK"),
                                countClientsByWifiNetwork(safeClients, "MUTUALIA"),
                                countClientsByWifiNetwork(safeClients, "MUTUALIA_RED_INTERNA"),
                                countClientsByWifiNetwork(safeClients, "MUTUALIA_RED_EXTERNA"),
                                countClientsByWifiNetwork(safeClients, "MUTUALIA_KORPORATIBOA"),
                                countClientsByWifiNetwork(safeClients, "WIFI_PACs"),
                                countClientsByWifiNetwork(safeClients, "MUT_VIDEO"));
        }

        /**
         * Prepara un mapa de diagnóstico útil para revisar grupos y redes
         * detectadas sin cambiar la lógica de KPIs.
         */
        public Map<String, Object> buildDiagnostics(List<ArubaWifiClientInfo> clients) {

                List<ArubaWifiClientInfo> safeClients = clients == null ? List.of() : clients;
                Map<String, Object> diagnostics = new LinkedHashMap<>();

                diagnostics.put("total", safeClients.size());
                diagnostics.put("groups", countByGroup(safeClients));
                diagnostics.put("mutualiaWifiNetworks", countByMutualiaWifiNetwork(safeClients));
                diagnostics.put("sample", safeClients.stream().limit(5).toList());

                return diagnostics;
        }

        public int countClientsByGroup(List<ArubaWifiClientInfo> clients,String groupName) {

                return (int) clients.stream()
                                .filter(clientInfo -> normalize(groupName).equals(normalize(clientInfo.getGroupName())))
                                .count();
        }

        public int countClientsByWifiNetwork(List<ArubaWifiClientInfo> clients,String network) {

                return (int) clients.stream()
                                .filter(clientInfo -> "MUTUALIA-WIFI".equals(normalize(clientInfo.getGroupName())))
                                .filter(clientInfo -> normalize(network).equals(normalize(clientInfo.getNetwork())))
                                .count();
        }

        public void logWifiClientBreakdown(List<ArubaWifiClientInfo> clients) {

                List<ArubaWifiClientInfo> safeClients = clients == null ? List.of() : clients;
                Map<String, Long> groups = countByGroup(safeClients);
                Map<String, Long> networks = countByMutualiaWifiNetwork(safeClients);

                log.info(
                                "Clientes WiFi detectados: total={}, grupos={}, redes MUTUALIA-WIFI={}",
                                safeClients.size(),
                                groups,
                                networks);
        }

        private String normalize(String value) {

                if (value == null) {

                        return "";
                }

                return value.trim().toUpperCase();
        }

        private Map<String, Long> countByGroup(List<ArubaWifiClientInfo> clients) {

                return clients.stream().collect(Collectors.groupingBy(
                                clientInfo -> normalize(clientInfo.getGroupName()),
                                LinkedHashMap::new,
                                Collectors.counting()));
        }

        private Map<String, Long> countByMutualiaWifiNetwork(List<ArubaWifiClientInfo> clients) {

                return clients.stream()
                                .filter(clientInfo -> "MUTUALIA-WIFI".equals(normalize(clientInfo.getGroupName())))
                                .collect(Collectors.groupingBy(clientInfo -> normalize(clientInfo.getNetwork()),
                                                LinkedHashMap::new,
                                                Collectors.counting()));
        }

        public static class WifiClientMetrics {

                private final int totalWifiClients;
                private final int mutualiaApsClients;
                private final int mutualiaWifiClients;
                private final int mutualiaLangileakClients;
                private final int mutualiaClients;
                private final int mutualiaRedInternaClients;
                private final int mutualiaRedExternaClients;
                private final int mutualiaKorporatiboaClients;
                private final int wifiPacsClients;
                private final int mutVideoClients;

                public WifiClientMetrics(
                                int totalWifiClients,
                                int mutualiaApsClients,
                                int mutualiaWifiClients,
                                int mutualiaLangileakClients,
                                int mutualiaClients,
                                int mutualiaRedInternaClients,
                                int mutualiaRedExternaClients,
                                int mutualiaKorporatiboaClients,
                                int wifiPacsClients,
                                int mutVideoClients) {

                        this.totalWifiClients = totalWifiClients;
                        this.mutualiaApsClients = mutualiaApsClients;
                        this.mutualiaWifiClients = mutualiaWifiClients;
                        this.mutualiaLangileakClients = mutualiaLangileakClients;
                        this.mutualiaClients = mutualiaClients;
                        this.mutualiaRedInternaClients = mutualiaRedInternaClients;
                        this.mutualiaRedExternaClients = mutualiaRedExternaClients;
                        this.mutualiaKorporatiboaClients = mutualiaKorporatiboaClients;
                        this.wifiPacsClients = wifiPacsClients;
                        this.mutVideoClients = mutVideoClients;
                }

                public int getTotalWifiClients() {
                        return totalWifiClients;
                }

                public int getMutualiaApsClients() {
                        return mutualiaApsClients;
                }

                public int getMutualiaWifiClients() {
                        return mutualiaWifiClients;
                }

                public int getMutualiaLangileakClients() {
                        return mutualiaLangileakClients;
                }

                public int getMutualiaClients() {
                        return mutualiaClients;
                }

                public int getMutualiaRedInternaClients() {
                        return mutualiaRedInternaClients;
                }

                public int getMutualiaRedExternaClients() {
                        return mutualiaRedExternaClients;
                }

                public int getMutualiaKorporatiboaClients() {
                        return mutualiaKorporatiboaClients;
                }

                public int getWifiPacsClients() {
                        return wifiPacsClients;
                }

                public int getMutVideoClients() {
                        return mutVideoClients;
                }
        }
}

