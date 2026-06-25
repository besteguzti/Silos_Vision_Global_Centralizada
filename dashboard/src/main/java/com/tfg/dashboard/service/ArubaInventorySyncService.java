package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tfg.dashboard.client.ArubaApiClient;
import com.tfg.dashboard.dto.ArubaFirmwareSwarmsResult;
import com.tfg.dashboard.dto.ArubaApInfo;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.dto.ArubaWifiClientInfo;
import com.tfg.dashboard.model.AccessPoint;
import com.tfg.dashboard.model.ArubaDashboardMetrics;
import com.tfg.dashboard.model.ArubaSwitch;
import com.tfg.dashboard.repository.AccessPointRepository;
import com.tfg.dashboard.repository.ArubaDashboardMetricsRepository;
import com.tfg.dashboard.repository.ArubaSwitchRepository;

/**
 * Coordina la sincronización de inventario Aruba.
 *
 * Consulta Aruba Central mediante ArubaApiClient y persiste APs, switches,
 * firmware y métricas agregadas en MySQL para que el dashboard no dependa de
 * llamadas en tiempo real en cada render.
 */
@Service
public class ArubaInventorySyncService {

        private static final Logger log = LoggerFactory.getLogger(ArubaInventorySyncService.class);
        private static final long METRICS_ID = 1L;

        private final ArubaApiClient client;
        private final AccessPointRepository accessPointRepository;
        private final ArubaSwitchRepository arubaSwitchRepository;
        private final ArubaDashboardMetricsRepository dashboardMetricsRepository;
        private final ArubaWifiClientAggregationService wifiClientAggregationService;
        private final ArubaSwitchUsageService switchUsageService;

        public ArubaInventorySyncService(
                        ArubaApiClient client,
                        AccessPointRepository accessPointRepository,
                        ArubaSwitchRepository arubaSwitchRepository,
                        ArubaDashboardMetricsRepository dashboardMetricsRepository,
                        ArubaWifiClientAggregationService wifiClientAggregationService,
                        ArubaSwitchUsageService switchUsageService) {

                this.client = client;
                this.accessPointRepository = accessPointRepository;
                this.arubaSwitchRepository = arubaSwitchRepository;
                this.dashboardMetricsRepository = dashboardMetricsRepository;
                this.wifiClientAggregationService = wifiClientAggregationService;
                this.switchUsageService = switchUsageService;
        }

        public List<ArubaApInfo> getApsList() {

                return client.getApsList();
        }

        public List<AccessPoint> getStoredAccessPoints() {

                return accessPointRepository.findAll();
        }

        public List<ArubaSwitchInfo> getSwitchesList() {

                return client.getMonitoringSwitchesList();
        }

        public List<ArubaSwitch> getStoredSwitches() {

                return arubaSwitchRepository.findAll();
        }

        public List<ArubaWifiClientInfo> getWifiClientsList() {

                return client.getWifiClientsList();
        }

        /**
         * Sincroniza Access Points y métricas de firmware de APs.
         */
        public void syncAccessPoints() {

                List<ArubaApInfo> aps = client.getApsList();

                syncAccessPoints(aps);
                syncFirmwareMetrics(client.getFirmwareSwarms());
        }

        /**
         * Sincroniza switches desde monitoring y su estado de firmware.
         */
        public void syncSwitches() {

                List<ArubaSwitchInfo> switches = client.getMonitoringSwitchesList();

                syncSwitches(switches);
                syncSwitchFirmwareState(client.getSwitchesList());
        }

        /**
         * Ejecuta una sincronización completa de Aruba con una única lectura de
         * cada bloque principal de API.
         */
        public void syncAll() {

                List<ArubaApInfo> aps = client.getApsList();
                ArubaFirmwareSwarmsResult firmwareSwarms = client.getFirmwareSwarms();
                List<ArubaSwitchInfo> switches = client.getMonitoringSwitchesList();
                List<ArubaSwitchInfo> firmwareSwitches = client.getSwitchesList();
                List<ArubaWifiClientInfo> wifiClients = client.getWifiClientsList();

                syncAccessPoints(aps);
                syncSwitches(switches);
                syncSwitchFirmwareState(firmwareSwitches);
                switchUsageService.syncSwitchClientUsage(switches);
                syncDashboardMetrics(firmwareSwarms, wifiClients);
        }

        private void syncAccessPoints(List<ArubaApInfo> aps) {

                LocalDateTime now = LocalDateTime.now();

                for (ArubaApInfo ap : aps == null ? List.<ArubaApInfo>of() : aps) {

                        String serial = ap.getSerial();

                        if (serial == null || serial.isBlank()) {
                                continue;
                        }

                        AccessPoint entity = accessPointRepository.findBySerial(serial).orElseGet(AccessPoint::new);

                        entity.setName(ap.getName());
                        entity.setStatus(ap.getStatus());
                        entity.setIpAddress(ap.getIpAddress());
                        entity.setPublicIpAddress(ap.getPublicIpAddress());
                        entity.setSerial(serial);
                        entity.setSite(ap.getSite());
                        entity.setFirmwareVersion(ap.getFirmwareVersion());
                        entity.setMacaddr(ap.getMacaddr());
                        entity.setSwarmName(ap.getSwarmName());

                        if (entity.getFirstSeenAt() == null) {
                                entity.setFirstSeenAt(now);
                        }

                        LocalDateTime realLastSeenAt = resolveAccessPointLastSeenAt(ap, now);

                        if (realLastSeenAt != null) {
                                entity.setLastSeenAt(realLastSeenAt);
                        } else if (entity.getLastSeenAt() == null) {
                                log.debug(
                                                "AP {} no trae fecha real de ultimo contacto desde Aruba; lastSeenAt queda sin dato.",
                                                serial);
                        }

                        accessPointRepository.save(entity);
                }
        }

        private LocalDateTime resolveAccessPointLastSeenAt(ArubaApInfo ap,LocalDateTime syncTime) {

                if (ap.getLastSeenAt() != null) {
                        return ap.getLastSeenAt();
                }

                // Si Aruba informa el AP como Up en la respuesta actual, se considera visto en
                // esta consulta. Para APs Down no se usa syncTime, porque eso solo indicaria
                // cuando nuestra aplicacion guardo el inventario, no cuando Aruba vio el AP.
                if (ap.getStatus() != null && ap.getStatus().equalsIgnoreCase("Up")) {
                        return syncTime;
                }

                return null;
        }

        private void syncSwitches(List<ArubaSwitchInfo> switches) {

                LocalDateTime now = LocalDateTime.now();

                for (ArubaSwitchInfo switchInfo : switches == null ? List.<ArubaSwitchInfo>of() : switches) {

                        String serial = switchInfo.getSerial();

                        if (serial == null || serial.isBlank()) {
                                continue;
                        }

                        ArubaSwitch entity = arubaSwitchRepository.findBySerial(serial).orElseGet(ArubaSwitch::new);

                        entity.setSerial(serial);
                        entity.setMacAddress(switchInfo.getMacAddress());
                        entity.setHostname(switchInfo.getHostname());
                        entity.setModel(switchInfo.getModel());
                        entity.setDeviceStatus(switchInfo.getDeviceStatus());
                        entity.setUpgradeRequired(switchInfo.isUpgradeRequired());
                        entity.setStatusState(switchInfo.getStatusState());

                        if (entity.getFirstSeenAt() == null) {
                                entity.setFirstSeenAt(now);
                        }

                        entity.setLastSeenAt(now);

                        arubaSwitchRepository.save(entity);
                }
        }

        private void syncSwitchFirmwareState(List<ArubaSwitchInfo> firmwareSwitches) {

                for (ArubaSwitchInfo firmwareSwitch : firmwareSwitches == null ? List.<ArubaSwitchInfo>of() : firmwareSwitches) {

                        String serial = firmwareSwitch.getSerial();

                        if (serial == null || serial.isBlank()) {

                                continue;
                        }

                        arubaSwitchRepository
                                        .findBySerial(serial)
                                        .ifPresent(entity -> {
                                                entity.setUpgradeRequired(firmwareSwitch.isUpgradeRequired());
                                                entity.setStatusState(firmwareSwitch.getStatusState());
                                                arubaSwitchRepository.save(entity);
                                        });
                }
        }

        private void syncFirmwareMetrics(ArubaFirmwareSwarmsResult firmwareSwarms) {

                ArubaDashboardMetrics metrics = getOrCreateMetrics();

                Optional<Integer> firmwareOutdated = countFirmwareOutdated(firmwareSwarms);

                if (firmwareOutdated.isEmpty()) {
                        log.warn("No se actualiza firmwareOutdated porque firmware Aruba no ha devuelto datos validos: {}",
                                        firmwareSwarms == null ? "sin resultado" : firmwareSwarms.getStatus());
                        return;
                }

                metrics.setFirmwareOutdated(firmwareOutdated.get());
                metrics.setUpdatedAt(LocalDateTime.now());

                dashboardMetricsRepository.save(metrics);
        }

        /**
         * Actualiza la fila agregada que resume firmware y clientes WiFi para
         * construir el resumen Aruba.
         */
        private void syncDashboardMetrics(ArubaFirmwareSwarmsResult firmwareSwarms,List<ArubaWifiClientInfo> wifiClients) {

                ArubaDashboardMetrics metrics = getOrCreateMetrics();
                ArubaWifiClientAggregationService.WifiClientMetrics clientMetrics =
                                wifiClientAggregationService.buildMetrics(wifiClients);

                countFirmwareOutdated(firmwareSwarms).ifPresent(metrics::setFirmwareOutdated);
                metrics.setTotalWifiClients(clientMetrics.getTotalWifiClients());
                metrics.setMutualiaApsClients(clientMetrics.getMutualiaApsClients());
                metrics.setMutualiaWifiClients(clientMetrics.getMutualiaWifiClients());
                metrics.setMutualiaLangileakClients(clientMetrics.getMutualiaLangileakClients());
                metrics.setMutualiaClients(clientMetrics.getMutualiaClients());
                metrics.setMutualiaRedInternaClients(clientMetrics.getMutualiaRedInternaClients());
                metrics.setMutualiaRedExternaClients(clientMetrics.getMutualiaRedExternaClients());
                metrics.setMutualiaKorporatiboaClients(clientMetrics.getMutualiaKorporatiboaClients());
                metrics.setWifiPacsClients(clientMetrics.getWifiPacsClients());
                metrics.setMutVideoClients(clientMetrics.getMutVideoClients());
                metrics.setUpdatedAt(LocalDateTime.now());

                dashboardMetricsRepository.save(metrics);
                wifiClientAggregationService.logWifiClientBreakdown(wifiClients);
        }

        private ArubaDashboardMetrics getOrCreateMetrics() {

                return dashboardMetricsRepository
                                .findById(METRICS_ID)
                                .orElseGet(() -> {
                                        ArubaDashboardMetrics newMetrics = new ArubaDashboardMetrics();
                                        newMetrics.setId(METRICS_ID);
                                        return newMetrics;
                                });
        }

        private Optional<Integer> countFirmwareOutdated(ArubaFirmwareSwarmsResult firmwareSwarms) {

                int firmwareOutdated = 0;

                if (firmwareSwarms == null || !firmwareSwarms.hasPayload()) {

                        return Optional.empty();
                }

                JsonNode swarms = firmwareSwarms.getPayload().get("swarms");

                if (swarms == null || !swarms.isArray()) {

                        return Optional.empty();
                }

                for (JsonNode swarm : swarms) {

                        String state = swarm.path("status").path("state").asText();

                        if (state.trim().equalsIgnoreCase("UPGRADE_REQUIRED")) {

                                firmwareOutdated++;
                        }
                }

                return Optional.of(firmwareOutdated);
        }
}

