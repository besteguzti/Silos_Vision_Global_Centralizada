package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Métricas agregadas de Aruba para la vista de resumen.
 *
 * Se actualizan durante la sincronización para no consultar Aruba Central en cada petición del dashboard.
 */
@Entity
@Table(name = "aruba_dashboard_metrics")
public class ArubaDashboardMetrics {

    @Id
    private Long id;
    private int firmwareOutdated;
    private int totalWifiClients;
    private int mutualiaApsClients;
    private int mutualiaWifiClients;
    private int mutualiaLangileakClients;
    private int mutualiaClients;
    private int mutualiaRedInternaClients;
    private int mutualiaRedExternaClients;
    private int mutualiaKorporatiboaClients;
    private int wifiPacsClients;
    private int mutVideoClients;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public int getFirmwareOutdated() {
        return firmwareOutdated;
    }

    public int getTotalWifiClients() {
        return totalWifiClients;
    }

    public int getMutualiaApsClients() {
        return mutualiaApsClients;
    }

    public int getMutualiaWifiClients() {
        return mutualiaWifiClients;
    }

    public int getMutualiaLangileakClients() {
        return mutualiaLangileakClients;
    }

    public int getMutualiaClients() {
        return mutualiaClients;
    }

    public int getMutualiaRedInternaClients() {
        return mutualiaRedInternaClients;
    }

    public int getMutualiaRedExternaClients() {
        return mutualiaRedExternaClients;
    }

    public int getMutualiaKorporatiboaClients() {
        return mutualiaKorporatiboaClients;
    }

    public int getWifiPacsClients() {
        return wifiPacsClients;
    }

    public int getMutVideoClients() {
        return mutVideoClients;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirmwareOutdated(int firmwareOutdated) {
        this.firmwareOutdated = firmwareOutdated;
    }

    public void setTotalWifiClients(int totalWifiClients) {
        this.totalWifiClients = totalWifiClients;
    }

    public void setMutualiaApsClients(int mutualiaApsClients) {
        this.mutualiaApsClients = mutualiaApsClients;
    }

    public void setMutualiaWifiClients(int mutualiaWifiClients) {
        this.mutualiaWifiClients = mutualiaWifiClients;
    }

    public void setMutualiaLangileakClients(int mutualiaLangileakClients) {
        this.mutualiaLangileakClients = mutualiaLangileakClients;
    }

    public void setMutualiaClients(int mutualiaClients) {
        this.mutualiaClients = mutualiaClients;
    }

    public void setMutualiaRedInternaClients(int mutualiaRedInternaClients) {
        this.mutualiaRedInternaClients = mutualiaRedInternaClients;
    }

    public void setMutualiaRedExternaClients(int mutualiaRedExternaClients) {
        this.mutualiaRedExternaClients = mutualiaRedExternaClients;
    }

    public void setMutualiaKorporatiboaClients(int mutualiaKorporatiboaClients) {
        this.mutualiaKorporatiboaClients = mutualiaKorporatiboaClients;
    }

    public void setWifiPacsClients(int wifiPacsClients) {
        this.wifiPacsClients = wifiPacsClients;
    }

    public void setMutVideoClients(int mutVideoClients) {
        this.mutVideoClients = mutVideoClients;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

