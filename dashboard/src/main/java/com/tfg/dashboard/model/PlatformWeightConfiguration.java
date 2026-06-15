package com.tfg.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Peso editable de una plataforma dentro del KPI Estado global.
 *
 * Se persiste en porcentaje entero para facilitar validaciónes de suma 100 en
 * el panel de configuración. El servicio lo convierte a decimal antes de aplicarlo sobre KpiProperties.
 */
@Entity
@Table(
        name = "platform_weight_configuration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_platform_weight_platform",
                columnNames = "platform"))
public class PlatformWeightConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String platform;

    @Column(nullable = false)
    private Integer weightPercent;

    @Column(nullable = false)
    private Integer defaultWeightPercent;

    public Long getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }

    public Integer getWeightPercent() {
        return weightPercent;
    }

    public Integer getDefaultWeightPercent() {
        return defaultWeightPercent;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setWeightPercent(Integer weightPercent) {
        this.weightPercent = weightPercent;
    }

    public void setDefaultWeightPercent(Integer defaultWeightPercent) {
        this.defaultWeightPercent = defaultWeightPercent;
    }
}

