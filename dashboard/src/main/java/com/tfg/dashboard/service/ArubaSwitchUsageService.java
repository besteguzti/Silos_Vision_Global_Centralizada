package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.client.ArubaApiClient;
import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.model.ArubaSwitchClientUsage;
import com.tfg.dashboard.model.ArubaSwitchInterfaceUsageHistory;
import com.tfg.dashboard.repository.ArubaSwitchClientUsageRepository;
import com.tfg.dashboard.repository.ArubaSwitchInterfaceUsageHistoryRepository;

/**
 * Gestiona métricas de uso de switches Aruba.
 *
 * Consulta puertos en down, persiste el agregado actual y guarda histórico para
 * detectar switches infrautilizados durante una ventana temporal.
 */
@Service
public class ArubaSwitchUsageService {

        private final ArubaApiClient client;
        private final ArubaSwitchClientUsageRepository switchClientUsageRepository;
        private final ArubaSwitchInterfaceUsageHistoryRepository switchInterfaceUsageHistoryRepository;
        private final KpiProperties kpiProperties;

        public ArubaSwitchUsageService(
                        ArubaApiClient client,
                        ArubaSwitchClientUsageRepository switchClientUsageRepository,
                        ArubaSwitchInterfaceUsageHistoryRepository switchInterfaceUsageHistoryRepository,
                        KpiProperties kpiProperties) {

                this.client = client;
                this.switchClientUsageRepository = switchClientUsageRepository;
                this.switchInterfaceUsageHistoryRepository = switchInterfaceUsageHistoryRepository;
                this.kpiProperties = kpiProperties;
        }

        public List<ArubaSwitchClientUsage> getSwitchClientUsage() {

                return switchClientUsageRepository.findAll();
        }

        /**
         * Devuelve switches que mantienen demasiadas interfaces sin uso durante
         * el periodo configurado.
         */
        public List<ArubaSwitchClientUsage> getUnderusedSwitches() {

                LocalDateTime limitDate = LocalDateTime.now().minusDays(kpiProperties.getAruba().getUnderusedSwitchDays());

                List<String> associatedDevices = switchInterfaceUsageHistoryRepository
                                .findDevicesAlwaysOverDownInterfaceLimitSince(
                                                "Up",
                                                kpiProperties.getAruba().getUnderusedSwitchDownInterfaceLimit(),
                                                limitDate);

                if (associatedDevices.isEmpty()) {
                        return List.of();
                }

                return switchClientUsageRepository.findByAssociatedDeviceInOrderByDownInterfacesDescAssociatedDeviceAsc(
                                associatedDevices);
        }

        /**
         * Sincroniza uso de switches consultando primero el inventario Aruba.
         */
        public void syncSwitchClientUsage() {

                syncSwitchClientUsage(client.getMonitoringSwitchesList());
        }

        /**
         * Actualiza el uso actual y el histórico de interfaces a partir de una
         * lista de switches ya obtenida por el inventario.
         */
        public void syncSwitchClientUsage(List<ArubaSwitchInfo> switches) {

                LocalDateTime now = LocalDateTime.now();
                Map<String, ArubaSwitchClientUsage> usageByDevice = new LinkedHashMap<>();

                for (ArubaSwitchInfo switchInfo : switches == null ? List.<ArubaSwitchInfo>of() : switches) {

                        String serial = switchInfo.getSerial();

                        if (serial == null || serial.isBlank()) {

                                continue;
                        }

                        ArubaSwitchClientUsage usage = new ArubaSwitchClientUsage();

                        usage.setAssociatedDevice(serial);
                        usage.setAssociatedDeviceName(switchInfo.getHostname());
                        usage.setAssociatedDeviceMac(switchInfo.getMacAddress());
                        usage.setDeviceStatus(switchInfo.getDeviceStatus());
                        usage.setDownInterfaces(client.countSwitchPortsDown(serial));
                        usageByDevice.put(serial, usage);
                }

                for (ArubaSwitchClientUsage existing : switchClientUsageRepository.findAll()) {

                        if (!usageByDevice.containsKey(existing.getAssociatedDevice())) {

                                existing.setDownInterfaces(0);
                                existing.setUpdatedAt(now);

                                switchClientUsageRepository.save(existing);
                        }
                }

                for (ArubaSwitchClientUsage aggregate : usageByDevice.values()) {

                        ArubaSwitchClientUsage entity = switchClientUsageRepository
                                        .findByAssociatedDevice(aggregate.getAssociatedDevice())
                                        .orElseGet(ArubaSwitchClientUsage::new);

                        entity.setAssociatedDevice(aggregate.getAssociatedDevice());
                        entity.setAssociatedDeviceName(aggregate.getAssociatedDeviceName());
                        entity.setAssociatedDeviceMac(aggregate.getAssociatedDeviceMac());
                        entity.setDeviceStatus(aggregate.getDeviceStatus());
                        entity.setDownInterfaces(aggregate.getDownInterfaces());
                        entity.setUpdatedAt(now);

                        switchClientUsageRepository.save(entity);

                        ArubaSwitchInterfaceUsageHistory history = new ArubaSwitchInterfaceUsageHistory();

                        history.setAssociatedDevice(aggregate.getAssociatedDevice());
                        history.setAssociatedDeviceName(aggregate.getAssociatedDeviceName());
                        history.setAssociatedDeviceMac(aggregate.getAssociatedDeviceMac());
                        history.setDeviceStatus(aggregate.getDeviceStatus());
                        history.setDownInterfaces(aggregate.getDownInterfaces());
                        history.setObservedAt(now);

                        switchInterfaceUsageHistoryRepository.save(history);
                }
        }
}

