package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tfg.dashboard.dto.ArubaApInfo;
import com.tfg.dashboard.dto.ArubaApAnnotationDto;
import com.tfg.dashboard.dto.ArubaApAnnotationRequest;
import com.tfg.dashboard.dto.ArubaInactiveApDto;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.dto.ArubaWifiClientInfo;
import com.tfg.dashboard.model.AccessPoint;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.ArubaSwitch;
import com.tfg.dashboard.model.ArubaSwitchClientUsage;

/**
 * Fachada principal de Aruba para controladores, scheduler y servicios de
 * dashboard.
 *
 * No concentra ya toda la lógica: delega sincronización, clientes WiFi,
 * switches, índice de salud Aruba y construcción del resumen en servicios
 * especializados.
 */
@Service
public class ArubaService {

        private static final Logger log = LoggerFactory.getLogger(ArubaService.class);

        private final ArubaInventorySyncService inventorySyncService;
        private final ArubaWifiClientAggregationService wifiClientAggregationService;
        private final ArubaSwitchUsageService switchUsageService;
        private final ArubaNetworkStatusService networkStatusService;
        private final ArubaSummaryService summaryService;

        public ArubaService(
                        ArubaInventorySyncService inventorySyncService,
                        ArubaWifiClientAggregationService wifiClientAggregationService,
                        ArubaSwitchUsageService switchUsageService,
                        ArubaNetworkStatusService networkStatusService,
                        ArubaSummaryService summaryService) {

                this.inventorySyncService = inventorySyncService;
                this.wifiClientAggregationService = wifiClientAggregationService;
                this.switchUsageService = switchUsageService;
                this.networkStatusService = networkStatusService;
                this.summaryService = summaryService;
        }

        /**
         * Devuelve el resumen persistido que consume la página Aruba.
         */
        public ArubaSummary getSummary() {

                return summaryService.getSummary();
        }

        /**
         * Devuelve el estado normalizado de red Aruba usado por el dashboard y
         * por los KPIs transversales.
         */
        public ArubaNetworkStatusDto getNetworkStatus() {

                return summaryService.getNetworkStatus();
        }

        public List<ArubaInactiveApDto> getInactiveAps() {

                return summaryService.getInactiveAps();
        }

        public ArubaApAnnotationDto saveInactiveApAnnotation(
                        String serial,
                        ArubaApAnnotationRequest request) {

                return summaryService.saveInactiveApAnnotation(serial, request);
        }

        public List<ArubaApInfo> getApsList() {

                return inventorySyncService.getApsList();
        }

        public List<AccessPoint> getStoredAccessPoints() {

                return inventorySyncService.getStoredAccessPoints();
        }

        public List<ArubaSwitchInfo> getSwitchesList() {

                return inventorySyncService.getSwitchesList();
        }

        public List<ArubaSwitch> getStoredSwitches() {

                return inventorySyncService.getStoredSwitches();
        }

        public List<ArubaSwitchClientUsage> getSwitchClientUsage() {

                return switchUsageService.getSwitchClientUsage();
        }

        public List<ArubaSwitchClientUsage> getUnderusedSwitches() {

                return switchUsageService.getUnderusedSwitches();
        }

        public List<ArubaWifiClientInfo> getWifiClientsList() {

                return inventorySyncService.getWifiClientsList();
        }

        public Map<String, Object> getWifiClientsDiagnostics() {

                return wifiClientAggregationService.buildDiagnostics(getWifiClientsList());
        }

        public void syncAccessPoints() {

                inventorySyncService.syncAccessPoints();
        }

        public void syncSwitches() {

                inventorySyncService.syncSwitches();
        }

        public void syncSwitchClientUsage() {

                switchUsageService.syncSwitchClientUsage();
        }

        /**
         * Ejecuta la sincronización completa de Aruba y guarda el snapshot del
         * índice de salud Aruba sin bloquear la sincronización si falla el histórico.
         */
        public void syncAll() {

                inventorySyncService.syncAll();

                try {
                        networkStatusService.saveNetworkStatusSnapshot(getNetworkStatus(), LocalDateTime.now());
                } catch (Exception exception) {

                        // El histórico de afectacion alimenta el análisis transversal, pero no debe
                        // bloquear la sincronizacion real de APs, switches y clientes Aruba.
                        log.error("Error guardando histórico del índice de salud Aruba", exception);
                }
        }
}

