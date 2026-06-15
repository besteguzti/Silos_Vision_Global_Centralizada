package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Histórico de KPIs transversales.
 *
 * Almacena código, nombre, unidad, valor y fecha de captura para poder comparar
 * indicadores globales en el módulo de análisis.
 */
@Entity
@Table(name = "transversal_kpi_history")
public class TransversalKpiHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_code")
    private String kpiCode;

    @Column(name = "kpi_name")
    private String kpiName;
    private Double value;
    private String unit;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public TransversalKpiHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getKpiCode() {
        return kpiCode;
    }

    public String getKpiName() {
        return kpiName;
    }

    public Double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKpiCode(String kpiCode) {
        this.kpiCode = kpiCode;
    }

    public void setKpiName(String kpiName) {
        this.kpiName = kpiName;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}
