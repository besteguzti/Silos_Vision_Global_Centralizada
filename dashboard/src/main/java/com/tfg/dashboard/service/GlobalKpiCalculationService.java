package com.tfg.dashboard.service;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;

/**
 * Centraliza el cálculo numérico de los KPIs transversales del dashboard.
 *
 * Recibe datos ya normalizados o snapshots de cada plataforma y los convierte
 * a una escala común de afección 0-100. Los pesos y umbrales se leen desde
 * {@link KpiProperties}, evitando duplicar reglas en el frontend.
 */
@Service
public class GlobalKpiCalculationService {

        private final KpiScoringService kpiScoringService;
        private final KpiProperties kpiProperties;

        public GlobalKpiCalculationService(KpiScoringService kpiScoringService,KpiProperties kpiProperties) {

                this.kpiScoringService = kpiScoringService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Suma elementos concretos que requieren revisión operativa.
         *
         * Este valor no es un porcentaje: es un contador agregado de firmware,
         * dispositivos, tickets y controladores no disponibles.
         */
        public int calculateItemsRequiringAction(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int unavailableDeliveryControllers = Math.max(
                                0,
                                citrix.getTotalDeliveryControllers() - citrix.getAvailableDeliveryControllers());

                return aruba.getFirmwareOutdated()
                                + aruba.getSwitchesFirmwareUpgradeRequired()
                                + microsoft365.getAppsSecretsExpiringSoon()
                                + microsoft365.getNonCompliantDevices()
                                + microsoft365.getStaleDevices()
                                + glpi.getSlaBreachedTickets()
                                + unavailableDeliveryControllers;
        }

        /**
         * Obtiene la afección de red Aruba. Si existe el DTO detallado, se usa
         * su porcentaje calculado por ArubaNetworkStatusService; si no existe,
         * se aplica una estimación conservadora basada en campos históricos.
         */
        public int calculateArubaNetworkAffection(ArubaSummary aruba) {

                ArubaNetworkStatusDto details = aruba.getNetworkStatusDetails();

                if (details != null) {

                        return clamp(details.getPercentage());
                }

                ArubaAffectationCalculator.Result affectation = ArubaAffectationCalculator.calculate(
                                new ArubaAffectationCalculator.Input(
                                                aruba.getTotalAps(),
                                                aruba.getDownAps(),
                                                aruba.getInactiveAps(),
                                                aruba.getFirmwareOutdated(),
                                                aruba.getTotalWifiClients(),
                                                aruba.getMutualiaApsClients(),
                                                aruba.getMutualiaWifiClients(),
                                                aruba.getTotalSwitches(),
                                                aruba.getDownSwitches(),
                                                aruba.getSwitchesFirmwareUpgradeRequired(),
                                                aruba.getUnderusedSwitches() == null ? 0 : aruba.getUnderusedSwitches().size(),
                                                aruba.getArubaOpenTickets()),
                                kpiProperties);

                return clamp(affectation.totalAffection());
        }

        /**
         * Normaliza los indicadores Citrix en una afección común 0-100.
         */
        public int calculateCitrixHealthAffection(CitrixMetricsHistory citrix) {

                CitrixAffectationCalculator.Result result = calculateCitrixHealthResult(citrix);

                return result.percentage();
        }

        /**
         * Devuelve la severidad final de Citrix separada del porcentaje.
         *
         * Una metrica interna en rojo puede marcar Citrix como critico aunque
         * la suma ponderada no llegue a 100 % de afeccion.
         */
        public String calculateCitrixHealthStatus(CitrixMetricsHistory citrix) {

                return calculateCitrixHealthResult(citrix).color();
        }

        private CitrixAffectationCalculator.Result calculateCitrixHealthResult(CitrixMetricsHistory citrix) {

                return CitrixAffectationCalculator.calculate(
                                new CitrixAffectationCalculator.Input(
                                                citrix.getActiveSessions(),
                                                citrix.getActiveLicenses(),
                                                citrix.getAvailableDeliveryControllers(),
                                                citrix.getTotalDeliveryControllers(),
                                                citrix.getDisconnectedSessions(),
                                                citrix.getAverageLogonDurationSeconds(),
                                                citrix.getServerLoadPercent(),
                                                citrix.getFailedLogons(),
                                                0),
                                kpiProperties);
        }

        /**
         * Normaliza los indicadores Microsoft 365 en una afección común 0-100.
         */
        public int calculateMicrosoft365HealthAffection(Microsoft365MetricsHistory microsoft365) {

                return calculateMicrosoft365HealthAffection(microsoft365, 0);
        }

        public int calculateMicrosoft365HealthAffection(
                        Microsoft365MetricsHistory microsoft365,
                        int microsoft365OpenTickets) {

                Microsoft365AffectationCalculator.Result result =
                                Microsoft365AffectationCalculator.calculate(
                                                microsoft365Input(microsoft365, microsoft365OpenTickets),
                                                kpiProperties);

                return result.percentage();
        }

        /**
         * Normaliza GLPI como presión/afección operativa a partir de tickets y
         * capacidad de cierre.
         */
        public int calculateGlpiHealthAffection(GlpiMetricsHistory glpi) {

                int openTickets = openTicketsIndicator(glpi);
                int criticalTickets = criticalTicketsIndicator(glpi);
                int closedToday = closedPercentageIndicator(glpi.getCreatedToday(),glpi.getClosedToday());
                int closedThisWeek = closedPercentageIndicator(glpi.getCreatedThisWeek(),glpi.getClosedThisWeek());

                int calculated = average(
                                openTickets,
                                criticalTickets,
                                slaBreachedTicketsIndicator(glpi),
                                closedToday,
                                closedThisWeek);

                return PlatformSeverityRules.applyInternalSeverityFloorFromScores(
                                calculated,
                                kpiProperties,
                                openTickets,
                                criticalTickets,
                                slaBreachedTicketsIndicator(glpi),
                                closedToday,
                                closedThisWeek);
        }

        /**
         * Calcula la criticidad global como media de condiciones críticas o de
         * advertencia detectadas en las plataformas.
         */
        public int calculateGlobalCriticality(ArubaSummary aruba,CitrixMetricsHistory citrix,Microsoft365MetricsHistory microsoft365,GlpiMetricsHistory glpi) {

                return average(
                                allApsDownIndicator(aruba),
                                allSwitchesDownIndicator(aruba),
                                noClientsIndicator(aruba.getTotalWifiClients()),
                                noClientsIndicator(aruba.getMutualiaApsClients()),
                                noClientsIndicator(aruba.getMutualiaWifiClients()),
                                activeSessionsIndicator(citrix),
                                deliveryControllersIndicator(citrix),
                                logonDurationIndicator(citrix),
                                serverLoadIndicator(citrix),
                                failedLogonsIndicator(citrix),
                                sharePointStorageIndicator(microsoft365),
                                usersWithoutMfaIndicator(microsoft365),
                                nonCompliantDevicesIndicator(microsoft365),
                                devicesWithoutEncryptionIndicator(microsoft365),
                                openTicketsIndicator(glpi),
                                criticalTicketsIndicator(glpi));
        }

        /**
         * Calcula la afección sobre disponibilidad aplicando los pesos definidos
         * para Aruba, Citrix, Microsoft 365 y GLPI.
         */
        public int calculateGlobalAvailability(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int arubaAvailability = average(
                                apAvailabilityIndicator(aruba),
                                switchAvailabilityIndicator(aruba),
                                noClientsIndicator(aruba.getTotalWifiClients()));

                int citrixAvailability = average(
                                activeSessionsIndicator(citrix),
                                deliveryControllersIndicator(citrix));

                int microsoftAvailability = average(
                                sharePointStorageIndicator(microsoft365),
                                secretsIndicator(microsoft365));

                int glpiAvailability = calculateGlpiHealthAffection(glpi);

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getAvailability();

                return weightedAverage(
                                arubaAvailability, weight(weights.getAruba()),
                                citrixAvailability, weight(weights.getCitrix()),
                                microsoftAvailability, weight(weights.getMicrosoft365()),
                                glpiAvailability, weight(weights.getGlpi()));
        }

        /**
         * Estima la presión operativa combinando carga GLPI con señales técnicas
         * que suelen generar trabajo para el equipo IT.
         */
        public int calculateOperationalPressure(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int glpiPressure = calculateGlpiHealthAffection(glpi);

                int citrixPressure = average(failedLogonsIndicator(citrix),serverLoadIndicator(citrix));

                int microsoftPressure = average(
                                nonCompliantDevicesIndicator(microsoft365),
                                outdatedWindowsIndicator(microsoft365),
                                devicesWithoutEncryptionIndicator(microsoft365));

                int arubaPressure = average(
                                countIndicator(aruba.getInactiveAps()),
                                countIndicator(aruba.getFirmwareOutdated()),
                                countIndicator(
                                                aruba.getSwitchesFirmwareUpgradeRequired()));

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getOperationalPressure();

                return weightedAverage(
                                glpiPressure, weight(weights.getGlpi()),
                                citrixPressure, weight(weights.getCitrix()),
                                microsoftPressure, weight(weights.getMicrosoft365()),
                                arubaPressure, weight(weights.getAruba()));
        }

        /**
         * Detecta deterioro técnico aunque no haya caída total del servicio.
         */
        public int calculateTechnicalDegradation(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int arubaDegradation = average(
                                countIndicator(aruba.getFirmwareOutdated()),
                                countIndicator(aruba.getInactiveAps()),
                                partialSwitchesDownIndicator(aruba));

                int citrixDegradation = average(
                                logonDurationIndicator(citrix),
                                serverLoadIndicator(citrix),
                                failedLogonsIndicator(citrix));

                int microsoftDegradation = average(
                                sharePointStorageIndicator(microsoft365),
                                secretsIndicator(microsoft365),
                                outdatedWindowsIndicator(microsoft365),
                                nonCompliantDevicesIndicator(microsoft365),
                                devicesWithoutEncryptionIndicator(microsoft365));

                int glpiDegradation = average(
                                openTicketsIndicator(glpi),
                                criticalTicketsIndicator(glpi));

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getTechnicalDegradation();

                return weightedAverage(
                                arubaDegradation, weight(weights.getAruba()),
                                citrixDegradation, weight(weights.getCitrix()),
                                microsoftDegradation, weight(weights.getMicrosoft365()),
                                glpiDegradation, weight(weights.getGlpi()));
        }

        /**
         * Estima riesgo de SLA ponderando Citrix, Aruba, GLPI y Microsoft 365.
         */
        public int calculateSlaRisk(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int citrixSlaRisk = average(
                                logonDurationIndicator(citrix),
                                activeSessionsIndicator(citrix),
                                deliveryControllersIndicator(citrix),
                                failedLogonsIndicator(citrix));

                int arubaSlaRisk = calculateArubaNetworkAffection(aruba);

                int glpiSlaRisk = average(
                                slaBreachedTicketsIndicator(glpi),
                                criticalTicketsIndicator(glpi),
                                closedPercentageIndicator(glpi.getCreatedToday(),glpi.getClosedToday()),
                                closedPercentageIndicator(glpi.getCreatedThisWeek(),glpi.getClosedThisWeek()));

                int microsoftSlaRisk = average(
                                sharePointStorageIndicator(microsoft365),
                                secretsIndicator(microsoft365),
                                nonCompliantDevicesIndicator(microsoft365));

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getSlaRisk();

                return weightedAverage(
                                citrixSlaRisk, weight(weights.getCitrix()),
                                arubaSlaRisk, weight(weights.getAruba()),
                                glpiSlaRisk, weight(weights.getGlpi()),
                                microsoftSlaRisk, weight(weights.getMicrosoft365()));
        }

        /**
         * Calcula trabajo pendiente acumulado dando más peso a GLPI, pero
         * incorporando señales pendientes de Microsoft 365, Aruba y Citrix.
         */
        public int calculateOperationalBacklog(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int glpiBacklog = calculateGlpiHealthAffection(glpi);

                int microsoftBacklog = average(
                                nonCompliantDevicesIndicator(microsoft365),
                                outdatedWindowsIndicator(microsoft365),
                                devicesWithoutEncryptionIndicator(microsoft365));

                int arubaBacklog = average(
                                countIndicator(aruba.getFirmwareOutdated()),
                                countIndicator(aruba.getSwitchesFirmwareUpgradeRequired()));

                int citrixBacklog = failedLogonsIndicator(citrix);

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getOperationalBacklog();

                return weightedAverage(
                                glpiBacklog, weight(weights.getGlpi()),
                                microsoftBacklog, weight(weights.getMicrosoft365()),
                                arubaBacklog, weight(weights.getAruba()),
                                citrixBacklog, weight(weights.getCitrix()));
        }

        /**
         * Estima afección percibida por usuarios a partir de señales de acceso,
         * conectividad, Microsoft 365 y presión GLPI.
         */
        public int calculateUserImpact(
                        ArubaSummary aruba,
                        CitrixMetricsHistory citrix,
                        Microsoft365MetricsHistory microsoft365,
                        GlpiMetricsHistory glpi) {

                int arubaImpact = average(
                                noClientsIndicator(aruba.getTotalWifiClients()),
                                noClientsIndicator(aruba.getMutualiaApsClients()),
                                noClientsIndicator(aruba.getMutualiaWifiClients()),
                                apAvailabilityIndicator(aruba),
                                switchAvailabilityIndicator(aruba));

                int citrixImpact = average(
                                activeSessionsIndicator(citrix),
                                logonDurationIndicator(citrix),
                                failedLogonsIndicator(citrix),
                                deliveryControllersIndicator(citrix));

                int microsoftImpact = average(
                                sharePointStorageIndicator(microsoft365),
                                usersWithoutMfaIndicator(microsoft365),
                                nonCompliantDevicesIndicator(microsoft365));

                int glpiImpact = average(
                                criticalTicketsIndicator(glpi),
                                openTicketsIndicator(glpi));

                KpiProperties.PlatformWeights weights = kpiProperties.getWeights().getUserImpact();

                return weightedAverage(
                                citrixImpact, weight(weights.getCitrix()),
                                arubaImpact, weight(weights.getAruba()),
                                microsoftImpact, weight(weights.getMicrosoft365()),
                                glpiImpact, weight(weights.getGlpi()));
        }

        /**
         * Convierte el número de plataformas afectadas en porcentaje sobre las
         * cuatro plataformas monitorizadas.
         */
        public int calculateAffectedServicesPercent(
                        int arubaHealthIndex,
                        int citrixHealthIndex,
                        int microsoft365HealthIndex,
                        int glpiHealthIndex) {

                return calculateAffectedPlatformCount(
                                arubaHealthIndex,
                                citrixHealthIndex,
                                microsoft365HealthIndex,
                                glpiHealthIndex) * (kpiProperties.getStatus().getMax() / 4);
        }

        /**
         * Cuenta plataformas con estado distinto de GREEN. Un estado nulo o
         * desconocido no se considera saludable.
         */
        public int calculateAffectedPlatformCount(
                        int arubaHealthIndex,
                        int citrixHealthIndex,
                        int microsoft365HealthIndex,
                        int glpiHealthIndex) {

                int affected = 0;

                if (!isGreen(kpiScoringService.statusFromAffection(arubaHealthIndex))) {

                        affected++;
                }

                if (!isGreen(kpiScoringService.statusFromAffection(citrixHealthIndex))) {

                        affected++;
                }

                if (!isGreen(kpiScoringService.statusFromAffection(
                                microsoft365HealthIndex))) {

                        affected++;
                }

                if (!isGreen(kpiScoringService.statusFromAffection(glpiHealthIndex))) {

                        affected++;
                }

                return affected;
        }

        /**
         * Calcula una media ponderada y acota el resultado a la escala común
         * 0-100.
         */
        public int weightedAverage(
                        int firstValue,
                        int firstWeight,
                        int secondValue,
                        int secondWeight,
                        int thirdValue,
                        int thirdWeight,
                        int fourthValue,
                        int fourthWeight) {

                int totalWeight = firstWeight + secondWeight + thirdWeight + fourthWeight;

                return clamp(
                                (firstValue * firstWeight
                                                + secondValue * secondWeight
                                                + thirdValue * thirdWeight
                                                + fourthValue * fourthWeight) / totalWeight);
        }

        private int apAvailabilityIndicator(ArubaSummary aruba) {

                if (aruba.getTotalAps() <= 0 || aruba.getDownAps() >= aruba.getTotalAps()) {

                        return redScore();
                }

                if (aruba.getDownAps() > 0) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int switchAvailabilityIndicator(ArubaSummary aruba) {

                if (aruba.getTotalSwitches() <= 0 || aruba.getDownSwitches() >= aruba.getTotalSwitches()) {

                        return redScore();
                }

                if (aruba.getDownSwitches() > 0) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int allApsDownIndicator(ArubaSummary aruba) {

                if (aruba.getTotalAps() <= 0) {

                        return redScore();
                }

                if (aruba.getDownAps() >= aruba.getTotalAps()) {

                        return redScore();
                }

                return aruba.getDownAps() > 0 ? yellowScore() : greenScore();
        }

        private int allSwitchesDownIndicator(ArubaSummary aruba) {

                if (aruba.getTotalSwitches() <= 0) {

                        return redScore();
                }

                if (aruba.getDownSwitches() >= aruba.getTotalSwitches()) {

                        return redScore();
                }

                return aruba.getDownSwitches() > 0 ? yellowScore() : greenScore();
        }

        private int partialSwitchesDownIndicator(ArubaSummary aruba) {

                if (aruba.getTotalSwitches() <= 0) {

                        return redScore();
                }

                if (aruba.getDownSwitches() >= aruba.getTotalSwitches()) {

                        return redScore();
                }

                return aruba.getDownSwitches() > 0 ? yellowScore() : greenScore();
        }

        private int noClientsIndicator(int clients) {

                return clients <= 0 ? redScore() : greenScore();
        }

        private int activeSessionsIndicator(CitrixMetricsHistory citrix) {

                return greenScore();
        }

        private int deliveryControllersIndicator(CitrixMetricsHistory citrix) {

                if (citrix.getTotalDeliveryControllers() <= 0 || citrix.getAvailableDeliveryControllers() <= 0) {

                        return redScore();
                }

                int availablePercent = citrix.getAvailableDeliveryControllers() * 100 / citrix.getTotalDeliveryControllers();

                if (availablePercent < kpiProperties.getStatus().getYellowMin()) {

                        return redScore();
                }

                return availablePercent < kpiProperties.getCitrix().getDeliveryControllerYellowBelowPercent()
                                ? yellowScore()
                                : greenScore();
        }

        private int logonDurationIndicator(CitrixMetricsHistory citrix) {

                if (citrix.getAverageLogonDurationSeconds() > kpiProperties.getCitrix().getLogonDurationRedAboveSeconds()) {

                        return redScore();
                }

                if (citrix.getAverageLogonDurationSeconds() > kpiProperties.getCitrix().getLogonDurationYellowAboveSeconds()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int serverLoadIndicator(CitrixMetricsHistory citrix) {

                if (citrix.getServerLoadPercent() >= kpiProperties.getCitrix().getServerLoadRedMin()) {

                        return redScore();
                }

                if (citrix.getServerLoadPercent() >= kpiProperties.getCitrix().getServerLoadYellowMin()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int failedLogonsIndicator(CitrixMetricsHistory citrix) {

                if (citrix.getFailedLogons() > kpiProperties.getCitrix().getFailedLogonsRedAbove()) {

                        return redScore();
                }

                if (citrix.getFailedLogons() > kpiProperties.getCitrix().getFailedLogonsYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private Microsoft365AffectationCalculator.Input microsoft365Input(
                        Microsoft365MetricsHistory microsoft365,
                        int microsoft365OpenTickets) {

                return new Microsoft365AffectationCalculator.Input(
                                microsoft365.getActiveUsers(),
                                microsoft365.getUnassignedLicenses(),
                                microsoft365.getOutlookStatus(),
                                microsoft365.getTeamsStatus(),
                                microsoft365.getSharePointStatus(),
                                microsoft365.getNearlyFullMailboxes(),
                                microsoft365.getEmailsQuarantined(),
                                microsoft365.getSharePointStoragePercent(),
                                microsoft365.getRiskyUsers(),
                                microsoft365.getFailedSignIns(),
                                microsoft365.getUsersWithoutMfa(),
                                microsoft365.getAppsSecretsExpiringSoon(),
                                microsoft365.getUnusedApplications(),
                                microsoft365.getHighPrivilegeApplications(),
                                microsoft365.getNonCompliantDevices(),
                                microsoft365OpenTickets,
                                microsoft365.getOutdatedWindowsDevices(),
                                microsoft365.getDevicesWithoutEncryption(),
                                microsoft365.getStaleDevices());
        }

        private int sharePointStorageIndicator(Microsoft365MetricsHistory microsoft365) {

                if (microsoft365.getSharePointStoragePercent() >= kpiProperties.getMicrosoft365().getSharePointRedAbove()) {

                        return redScore();
                }

                if (microsoft365.getSharePointStoragePercent() >= kpiProperties.getMicrosoft365().getSharePointYellowMin()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int usersWithoutMfaIndicator(Microsoft365MetricsHistory microsoft365) {

                if (microsoft365.getUsersWithoutMfa() > kpiProperties.getMicrosoft365().getUsersWithoutMfaRedAbove()) {

                        return redScore();
                }

                if (microsoft365.getUsersWithoutMfa() > kpiProperties.getMicrosoft365().getUsersWithoutMfaYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int secretsIndicator(Microsoft365MetricsHistory microsoft365) {

                return microsoft365.getAppsSecretsExpiringSoon() > kpiProperties.getMicrosoft365().getSecretsYellowAbove()
                                ? yellowScore()
                                : greenScore();
        }

        private int nonCompliantDevicesIndicator(Microsoft365MetricsHistory microsoft365) {

                if (microsoft365.getNonCompliantDevices() > kpiProperties.getMicrosoft365().getNonCompliantDevicesRedAbove()) {

                        return redScore();
                }

                if (microsoft365.getNonCompliantDevices() > kpiProperties.getMicrosoft365().getNonCompliantDevicesYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int outdatedWindowsIndicator(Microsoft365MetricsHistory microsoft365) {

                return microsoft365.getOutdatedWindowsDevices() > kpiProperties.getMicrosoft365().getOutdatedWindowsYellowAbove()
                                ? yellowScore()
                                : greenScore();
        }

        private int devicesWithoutEncryptionIndicator(Microsoft365MetricsHistory microsoft365) {

                if (microsoft365.getDevicesWithoutEncryption() > kpiProperties.getMicrosoft365().getDevicesWithoutEncryptionRedAbove()) {

                        return redScore();
                }

                if (microsoft365.getDevicesWithoutEncryption() > kpiProperties.getMicrosoft365().getDevicesWithoutEncryptionYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int openTicketsIndicator(GlpiMetricsHistory glpi) {

                if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsRedMin()) {

                        return redScore();
                }

                if (glpi.getOpenTickets() >= kpiProperties.getGlpi().getOpenTicketsYellowMin()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int criticalTicketsIndicator(GlpiMetricsHistory glpi) {

                if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsRedAbove()) {

                        return redScore();
                }

                if (glpi.getCriticalOpenTickets() > kpiProperties.getGlpi().getCriticalTicketsYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int slaBreachedTicketsIndicator(GlpiMetricsHistory glpi) {

                if (glpi.getSlaBreachedTickets() > kpiProperties.getGlpi().getSlaBreachedTicketsRedAbove()) {

                        return redScore();
                }

                if (glpi.getSlaBreachedTickets() > kpiProperties.getGlpi().getSlaBreachedTicketsYellowAbove()) {

                        return yellowScore();
                }

                return greenScore();
        }

        private int closedPercentageIndicator(int created,int closed) {

                if (created <= 0) {

                        return 0;
                }

                int closedPercent = closed * 100 / created;

                return closedPercent >= kpiProperties.getGlpi().getClosedPercentGreenMin()
                                ? greenScore()
                                : yellowScore();
        }

        private int countIndicator(int value) {

                return value > 0 ? yellowScore() : greenScore();
        }

        private int average(int... values) {

                if (values.length == 0) {

                        return 0;
                }

                int total = 0;

                for (int value : values) {

                        total += value;
                }

                return clamp(total / values.length);
        }

        private boolean isGreen(String status) {

                // Un estado null, vacio o desconocido no debe tratarse
                // como correcto porque podria ocultar falta de datos.

                return "GREEN".equalsIgnoreCase(status);
        }

        private int clamp(int value) {

                if (value < 0) {

                        return 0;
                }

                if (value > kpiProperties.getStatus().getMax()) {

                        return kpiProperties.getStatus().getMax();
                }

                return value;
        }

        private int weight(double configuredWeight) {

                return kpiProperties.asWeightPercent(configuredWeight);
        }

        private int greenScore() {

                return kpiProperties.getAffection().getGreen();
        }

        private int yellowScore() {

                return kpiProperties.getAffection().getYellow();
        }

        private int redScore() {

                return kpiProperties.getAffection().getRed();
        }
}

