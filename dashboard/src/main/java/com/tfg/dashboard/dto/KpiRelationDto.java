package com.tfg.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Relación exploratoria entre dos indicadores.
 * Incluye la definición, los puntos históricos y una lectura automática.
 */
public class KpiRelationDto {

    private String code;
    private String title;
    private String xLabel;
    private String yLabel;
    private String xUnit;
    private String yUnit;
    private String description;
    private String reading;
    private String readingStatus;
    private boolean hasEnoughData;
    private List<KpiRelationPointDto> points;

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("xLabel")
    public String getXLabel() {
        return xLabel;
    }

    @JsonProperty("yLabel")
    public String getYLabel() {
        return yLabel;
    }

    @JsonProperty("xUnit")
    public String getXUnit() {
        return xUnit;
    }

    @JsonProperty("yUnit")
    public String getYUnit() {
        return yUnit;
    }

    public String getDescription() {
        return description;
    }

    public String getReading() {
        return reading;
    }

    public String getReadingStatus() {
        return readingStatus;
    }

    @JsonProperty("hasEnoughData")
    public boolean isHasEnoughData() {
        return hasEnoughData;
    }

    public List<KpiRelationPointDto> getPoints() {
        return points;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setXLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    public void setYLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    public void setXUnit(String xUnit) {
        this.xUnit = xUnit;
    }

    public void setYUnit(String yUnit) {
        this.yUnit = yUnit;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public void setReadingStatus(String readingStatus) {
        this.readingStatus = readingStatus;
    }

    public void setHasEnoughData(boolean hasEnoughData) {
        this.hasEnoughData = hasEnoughData;
    }

    public void setPoints(List<KpiRelationPointDto> points) {
        this.points = points;
    }
}

