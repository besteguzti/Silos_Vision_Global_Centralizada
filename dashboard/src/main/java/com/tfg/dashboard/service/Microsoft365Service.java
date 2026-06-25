package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.KpiStatus;
import com.tfg.dashboard.dto.Microsoft365HealthStatusDto;
import com.tfg.dashboard.dto.Microsoft365IndicatorStatusDto;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

/**
 * Servicio de Microsoft 365 simulado.
 *
 * Genera métricas dinámicas, lee snapshots persistidos y calcula el índice de
 * salud Microsoft 365 a partir de capacidad, identidad, seguridad y
 * dispositivos.
 */
@Service
public class Microsoft365Service {

        private final Random random = new Random();

        private static final String GREEN = "GREEN";

        private static final String YELLOW = "YELLOW";

        private static final String RED = "RED";

        private final Microsoft365MetricsHistoryRepository metricsHistoryRepository;
        private final GlpiPlatformTicketService glpiPlatformTicketService;
        private final KpiProperties kpiProperties;

        public Microsoft365Service(
                        Microsoft365MetricsHistoryRepository metricsHistoryRepository,
                        GlpiPlatformTicketService glpiPlatformTicketService,
                        KpiProperties kpiProperties) {

                this.metricsHistoryRepository = metricsHistoryRepository;
                this.glpiPlatformTicketService = glpiPlatformTicketService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Devuelve el último snapshot Microsoft 365 almacenado en MySQL o
         * NO_DATA si aún no existe histórico.
         */
        public Microsoft365Summary getSummary() {

                return metricsHistoryRepository
                                .findTopByOrderByCollectedAtDesc()
                                .map(this::mapHistoryToSummary)
                                .orElseGet(this::noDataSummary);
        }

        /**
         * Genera KPIs simulados de uso, licencias, servicios, seguridad,
         * aplicaciones e Intune para persistirlos como snapshot.
         */
        public Microsoft365Summary generateSimulatedSummary() {

                Microsoft365Summary summary = new Microsoft365Summary();

                int activeUsers = 1200 + random.nextInt(500);

                int unassignedLicenses = 20 + random.nextInt(120);

                String outlookStatus = randomServiceStatus();

                String teamsStatus = randomServiceStatus();

                String sharePointStatus = randomServiceStatus();

                int nearlyFullMailboxes = random.nextInt(40);

                int emailsQuarantined = 20 + random.nextInt(120);

                int sharePointStoragePercent = 45 + random.nextInt(50);

                int riskyUsers = random.nextInt(21);

                int failedSignIns = random.nextInt(31);

                int usersWithoutMfa = Math.min(activeUsers, random.nextInt(11));

                int appsSecretsExpiringSoon = random.nextInt(15);

                int unusedApplications = random.nextInt(25);

                int highPrivilegeApplications = random.nextInt(12);

                int nonCompliantDevices = random.nextInt(80);

                int outdatedWindowsDevices = random.nextInt(60);

                int devicesWithoutEncryption = 0;

                int staleDevices = random.nextInt(50);

                int microsoft365OpenTickets =
                                glpiPlatformTicketService.getMicrosoft365OpenTickets();

                Microsoft365HealthStatusDto healthDetails = calculateHealthDetails(
                                activeUsers,
                                unassignedLicenses,
                                outlookStatus,
                                teamsStatus,
                                sharePointStatus,
                                nearlyFullMailboxes,
                                emailsQuarantined,
                                sharePointStoragePercent,
                                riskyUsers,
                                failedSignIns,
                                usersWithoutMfa,
                                appsSecretsExpiringSoon,
                                unusedApplications,
                                highPrivilegeApplications,
                                nonCompliantDevices,
                                microsoft365OpenTickets,
                                outdatedWindowsDevices,
                                devicesWithoutEncryption,
                                staleDevices);

                summary.setActiveUsers(
                                activeUsers);

                summary.setUnassignedLicenses(
                                unassignedLicenses);

                summary.setOutlookStatus(
                                outlookStatus);

                summary.setTeamsStatus(
                                teamsStatus);

                summary.setSharePointStatus(
                                sharePointStatus);

                summary.setNearlyFullMailboxes(
                                nearlyFullMailboxes);

                summary.setEmailsQuarantined(
                                emailsQuarantined);

                summary.setSharePointStoragePercent(
                                sharePointStoragePercent);

                summary.setRiskyUsers(
                                riskyUsers);

                summary.setFailedSignIns(
                                failedSignIns);

                summary.setUsersWithoutMfa(
                                usersWithoutMfa);

                summary.setAppsSecretsExpiringSoon(
                                appsSecretsExpiringSoon);

                summary.setUnusedApplications(
                                unusedApplications);

                summary.setHighPrivilegeApplications(
                                highPrivilegeApplications);

                summary.setNonCompliantDevices(
                                nonCompliantDevices);
                summary.setMicrosoft365OpenTickets(
                                microsoft365OpenTickets);

                summary.setOutdatedWindowsDevices(
                                outdatedWindowsDevices);

                summary.setDevicesWithoutEncryption(
                                devicesWithoutEncryption);

                summary.setStaleDevices(
                                staleDevices);
                summary.setMicrosoft365HealthDetails(
                                healthDetails);
                summary.setMicrosoft365HealthKpi(
                                buildMicrosoft365HealthKpi(
                                                healthDetails,
                                                LocalDateTime.now(),
                                                "SIMULATED"));

                return summary;
        }

        private Microsoft365Summary mapHistoryToSummary(
                        Microsoft365MetricsHistory history) {

                Microsoft365Summary summary = new Microsoft365Summary();

                summary.setActiveUsers(history.getActiveUsers());
                summary.setUnassignedLicenses(history.getUnassignedLicenses());
                summary.setOutlookStatus(history.getOutlookStatus());
                summary.setTeamsStatus(history.getTeamsStatus());
                summary.setSharePointStatus(history.getSharePointStatus());
                summary.setNearlyFullMailboxes(history.getNearlyFullMailboxes());
                summary.setEmailsQuarantined(history.getEmailsQuarantined());
                summary.setSharePointStoragePercent(
                                history.getSharePointStoragePercent());
                summary.setRiskyUsers(history.getRiskyUsers());
                summary.setFailedSignIns(history.getFailedSignIns());
                summary.setUsersWithoutMfa(history.getUsersWithoutMfa());
                summary.setAppsSecretsExpiringSoon(
                                history.getAppsSecretsExpiringSoon());
                summary.setUnusedApplications(history.getUnusedApplications());
                summary.setHighPrivilegeApplications(
                                history.getHighPrivilegeApplications());
                summary.setNonCompliantDevices(history.getNonCompliantDevices());
                int microsoft365OpenTickets = glpiPlatformTicketService.getMicrosoft365OpenTickets();
                summary.setMicrosoft365OpenTickets(microsoft365OpenTickets);
                summary.setOutdatedWindowsDevices(
                                history.getOutdatedWindowsDevices());
                summary.setDevicesWithoutEncryption(
                                history.getDevicesWithoutEncryption());
                summary.setStaleDevices(history.getStaleDevices());
                Microsoft365HealthStatusDto healthDetails = calculateHealthDetails(
                                history.getActiveUsers(),
                                history.getUnassignedLicenses(),
                                history.getOutlookStatus(),
                                history.getTeamsStatus(),
                                history.getSharePointStatus(),
                                history.getNearlyFullMailboxes(),
                                history.getEmailsQuarantined(),
                                history.getSharePointStoragePercent(),
                                history.getRiskyUsers(),
                                history.getFailedSignIns(),
                                history.getUsersWithoutMfa(),
                                history.getAppsSecretsExpiringSoon(),
                                history.getUnusedApplications(),
                                history.getHighPrivilegeApplications(),
                                history.getNonCompliantDevices(),
                                microsoft365OpenTickets,
                                history.getOutdatedWindowsDevices(),
                                history.getDevicesWithoutEncryption(),
                                history.getStaleDevices());

                summary.setMicrosoft365HealthDetails(healthDetails);
                summary.setLastUpdated(history.getCollectedAt());
                summary.setDataStatus(
                                calculateDataStatus(history.getCollectedAt()));
                summary.setMicrosoft365HealthKpi(
                                buildMicrosoft365HealthKpi(
                                                healthDetails,
                                                history.getCollectedAt(),
                                                summary.getDataStatus()));

                return summary;
        }

        private Microsoft365Summary noDataSummary() {

                Microsoft365Summary summary = new Microsoft365Summary();

                summary.setOutlookStatus("NO_DATA");
                summary.setTeamsStatus("NO_DATA");
                summary.setSharePointStatus("NO_DATA");
                summary.setDataStatus("NO_DATA");
                summary.setMicrosoft365HealthDetails(noDataHealthDetails());
                summary.setMicrosoft365HealthKpi(
                                buildMicrosoft365HealthKpi(
                                                summary.getMicrosoft365HealthDetails(),
                                                null,
                                                summary.getDataStatus()));

                return summary;
        }

        private String calculateDataStatus(
                        LocalDateTime collectedAt) {

                // OK: snapshot reciente.
                // STALE: existe, pero supera
                // el margen esperado.
                // NO_DATA: no hay snapshot.

                if (collectedAt == null) {

                        return "NO_DATA";
                }

                if (collectedAt.isAfter(
                                LocalDateTime.now().minusMinutes(
                                                kpiProperties.getFreshness()
                                                                .getMicrosoft365Minutes()))) {

                        return "OK";
                }

                return "STALE";
        }

        /**
         * Genera estados simulados de servicio con predominio de HEALTHY y
         * pequeñas probabilidades de degradación o incidencia.
         */
        private String randomServiceStatus() {

                int value = random.nextInt(100);

                if (value < 80) {

                        return "HEALTHY";
                }

                if (value < 95) {

                        return "DEGRADED";
                }

                return "INCIDENT";
        }

        /**
         * Normaliza los indicadores de Microsoft 365 en una escala común de
         * afección 0-100.
         */
        private Microsoft365HealthStatusDto calculateHealthDetails(
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

                Microsoft365AffectationCalculator.Result result =
                                Microsoft365AffectationCalculator.calculate(
                                                new Microsoft365AffectationCalculator.Input(
                                                                activeUsers,
                                                                unassignedLicenses,
                                                                outlookStatus,
                                                                teamsStatus,
                                                                sharePointStatus,
                                                                nearlyFullMailboxes,
                                                                emailsQuarantined,
                                                                sharePointStoragePercent,
                                                                riskyUsers,
                                                                failedSignIns,
                                                                usersWithoutMfa,
                                                                appsSecretsExpiringSoon,
                                                                unusedApplications,
                                                                highPrivilegeApplications,
                                                                nonCompliantDevices,
                                                                microsoft365OpenTickets,
                                                                outdatedWindowsDevices,
                                                                devicesWithoutEncryption,
                                                                staleDevices),
                                                kpiProperties);

                Microsoft365HealthStatusDto details = new Microsoft365HealthStatusDto();

                details.setPercentage(result.percentage());
                details.setColor(result.color());
                details.setIndicators(result.indicators());
                details.setReasons(result.reasons());
                details.setAffectedService(result.affectedService());
                details.setCriticalCondition(result.criticalCondition());
                details.setTechnicalDegradationValue(result.percentage());
                details.setTransversalReady(true);

                return details;
        }

        private Microsoft365IndicatorStatusDto indicator(
                        String name,
                        String color,
                        String reason) {

                Microsoft365IndicatorStatusDto indicator = new Microsoft365IndicatorStatusDto();

                indicator.setName(name);
                indicator.setColor(color);
                indicator.setAffectionPercent(affectionPercent(color));
                indicator.setReason(reason);

                return indicator;
        }

        private int affectionPercent(
                        String color) {

                if (RED.equals(color)) {

                        return kpiProperties.getAffection().getRed();
                }

                if (YELLOW.equals(color)) {

                        return kpiProperties.getAffection().getYellow();
                }

                return kpiProperties.getAffection().getGreen();
        }

        private Microsoft365HealthStatusDto noDataHealthDetails() {

                Microsoft365IndicatorStatusDto noData = indicator(
                                "Datos Microsoft 365",
                                RED,
                                "No hay snapshot Microsoft 365 disponible");

                Microsoft365HealthStatusDto details = new Microsoft365HealthStatusDto();

                details.setPercentage(kpiProperties.getAffection().getRed());
                details.setColor(RED);
                details.setIndicators(List.of(noData));
                details.setReasons(List.of(noData.getReason()));
                details.setAffectedService(true);
                details.setCriticalCondition(true);
                details.setTechnicalDegradationValue(kpiProperties.getAffection().getRed());
                details.setTransversalReady(true);

                return details;
        }

        private KpiResultDto buildMicrosoft365HealthKpi(
                        Microsoft365HealthStatusDto details,
                        LocalDateTime timestamp,
                        String freshness) {

                return new KpiResultDto(
                                "microsoft365_health",
                                "Índice de salud Microsoft 365",
                                details.getPercentage(),
                                KpiStatus.from(details.getColor()),
                                "Afección normalizada de Microsoft 365.",
                                "Suma de afecciones parciales de servicios, licencias, seguridad, dispositivos, SharePoint y tickets Microsoft 365, limitada a 100.",
                                timestamp,
                                freshness,
                                details.getPercentage(),
                                details.getIndicators().stream()
                                                .map(indicator -> new KpiResultDto(
                                                                indicatorId(indicator.getName()),
                                                                indicator.getName(),
                                                                indicator.getAffectionPercent(),
                                                                KpiStatus.from(indicator.getColor()),
                                                                indicator.getReason(),
                                                                null,
                                                                timestamp,
                                                                freshness,
                                                                indicator.getAffectionPercent(),
                                                                List.of()))
                                                .toList());
        }

        private String indicatorId(String name) {

                return name.toLowerCase()
                                .replace(" ", "_")
                                .replace("%", "percent");
        }
}

