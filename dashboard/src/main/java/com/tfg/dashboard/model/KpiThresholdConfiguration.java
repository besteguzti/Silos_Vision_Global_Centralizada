package com.tfg.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Umbral KPI editable desde la configuración.
 *
 * La aplicación carga estos valores desde MySQL y los aplica sobre KpiProperties en tiempo de ejecución.
 */
@Entity
@Table(
        name = "kpi_threshold_configuration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kpi_threshold_config_key",
                columnNames = "config_key"))
public class KpiThresholdConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(nullable = false)
    private String sectionKey;

    @Column(nullable = false)
    private String label;

    private String unit;

    @Column(name = "config_value", nullable = false)
    private Integer value;

    @Column(name = "default_value", nullable = false)
    private Integer defaultValue;

    @Column(length = 600)
    private String description;

    public Long getId() {
        return id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getValue() {
        return value;
    }

    public Integer getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setDefaultValue(Integer defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

