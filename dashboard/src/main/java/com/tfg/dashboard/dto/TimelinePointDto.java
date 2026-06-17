package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

public class TimelinePointDto {

    private LocalDateTime timestamp;
    private Double aruba;
    private Double citrix;
    private Double microsoft365;
    private Double technicalDegradation;
    private Double glpiOperationalPressure;
    private Double userImpact;
    private Double globalStatus;
    private Boolean generatedScenario;

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Double getAruba() {
        return aruba;
    }

    public Double getCitrix() {
        return citrix;
    }

    public Double getMicrosoft365() {
        return microsoft365;
    }

    public Double getTechnicalDegradation() {
        return technicalDegradation;
    }

    public Double getGlpiOperationalPressure() {
        return glpiOperationalPressure;
    }

    public Double getUserImpact() {
        return userImpact;
    }

    public Double getGlobalStatus() {
        return globalStatus;
    }

    public Boolean isGeneratedScenario() {
        return generatedScenario;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setAruba(Double aruba) {
        this.aruba = aruba;
    }

    public void setCitrix(Double citrix) {
        this.citrix = citrix;
    }

    public void setMicrosoft365(Double microsoft365) {
        this.microsoft365 = microsoft365;
    }

    public void setTechnicalDegradation(Double technicalDegradation) {
        this.technicalDegradation = technicalDegradation;
    }

    public void setGlpiOperationalPressure(Double glpiOperationalPressure) {
        this.glpiOperationalPressure = glpiOperationalPressure;
    }

    public void setUserImpact(Double userImpact) {
        this.userImpact = userImpact;
    }

    public void setGlobalStatus(Double globalStatus) {
        this.globalStatus = globalStatus;
    }

    public void setGeneratedScenario(Boolean generatedScenario) {
        this.generatedScenario = generatedScenario;
    }
}
