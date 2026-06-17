package com.tfg.dashboard.dto.summary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.ArubaSwitchClientUsageDto;
import com.tfg.dashboard.dto.KpiResultDto;

//Respuesta de la vista Aruba.

public class ArubaSummary {

    private int totalAps;
    private int upAps;
    private int downAps;
    private int totalSites;
    private int totalSwarms;
    private int firmwareOutdated;
    private int apsWithoutPublicIp;
    private ArubaNetworkStatusDto networkStatusDetails;
    private KpiResultDto networkStatusKpi;
    private Map<String, String> kpiStatuses;
    private int inactiveAps;
    private int totalSwitches;
    private int downSwitches;
    private int switchesFirmwareUpgradeRequired;
    private List<ArubaSwitchClientUsageDto> underusedSwitches;
    private int totalWifiClients;
    private int arubaOpenTickets;
    private int mutualiaApsClients;
    private int mutualiaWifiClients;
    private int mutualiaLangileakClients;
    private int mutualiaClients;
    private int mutualiaRedInternaClients;
    private int mutualiaRedExternaClients;
    private int mutualiaKorporatiboaClients;
    private int wifiPacsClients;
    private int mutVideoClients;
    private LocalDateTime lastUpdated;
    private String dataStatus;

    public int getTotalAps() {
        return totalAps;
    }

    public int getUpAps() {
        return upAps;
    }

    public int getDownAps() {
        return downAps;
    }

    public int getTotalSites() {
        return totalSites;
    }

    public int getTotalSwarms() {
        return totalSwarms;
    }

    public int getFirmwareOutdated() {
        return firmwareOutdated;
    }

    public int getApsWithoutPublicIp() {
        return apsWithoutPublicIp;
    }

    public ArubaNetworkStatusDto getNetworkStatusDetails() {
        return networkStatusDetails;
    }

    public KpiResultDto getNetworkStatusKpi() {
        return networkStatusKpi;
    }

    public Map<String, String> getKpiStatuses() {
        return kpiStatuses;
    }

    public int getInactiveAps() {
        return inactiveAps;
    }

    public int getTotalSwitches() {
        return totalSwitches;
    }

    public int getDownSwitches() {
        return downSwitches;
    }

    public int getSwitchesFirmwareUpgradeRequired() {
        return switchesFirmwareUpgradeRequired;
    }

    public List<ArubaSwitchClientUsageDto> getUnderusedSwitches() {
        return underusedSwitches;
    }

    public int getTotalWifiClients() {
        return totalWifiClients;
    }

    public int getArubaOpenTickets() {
        return arubaOpenTickets;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getDataStatus() {
        return dataStatus;
    }

    public void setTotalAps(int totalAps) {
        this.totalAps = totalAps;
    }

    public void setUpAps(int upAps) {
        this.upAps = upAps;
    }

    public void setDownAps(int downAps) {
        this.downAps = downAps;
    }

    public void setTotalSites(int totalSites) {
        this.totalSites = totalSites;
    }

    public void setTotalSwarms(int totalSwarms) {
        this.totalSwarms = totalSwarms;
    }

    public void setFirmwareOutdated(int firmwareOutdated) {
        this.firmwareOutdated = firmwareOutdated;
    }

    public void setApsWithoutPublicIp(int apsWithoutPublicIp) {
        this.apsWithoutPublicIp = apsWithoutPublicIp;
    }

    public void setNetworkStatusDetails(
            ArubaNetworkStatusDto networkStatusDetails) {
        this.networkStatusDetails = networkStatusDetails;
    }

    public void setNetworkStatusKpi(KpiResultDto networkStatusKpi) {
        this.networkStatusKpi = networkStatusKpi;
    }

    public void setKpiStatuses(Map<String, String> kpiStatuses) {
        this.kpiStatuses = kpiStatuses;
    }

    public void setInactiveAps(int inactiveAps) {
        this.inactiveAps = inactiveAps;
    }

    public void setTotalSwitches(int totalSwitches) {
        this.totalSwitches = totalSwitches;
    }

    public void setDownSwitches(int downSwitches) {
        this.downSwitches = downSwitches;
    }

    public void setSwitchesFirmwareUpgradeRequired(int switchesFirmwareUpgradeRequired) {
        this.switchesFirmwareUpgradeRequired = switchesFirmwareUpgradeRequired;
    }

    public void setUnderusedSwitches(
            List<ArubaSwitchClientUsageDto> underusedSwitches) {
        this.underusedSwitches = underusedSwitches;
    }

    public void setTotalWifiClients(int totalWifiClients) {
        this.totalWifiClients = totalWifiClients;
    }

    public void setArubaOpenTickets(int arubaOpenTickets) {
        this.arubaOpenTickets = arubaOpenTickets;
    }

    public void setMutualiaApsClients(int mutualiaApsClients) {
        this.mutualiaApsClients = mutualiaApsClients;
    }

    public void setMutualiaWifiClients(int mutualiaWifiClients) {
        this.mutualiaWifiClients = mutualiaWifiClients;
    }

    public void setMutualiaLangileakClients(int mutualiaLangileakClients) {
        this.mutualiaLangileakClients = mutualiaLangileakClients;
    }

    public void setMutualiaClients(int mutualiaClients) {
        this.mutualiaClients = mutualiaClients;
    }

    public void setMutualiaRedInternaClients(int mutualiaRedInternaClients) {
        this.mutualiaRedInternaClients = mutualiaRedInternaClients;
    }

    public void setMutualiaRedExternaClients(int mutualiaRedExternaClients) {
        this.mutualiaRedExternaClients = mutualiaRedExternaClients;
    }

    public void setMutualiaKorporatiboaClients(int mutualiaKorporatiboaClients) {
        this.mutualiaKorporatiboaClients = mutualiaKorporatiboaClients;
    }

    public void setWifiPacsClients(int wifiPacsClients) {
        this.wifiPacsClients = wifiPacsClients;
    }

    public void setMutVideoClients(int mutVideoClients) {
        this.mutVideoClients = mutVideoClients;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setDataStatus(String dataStatus) {
        this.dataStatus = dataStatus;
    }
}

