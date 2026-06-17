package com.tfg.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class KpiResultDto {

    //DTO común para devolver KPIs con la misma estructura en todos los paneles.
    private String id;
    private String name;
    private Object value;
    private KpiStatus status;
    private String description;
    private String calculation;
    private LocalDateTime timestamp;
    private String freshness;
    private Integer score;
    private List<KpiResultDto> components;

    public KpiResultDto() {
    }

    public KpiResultDto(
            String id,
            String name,
            Object value,
            KpiStatus status,
            String description,
            String calculation,
            LocalDateTime timestamp,
            String freshness,
            Integer score,
            List<KpiResultDto> components) {

        this.id = id;
        this.name = name;
        this.value = value;
        this.status = status;
        this.description = description;
        this.calculation = calculation;
        this.timestamp = timestamp;
        this.freshness = freshness;
        this.score = score;
        this.components = components;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public KpiStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getCalculation() {
        return calculation;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFreshness() {
        return freshness;
    }

    public Integer getScore() {
        return score;
    }

    public List<KpiResultDto> getComponents() {
        return components;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setStatus(KpiStatus status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCalculation(String calculation) {
        this.calculation = calculation;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setFreshness(String freshness) {
        this.freshness = freshness;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public void setComponents(List<KpiResultDto> components) {
        this.components = components;
    }
}

