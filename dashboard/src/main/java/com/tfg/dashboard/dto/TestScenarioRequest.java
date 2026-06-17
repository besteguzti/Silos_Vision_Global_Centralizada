package com.tfg.dashboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Datos enviados desde el banco de pruebas para evaluar un escenario manual.
 */
public class TestScenarioRequest {

    @Valid
    @NotNull
    private ArubaData aruba;

    @Valid
    @NotNull
    private CitrixData citrix;

    @Valid
    @NotNull
    private Microsoft365Data microsoft365;

    @Valid
    @NotNull
    private GlpiData glpi;

    public TestScenarioRequest() {
    }

    public ArubaData getAruba() {
        return aruba;
    }

    public void setAruba(ArubaData aruba) {
        this.aruba = aruba;
    }

    public CitrixData getCitrix() {
        return citrix;
    }

    public void setCitrix(CitrixData citrix) {
        this.citrix = citrix;
    }

    public Microsoft365Data getMicrosoft365() {
        return microsoft365;
    }

    public void setMicrosoft365(Microsoft365Data microsoft365) {
        this.microsoft365 = microsoft365;
    }

    public GlpiData getGlpi() {
        return glpi;
    }

    public void setGlpi(GlpiData glpi) {
        this.glpi = glpi;
    }

    public static class ArubaData {

        private Integer totalAps;

        private Integer downAps;

        private Integer inactiveAps;

        private Integer firmwareOutdated;

        private Integer totalSwitches;

        private Integer downSwitches;

        private Integer switchesFirmwareUpgradeRequired;

        private Integer totalWifiClients;

        private Integer mutualiaWifiClients;

        private Integer mutualiaApsClients;

        private Integer arubaOpenTickets;

        public Integer getTotalAps() {
            return totalAps;
        }

        public void setTotalAps(Integer totalAps) {
            this.totalAps = totalAps;
        }

        public Integer getDownAps() {
            return downAps;
        }

        public void setDownAps(Integer downAps) {
            this.downAps = downAps;
        }

        public Integer getInactiveAps() {
            return inactiveAps;
        }

        public void setInactiveAps(Integer inactiveAps) {
            this.inactiveAps = inactiveAps;
        }

        public Integer getFirmwareOutdated() {
            return firmwareOutdated;
        }

        public void setFirmwareOutdated(Integer firmwareOutdated) {
            this.firmwareOutdated = firmwareOutdated;
        }

        public Integer getTotalSwitches() {
            return totalSwitches;
        }

        public void setTotalSwitches(Integer totalSwitches) {
            this.totalSwitches = totalSwitches;
        }

        public Integer getDownSwitches() {
            return downSwitches;
        }

        public void setDownSwitches(Integer downSwitches) {
            this.downSwitches = downSwitches;
        }

        public Integer getSwitchesFirmwareUpgradeRequired() {
            return switchesFirmwareUpgradeRequired;
        }

        public void setSwitchesFirmwareUpgradeRequired(Integer switchesFirmwareUpgradeRequired) {
            this.switchesFirmwareUpgradeRequired = switchesFirmwareUpgradeRequired;
        }

        public Integer getTotalWifiClients() {
            return totalWifiClients;
        }

        public void setTotalWifiClients(Integer totalWifiClients) {
            this.totalWifiClients = totalWifiClients;
        }

        public Integer getMutualiaWifiClients() {
            return mutualiaWifiClients;
        }

        public void setMutualiaWifiClients(Integer mutualiaWifiClients) {
            this.mutualiaWifiClients = mutualiaWifiClients;
        }

        public Integer getMutualiaApsClients() {
            return mutualiaApsClients;
        }

        public void setMutualiaApsClients(Integer mutualiaApsClients) {
            this.mutualiaApsClients = mutualiaApsClients;
        }

        public Integer getArubaOpenTickets() {
            return arubaOpenTickets;
        }

        public void setArubaOpenTickets(Integer arubaOpenTickets) {
            this.arubaOpenTickets = arubaOpenTickets;
        }
    }

    public static class CitrixData {

        private Integer activeSessions;

        private Integer activeLicenses;

        private Integer disconnectedSessions;

        private Integer totalDeliveryControllers;

        private Integer availableDeliveryControllers;

        private Integer averageLogonDurationSeconds;

        private Integer serverLoadPercent;

        private Integer failedLogons;

        private Integer citrixOpenTickets;

        public Integer getActiveSessions() {
            return activeSessions;
        }

        public void setActiveSessions(Integer activeSessions) {
            this.activeSessions = activeSessions;
        }

        public Integer getActiveLicenses() {
            return activeLicenses;
        }

        public void setActiveLicenses(Integer activeLicenses) {
            this.activeLicenses = activeLicenses;
        }

        public Integer getDisconnectedSessions() {
            return disconnectedSessions;
        }

        public void setDisconnectedSessions(Integer disconnectedSessions) {
            this.disconnectedSessions = disconnectedSessions;
        }

        public Integer getTotalDeliveryControllers() {
            return totalDeliveryControllers;
        }

        public void setTotalDeliveryControllers(Integer totalDeliveryControllers) {
            this.totalDeliveryControllers = totalDeliveryControllers;
        }

        public Integer getAvailableDeliveryControllers() {
            return availableDeliveryControllers;
        }

        public void setAvailableDeliveryControllers(Integer availableDeliveryControllers) {
            this.availableDeliveryControllers = availableDeliveryControllers;
        }

        public Integer getAverageLogonDurationSeconds() {
            return averageLogonDurationSeconds;
        }

        public void setAverageLogonDurationSeconds(Integer averageLogonDurationSeconds) {
            this.averageLogonDurationSeconds = averageLogonDurationSeconds;
        }

        public Integer getServerLoadPercent() {
            return serverLoadPercent;
        }

        public void setServerLoadPercent(Integer serverLoadPercent) {
            this.serverLoadPercent = serverLoadPercent;
        }

        public Integer getFailedLogons() {
            return failedLogons;
        }

        public void setFailedLogons(Integer failedLogons) {
            this.failedLogons = failedLogons;
        }

        public Integer getCitrixOpenTickets() {
            return citrixOpenTickets;
        }

        public void setCitrixOpenTickets(Integer citrixOpenTickets) {
            this.citrixOpenTickets = citrixOpenTickets;
        }
    }

    public static class Microsoft365Data {

        private Integer sharePointStoragePercent;

        private Integer activeUsers;

        private Integer usersWithoutMfa;

        private Integer appsSecretsExpiringSoon;

        private Integer nonCompliantDevices;

        private Integer outdatedWindowsDevices;

        private Integer devicesWithoutEncryption;

        private Integer microsoft365OpenTickets;

        public Integer getSharePointStoragePercent() {
            return sharePointStoragePercent;
        }

        public void setSharePointStoragePercent(Integer sharePointStoragePercent) {
            this.sharePointStoragePercent = sharePointStoragePercent;
        }

        public Integer getActiveUsers() {
            return activeUsers;
        }

        public void setActiveUsers(Integer activeUsers) {
            this.activeUsers = activeUsers;
        }

        public Integer getUsersWithoutMfa() {
            return usersWithoutMfa;
        }

        public void setUsersWithoutMfa(Integer usersWithoutMfa) {
            this.usersWithoutMfa = usersWithoutMfa;
        }

        public Integer getAppsSecretsExpiringSoon() {
            return appsSecretsExpiringSoon;
        }

        public void setAppsSecretsExpiringSoon(Integer appsSecretsExpiringSoon) {
            this.appsSecretsExpiringSoon = appsSecretsExpiringSoon;
        }

        public Integer getNonCompliantDevices() {
            return nonCompliantDevices;
        }

        public void setNonCompliantDevices(Integer nonCompliantDevices) {
            this.nonCompliantDevices = nonCompliantDevices;
        }

        public Integer getOutdatedWindowsDevices() {
            return outdatedWindowsDevices;
        }

        public void setOutdatedWindowsDevices(Integer outdatedWindowsDevices) {
            this.outdatedWindowsDevices = outdatedWindowsDevices;
        }

        public Integer getDevicesWithoutEncryption() {
            return devicesWithoutEncryption;
        }

        public void setDevicesWithoutEncryption(Integer devicesWithoutEncryption) {
            this.devicesWithoutEncryption = devicesWithoutEncryption;
        }

        public Integer getMicrosoft365OpenTickets() {
            return microsoft365OpenTickets;
        }

        public void setMicrosoft365OpenTickets(Integer microsoft365OpenTickets) {
            this.microsoft365OpenTickets = microsoft365OpenTickets;
        }
    }

    public static class GlpiData {

        private Integer criticalOpenTickets;

        private Integer dailyClosurePercent;

        private Integer weeklyClosurePercent;

        public Integer getCriticalOpenTickets() {
            return criticalOpenTickets;
        }

        public void setCriticalOpenTickets(Integer criticalOpenTickets) {
            this.criticalOpenTickets = criticalOpenTickets;
        }

        public Integer getDailyClosurePercent() {
            return dailyClosurePercent;
        }

        public void setDailyClosurePercent(Integer dailyClosurePercent) {
            this.dailyClosurePercent = dailyClosurePercent;
        }

        public Integer getWeeklyClosurePercent() {
            return weeklyClosurePercent;
        }

        public void setWeeklyClosurePercent(Integer weeklyClosurePercent) {
            this.weeklyClosurePercent = weeklyClosurePercent;
        }
    }
}
