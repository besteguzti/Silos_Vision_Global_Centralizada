package com.tfg.dashboard.dto.summary;

import java.time.LocalDateTime;
import java.util.List;

import com.tfg.dashboard.dto.KpiResultDto;

// Respuesta agregada del dashboard principal.
 
public class MainDashboardSummary {

    private String globalHealth;
    private int globalHealthPercentage;
    private String globalHealthStatus;
    private int globalCriticality;
    private String globalCriticalityStatus;
    private int globalAvailability;
    private String globalAvailabilityStatus;
    private int userImpact;
    private String userImpactStatus;
    private int affectedServicesPercent;
    private String affectedServicesStatus;
    private int technicalDegradation;
    private String technicalDegradationStatus;
    private int operationalPressure;
    private String operationalPressureStatus;
    private int operationalBacklog;
    private String operationalBacklogStatus;
    private int slaRisk;
    private String slaRiskStatus;
    private List<KpiResultDto> kpis;
    private LocalDateTime lastUpdated;
    private String dataStatus;
    private String arubaDataStatus;
    private String citrixDataStatus;
    private String microsoft365DataStatus;
    private String glpiDataStatus;

    public MainDashboardSummary() {
    }

    public String getGlobalHealth() {
        return globalHealth;
    }

    public int getGlobalHealthPercentage() {
        return globalHealthPercentage;
    }

    public String getGlobalHealthStatus() {
        return globalHealthStatus;
    }

    public int getGlobalCriticality() {
        return globalCriticality;
    }

    public String getGlobalCriticalityStatus() {
        return globalCriticalityStatus;
    }

    public int getGlobalAvailability() {
        return globalAvailability;
    }

    public String getGlobalAvailabilityStatus() {
        return globalAvailabilityStatus;
    }

    public int getUserImpact() {
        return userImpact;
    }

    public String getUserImpactStatus() {
        return userImpactStatus;
    }

    public int getAffectedServicesPercent() {
        return affectedServicesPercent;
    }

    public String getAffectedServicesStatus() {
        return affectedServicesStatus;
    }

    public int getTechnicalDegradation() {
        return technicalDegradation;
    }

    public String getTechnicalDegradationStatus() {
        return technicalDegradationStatus;
    }

    public int getOperationalPressure() {
        return operationalPressure;
    }

    public String getOperationalPressureStatus() {
        return operationalPressureStatus;
    }

    public int getOperationalBacklog() {
        return operationalBacklog;
    }

    public String getOperationalBacklogStatus() {
        return operationalBacklogStatus;
    }

    public int getSlaRisk() {
        return slaRisk;
    }

    public String getSlaRiskStatus() {
        return slaRiskStatus;
    }


    public List<KpiResultDto> getKpis() {
        return kpis;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getDataStatus() {
        return dataStatus;
    }

    public String getCitrixDataStatus() {
        return citrixDataStatus;
    }

    public String getArubaDataStatus() {
        return arubaDataStatus;
    }

    public String getMicrosoft365DataStatus() {
        return microsoft365DataStatus;
    }

    public String getGlpiDataStatus() {
        return glpiDataStatus;
    }

    public void setGlobalHealth(String globalHealth) {
        this.globalHealth = globalHealth;
    }

    public void setGlobalHealthPercentage(int globalHealthPercentage) {
        this.globalHealthPercentage = globalHealthPercentage;
    }

    public void setGlobalHealthStatus(String globalHealthStatus) {
        this.globalHealthStatus = globalHealthStatus;
    }

    public void setGlobalCriticality(int globalCriticality) {
        this.globalCriticality = globalCriticality;
    }

    public void setGlobalCriticalityStatus(String globalCriticalityStatus) {
        this.globalCriticalityStatus = globalCriticalityStatus;
    }

    public void setGlobalAvailability(int globalAvailability) {
        this.globalAvailability = globalAvailability;
    }

    public void setGlobalAvailabilityStatus(String globalAvailabilityStatus) {
        this.globalAvailabilityStatus = globalAvailabilityStatus;
    }

    public void setUserImpact(int userImpact) {
        this.userImpact = userImpact;
    }

    public void setUserImpactStatus(String userImpactStatus) {
        this.userImpactStatus = userImpactStatus;
    }

    public void setAffectedServicesPercent(int affectedServicesPercent) {
        this.affectedServicesPercent = affectedServicesPercent;
    }

    public void setAffectedServicesStatus(String affectedServicesStatus) {
        this.affectedServicesStatus = affectedServicesStatus;
    }

    public void setTechnicalDegradation(int technicalDegradation) {
        this.technicalDegradation = technicalDegradation;
    }

    public void setTechnicalDegradationStatus(String technicalDegradationStatus) {
        this.technicalDegradationStatus = technicalDegradationStatus;
    }

    public void setOperationalPressure(int operationalPressure) {
        this.operationalPressure = operationalPressure;
    }

    public void setOperationalPressureStatus(String operationalPressureStatus) {
        this.operationalPressureStatus = operationalPressureStatus;
    }

    public void setOperationalBacklog(int operationalBacklog) {
        this.operationalBacklog = operationalBacklog;
    }

    public void setOperationalBacklogStatus(String operationalBacklogStatus) {
        this.operationalBacklogStatus = operationalBacklogStatus;
    }

    public void setSlaRisk(int slaRisk) {
        this.slaRisk = slaRisk;
    }

    public void setSlaRiskStatus(String slaRiskStatus) {
        this.slaRiskStatus = slaRiskStatus;
    }

    public void setKpis(List<KpiResultDto> kpis) {
        this.kpis = kpis;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setDataStatus(String dataStatus) {
        this.dataStatus = dataStatus;
    }

    public void setArubaDataStatus(String arubaDataStatus) {
        this.arubaDataStatus = arubaDataStatus;
    }

    public void setCitrixDataStatus(String citrixDataStatus) {
        this.citrixDataStatus = citrixDataStatus;
    }

    public void setMicrosoft365DataStatus(String microsoft365DataStatus) {
        this.microsoft365DataStatus = microsoft365DataStatus;
    }

    public void setGlpiDataStatus(String glpiDataStatus) {
        this.glpiDataStatus = glpiDataStatus;
    }
}
