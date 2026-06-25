package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.AccessPointStatusDto;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.KpiStatus;
import com.tfg.dashboard.dto.SwitchStatusDto;
import com.tfg.dashboard.model.ArubaNetworkStatusHistory;
import com.tfg.dashboard.model.TransversalKpiHistory;
import com.tfg.dashboard.repository.ArubaNetworkStatusHistoryRepository;
import com.tfg.dashboard.repository.TransversalKpiHistoryRepository;

/**
 * Calcula el índice normalizado de salud Aruba.
 *
 * Aplica la tabla de afecciones parciales definida para Aruba, genera motivos
 * explicativos, guarda historicos y prepara valores para el analisis
 * transversal.
 */
@Service
public class ArubaNetworkStatusService {

        private static final String GREEN = "GREEN";

        private final ArubaNetworkStatusHistoryRepository networkStatusHistoryRepository;
        private final TransversalKpiHistoryRepository transversalKpiHistoryRepository;
        private final KpiProperties kpiProperties;

        public ArubaNetworkStatusService(
                        ArubaNetworkStatusHistoryRepository networkStatusHistoryRepository,
                        TransversalKpiHistoryRepository transversalKpiHistoryRepository,
                        KpiProperties kpiProperties) {

                this.networkStatusHistoryRepository = networkStatusHistoryRepository;
                this.transversalKpiHistoryRepository = transversalKpiHistoryRepository;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Calcula porcentaje, color global y motivos del índice de salud Aruba.
         */
        public ArubaNetworkStatusDto buildNetworkStatusDetails(
                        int totalAps,
                        int downAps,
                        int inactiveAps,
                        int pendingFirmwareAps,
                        int totalWifiClients,
                        int mutualiaApsClients,
                        int mutualiaWifiClients,
                        int totalSwitches,
                        int downSwitches,
                        int pendingFirmwareSwitches,
                        int underusedSwitches,
                        int arubaOpenTickets) {

                ArubaAffectationCalculator.Result affectation = ArubaAffectationCalculator.calculate(
                                new ArubaAffectationCalculator.Input(
                                                totalAps,
                                                downAps,
                                                inactiveAps,
                                                pendingFirmwareAps,
                                                totalWifiClients,
                                                mutualiaApsClients,
                                                mutualiaWifiClients,
                                                totalSwitches,
                                                downSwitches,
                                                pendingFirmwareSwitches,
                                                underusedSwitches,
                                                arubaOpenTickets),
                                kpiProperties);

                AccessPointStatusDto accessPointStatus = buildAccessPointStatus(
                                totalAps,
                                downAps,
                                inactiveAps,
                                pendingFirmwareAps,
                                totalWifiClients,
                                mutualiaApsClients,
                                mutualiaWifiClients,
                                affectation);

                SwitchStatusDto switchStatus = buildSwitchStatus(
                                totalSwitches,
                                downSwitches,
                                pendingFirmwareSwitches,
                                affectation);

                ArubaNetworkStatusDto status = new ArubaNetworkStatusDto();

                status.setPercentage(affectation.totalAffection());
                status.setColor(affectation.color());
                status.setAccessPointStatus(accessPointStatus);
                status.setSwitchStatus(switchStatus);
                status.setReasons(affectation.reasons());
                status.setIndicatorStatuses(affectation.indicatorStatuses());
                status.setAffectedService(!GREEN.equals(affectation.color()));
                status.setCriticalCondition("RED".equals(affectation.color()));
                status.setTechnicalDegradationValue(affectation.totalAffection());
                status.setTransversalReady(true);

                return status;
        }

        /**
         * Convierte el índice de salud Aruba en un KPI homogeneo con componentes
         * de APs y switches.
         */
        public KpiResultDto buildNetworkStatusKpi(ArubaNetworkStatusDto details,LocalDateTime timestamp,String freshness) {

                return new KpiResultDto(
                                "aruba_network_affectation",
                                "Índice de salud Aruba",
                                details.getPercentage(),
                                KpiStatus.from(details.getColor()),
                                "Afeccion normalizada de la red Aruba.",
                                "Suma de afecciones parciales: tickets Aruba, APs caidos, firmware AP, APs inactivos, clientes WiFi, switches apagados, con upgrade pendiente e infrautilizados.",
                                timestamp,
                                freshness,
                                details.getPercentage(),
                                List.of(
                                                new KpiResultDto(
                                                                "aruba_access_points_status",
                                                                "Estado parcial Access Points",
                                                                details.getAccessPointStatus().getPercentageContribution(),
                                                                KpiStatus.from(details.getAccessPointStatus().getColor()),
                                                                String.join("; ", details.getAccessPointStatus().getReasons()),
                                                                "Suma de APs caidos, firmware AP, APs inactivos y clientes WiFi.",
                                                                timestamp,
                                                                freshness,
                                                                details.getAccessPointStatus().getPercentageContribution(),
                                                                List.of()),
                                                new KpiResultDto(
                                                                "aruba_switches_status",
                                                                "Estado parcial Switches",
                                                                details.getSwitchStatus().getPercentageContribution(),
                                                                KpiStatus.from(details.getSwitchStatus().getColor()),
                                                                String.join("; ", details.getSwitchStatus().getReasons()),
                                                                "Suma de switches apagados, con upgrade pendiente e infrautilizados.",
                                                                timestamp,
                                                                freshness,
                                                                details.getSwitchStatus().getPercentageContribution(),
                                                                List.of())));
        }

        /**
         * Persiste el estado calculado y sus KPIs transversales derivados.
         */
        public void saveNetworkStatusSnapshot(ArubaNetworkStatusDto status,LocalDateTime collectedAt) {

                ArubaNetworkStatusHistory history = new ArubaNetworkStatusHistory();

                history.setPercentage(status.getPercentage());
                history.setColor(status.getColor());
                history.setAccessPointContribution(status.getAccessPointStatus().getPercentageContribution());
                history.setAccessPointColor(status.getAccessPointStatus().getColor());
                history.setSwitchContribution(status.getSwitchStatus().getPercentageContribution());
                history.setSwitchColor(status.getSwitchStatus().getColor());
                history.setAffectedService(status.isAffectedService());
                history.setCriticalCondition(status.isCriticalCondition());
                history.setTechnicalDegradationValue(status.getTechnicalDegradationValue());
                history.setReasons(String.join(" | ", status.getReasons()));
                history.setCollectedAt(collectedAt);

                networkStatusHistoryRepository.save(history);
                saveArubaTransversalSnapshot(status, collectedAt);
        }

        /**
         * Ensambla el detalle parcial de Access Points a partir del calculo
         * centralizado. Los clientes Mutualia se mantienen en el DTO para no
         * romper el contrato del resumen, aunque la afeccion global de clientes
         * WiFi se evalua sobre el total.
         */
        private AccessPointStatusDto buildAccessPointStatus(
                        int totalAps,
                        int downAps,
                        int inactiveAps,
                        int pendingFirmwareAps,
                        int totalWifiClients,
                        int mutualiaApsClients,
                        int mutualiaWifiClients,
                        ArubaAffectationCalculator.Result affectation) {

                AccessPointStatusDto status = new AccessPointStatusDto();

                status.setPercentageContribution(affectation.accessPointAffection());
                status.setColor(affectation.accessPointColor());
                status.setTotalAps(totalAps);
                status.setDownAps(downAps);
                status.setInactiveAps(inactiveAps);
                status.setPendingFirmwareAps(pendingFirmwareAps);
                status.setTotalWifiClients(totalWifiClients);
                status.setMutualiaApsClients(mutualiaApsClients);
                status.setMutualiaWifiClients(mutualiaWifiClients);
                status.setReasons(affectation.accessPointReasons());

                return status;
        }

        /**
         * Ensambla el detalle parcial de switches. El firmware con upgrade
         * pendiente participa en la afeccion junto con switches apagados e
         * infrautilizados.
         */
        private SwitchStatusDto buildSwitchStatus(
                        int totalSwitches,
                        int downSwitches,
                        int pendingFirmwareSwitches,
                        ArubaAffectationCalculator.Result affectation) {

                SwitchStatusDto status = new SwitchStatusDto();

                status.setPercentageContribution(affectation.switchAffection());
                status.setColor(affectation.switchColor());
                status.setTotalSwitches(totalSwitches);
                status.setDownSwitches(downSwitches);
                status.setPendingFirmwareSwitches(pendingFirmwareSwitches);
                status.setReasons(affectation.switchReasons());

                return status;
        }

        /**
         * Guarda KPIs especificos de Aruba en la tabla transversal para que el
         * modulo de analisis pueda compararlos con otras fuentes.
         */
        private void saveArubaTransversalSnapshot(ArubaNetworkStatusDto status,LocalDateTime collectedAt) {

                List<TransversalKpiHistory> histories = List.of(
                                transversalHistory(
                                                "aruba_network_affectation",
                                                "Afectacion de red Aruba",
                                                "%",
                                                (double) status.getPercentage(),
                                                collectedAt),
                                transversalHistory(
                                                "aruba_network_degradation",
                                                "Degradacion de red Aruba",
                                                "indice 0-100",
                                                (double) status.getTechnicalDegradationValue(),
                                                collectedAt),
                                transversalHistory(
                                                "aruba_network_health",
                                                "Salud de red Aruba",
                                                "%",
                                                (double) (100 - status.getPercentage()),
                                                collectedAt));
                transversalKpiHistoryRepository.saveAll(Objects.requireNonNull(histories));
        }

        private TransversalKpiHistory transversalHistory(
                        String code,
                        String name,
                        String unit,
                        Double value,
                        LocalDateTime collectedAt) {

                TransversalKpiHistory history = new TransversalKpiHistory();

                history.setKpiCode(code);
                history.setKpiName(name);
                history.setUnit(unit);
                history.setValue(value);
                history.setCollectedAt(collectedAt);

                return history;
        }
}
