package com.tfg.dashboard.dto;

import java.util.List;

public class Microsoft365HealthStatusDto {

    private int percentage;
    private String color;
    private List<Microsoft365IndicatorStatusDto> indicators;
    private List<String> reasons;
    private boolean affectedService;
    private boolean criticalCondition;
    private int technicalDegradationValue;
    private boolean transversalReady;

    public int getPercentage() {
        return percentage;
    }

    public String getColor() {
        return color;
    }

    public List<Microsoft365IndicatorStatusDto> getIndicators() {
        return indicators;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public boolean isAffectedService() {
        return affectedService;
    }

    public boolean isCriticalCondition() {
        return criticalCondition;
    }

    public int getTechnicalDegradationValue() {
        return technicalDegradationValue;
    }

    public boolean isTransversalReady() {
        return transversalReady;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setIndicators(
            List<Microsoft365IndicatorStatusDto> indicators
    ) {
        this.indicators = indicators;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public void setAffectedService(boolean affectedService) {
        this.affectedService = affectedService;
    }

    public void setCriticalCondition(boolean criticalCondition) {
        this.criticalCondition = criticalCondition;
    }

    public void setTechnicalDegradationValue(int technicalDegradationValue) {
        this.technicalDegradationValue = technicalDegradationValue;
    }

    public void setTransversalReady(boolean transversalReady) {
        this.transversalReady = transversalReady;
    }
}
