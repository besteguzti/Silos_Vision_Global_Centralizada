package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Snapshot histórico de métricas Citrix simuladas.
 *
 * Cada fila representa una captura generada por MetricsSyncService y sirve para consultar el último estado,
 * calcular frescura y alimentar KPIs transversales.
 */
@Entity
@Table(name = "citrix_metrics_history")
public class CitrixMetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer activeSessions;
    private Integer activeLicenses;
    private Integer availableDeliveryControllers;
    private Integer totalDeliveryControllers;
    private Integer disconnectedSessions;
    private Integer averageLogonDurationSeconds;
    private Integer serverLoadPercent;
    private Integer failedLogons;
    private String citrixHealth;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public CitrixMetricsHistory() {
    }

    public Long getId() {
        return id;
    }

    public int getActiveSessions() {
        return activeSessions == null ? 0 : activeSessions;
    }

    public int getActiveLicenses() {
        return activeLicenses == null ? 0 : activeLicenses;
    }

    public int getAvailableDeliveryControllers() {
        return availableDeliveryControllers == null
                ? 0
                : availableDeliveryControllers;
    }

    public int getTotalDeliveryControllers() {
        return totalDeliveryControllers == null
                ? 0
                : totalDeliveryControllers;
    }

    public int getDisconnectedSessions() {
        return disconnectedSessions == null ? 0 : disconnectedSessions;
    }

    public int getAverageLogonDurationSeconds() {
        return averageLogonDurationSeconds == null
                ? 0
                : averageLogonDurationSeconds;
    }

    public int getServerLoadPercent() {
        return serverLoadPercent == null ? 0 : serverLoadPercent;
    }

    public int getFailedLogons() {
        return failedLogons == null ? 0 : failedLogons;
    }

    public String getCitrixHealth() {
        return citrixHealth;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setActiveSessions(Integer activeSessions) {
        this.activeSessions = activeSessions;
    }

    public void setActiveLicenses(Integer activeLicenses) {
        this.activeLicenses = activeLicenses;
    }

    public void setAvailableDeliveryControllers(Integer availableDeliveryControllers) {
        this.availableDeliveryControllers = availableDeliveryControllers;
    }

    public void setTotalDeliveryControllers(Integer totalDeliveryControllers) {
        this.totalDeliveryControllers = totalDeliveryControllers;
    }

    public void setDisconnectedSessions(Integer disconnectedSessions) {
        this.disconnectedSessions = disconnectedSessions;
    }

    public void setAverageLogonDurationSeconds(Integer averageLogonDurationSeconds) {
        this.averageLogonDurationSeconds = averageLogonDurationSeconds;
    }

    public void setServerLoadPercent(Integer serverLoadPercent) {
        this.serverLoadPercent = serverLoadPercent;
    }

    public void setFailedLogons(Integer failedLogons) {
        this.failedLogons = failedLogons;
    }

    public void setCitrixHealth(String citrixHealth) {
        this.citrixHealth = citrixHealth;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}

