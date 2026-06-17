package com.tfg.dashboard.dto.summary;

import java.time.LocalDateTime;

import com.tfg.dashboard.dto.GlpiHealthStatusDto;
import com.tfg.dashboard.dto.KpiResultDto;

//Respuesta de la vista GLPI.

public class GlpiSummary {

    private int openTickets;
    private int arubaOpenTickets;
    private int citrixOpenTickets;
    private int microsoft365OpenTickets;
    private int criticalOpenTickets;
    private int slaBreachedTickets;
    private int averageResolutionHours;
    private int createdToday;
    private int closedToday;
    private int createdThisWeek;
    private int closedThisWeek;
    private int operationalBacklog;

    private GlpiHealthStatusDto glpiHealthDetails;
    private KpiResultDto glpiHealthKpi;
    private LocalDateTime lastUpdated;
    private String dataStatus;

    public GlpiSummary() {
    }

    public int getOpenTickets() {
        return openTickets;
    }

    public void setOpenTickets(int openTickets) {
        this.openTickets = openTickets;
    }

    public int getArubaOpenTickets() {
        return arubaOpenTickets;
    }

    public void setArubaOpenTickets(int arubaOpenTickets) {
        this.arubaOpenTickets = arubaOpenTickets;
    }

    public int getCitrixOpenTickets() {
        return citrixOpenTickets;
    }

    public void setCitrixOpenTickets(int citrixOpenTickets) {
        this.citrixOpenTickets = citrixOpenTickets;
    }

    public int getMicrosoft365OpenTickets() {
        return microsoft365OpenTickets;
    }

    public void setMicrosoft365OpenTickets(int microsoft365OpenTickets) {
        this.microsoft365OpenTickets = microsoft365OpenTickets;
    }

    public int getCriticalOpenTickets() {
        return criticalOpenTickets;
    }

    public void setCriticalOpenTickets(int criticalOpenTickets) {
        this.criticalOpenTickets = criticalOpenTickets;
    }

    public int getSlaBreachedTickets() {
        return slaBreachedTickets;
    }

    public void setSlaBreachedTickets(int slaBreachedTickets) {
        this.slaBreachedTickets = slaBreachedTickets;
    }

    public int getAverageResolutionHours() {
        return averageResolutionHours;
    }

    public void setAverageResolutionHours(int averageResolutionHours) {
        this.averageResolutionHours = averageResolutionHours;
    }

    public int getCreatedToday() {
        return createdToday;
    }

    public void setCreatedToday(int createdToday) {
        this.createdToday = createdToday;
    }

    public int getClosedToday() {
        return closedToday;
    }

    public void setClosedToday(int closedToday) {
        this.closedToday = closedToday;
    }

    public int getCreatedThisWeek() {
        return createdThisWeek;
    }

    public void setCreatedThisWeek(int createdThisWeek) {
        this.createdThisWeek = createdThisWeek;
    }

    public int getClosedThisWeek() {
        return closedThisWeek;
    }

    public void setClosedThisWeek(int closedThisWeek) {
        this.closedThisWeek = closedThisWeek;
    }

    public int getOperationalBacklog() {
        return operationalBacklog;
    }

    public GlpiHealthStatusDto getGlpiHealthDetails() {
        return glpiHealthDetails;
    }

    public KpiResultDto getGlpiHealthKpi() {
        return glpiHealthKpi;
    }

    public void setOperationalBacklog(int operationalBacklog) {
        this.operationalBacklog = operationalBacklog;
    }

    public void setGlpiHealthDetails(GlpiHealthStatusDto glpiHealthDetails) {
        this.glpiHealthDetails = glpiHealthDetails;
    }

    public void setGlpiHealthKpi(KpiResultDto glpiHealthKpi) {
        this.glpiHealthKpi = glpiHealthKpi;
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
