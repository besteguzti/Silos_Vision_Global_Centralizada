package com.tfg.dashboard.dto.summary;

import java.time.LocalDateTime;

import com.tfg.dashboard.dto.CitrixHealthStatusDto;
import com.tfg.dashboard.dto.KpiResultDto;

// Respuesta de la vista Citrix.
public class CitrixSummary {

    private int activeSessions;
    private int activeLicenses;
    private int availableDeliveryControllers;
    private int totalDeliveryControllers;
    private int disconnectedSessions;
    private int averageLogonDurationSeconds;
    private int serverLoadPercent;
    private int failedLogons;
    private int citrixOpenTickets;
    private CitrixHealthStatusDto citrixHealthDetails;
    private KpiResultDto citrixHealthKpi;
    private LocalDateTime lastUpdated;
    private String dataStatus;

    public CitrixSummary() {
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveLicenses() {
        return activeLicenses;
    }

    public void setActiveLicenses(int activeLicenses) {
        this.activeLicenses = activeLicenses;
    }

    public int getAvailableDeliveryControllers() {
        return availableDeliveryControllers;
    }

    public void setAvailableDeliveryControllers(int availableDeliveryControllers) {
        this.availableDeliveryControllers = availableDeliveryControllers;
    }

    public int getTotalDeliveryControllers() {
        return totalDeliveryControllers;
    }

    public void setTotalDeliveryControllers(int totalDeliveryControllers) {
        this.totalDeliveryControllers = totalDeliveryControllers;
    }

    public int getDisconnectedSessions() {
        return disconnectedSessions;
    }

    public void setDisconnectedSessions(int disconnectedSessions) {
        this.disconnectedSessions = disconnectedSessions;
    }

    public int getAverageLogonDurationSeconds() {
        return averageLogonDurationSeconds;
    }

    public void setAverageLogonDurationSeconds(int averageLogonDurationSeconds) {
        this.averageLogonDurationSeconds = averageLogonDurationSeconds;
    }

    public int getServerLoadPercent() {
        return serverLoadPercent;
    }

    public void setServerLoadPercent(int serverLoadPercent) {
        this.serverLoadPercent = serverLoadPercent;
    }

    public int getFailedLogons() {
        return failedLogons;
    }

    public void setFailedLogons(int failedLogons) {
        this.failedLogons = failedLogons;
    }

    public int getCitrixOpenTickets() {
        return citrixOpenTickets;
    }

    public void setCitrixOpenTickets(int citrixOpenTickets) {
        this.citrixOpenTickets = citrixOpenTickets;
    }

    public CitrixHealthStatusDto getCitrixHealthDetails() {
        return citrixHealthDetails;
    }

    public KpiResultDto getCitrixHealthKpi() {
        return citrixHealthKpi;
    }

    public void setCitrixHealthDetails(CitrixHealthStatusDto citrixHealthDetails) {
        this.citrixHealthDetails = citrixHealthDetails;
    }

    public void setCitrixHealthKpi(KpiResultDto citrixHealthKpi) {
        this.citrixHealthKpi = citrixHealthKpi;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDataStatus() {
        return dataStatus;
    }

    public void setDataStatus(String dataStatus) {
        this.dataStatus = dataStatus;
    }
}
