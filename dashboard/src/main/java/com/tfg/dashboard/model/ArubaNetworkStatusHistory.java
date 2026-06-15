package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Histórico del índice de salud Aruba.
 *
 * Guarda el porcentaje de afección, el color y los motivos calculados para poder revisar cómo ha evolucionado el índice.
 */
@Entity
@Table(name = "aruba_network_status_history")
public class ArubaNetworkStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int percentage;
    private String color;
    private int accessPointContribution;
    private String accessPointColor;
    private int switchContribution;
    private String switchColor;
    private boolean affectedService;
    private boolean criticalCondition;
    private int technicalDegradationValue;

    @Column(length = 2000)
    private String reasons;
    private LocalDateTime collectedAt;

    public Long getId() {
        return id;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getColor() {
        return color;
    }

    public int getAccessPointContribution() {
        return accessPointContribution;
    }

    public String getAccessPointColor() {
        return accessPointColor;
    }

    public int getSwitchContribution() {
        return switchContribution;
    }

    public String getSwitchColor() {
        return switchColor;
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

    public String getReasons() {
        return reasons;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setAccessPointContribution(int accessPointContribution) {
        this.accessPointContribution = accessPointContribution;
    }

    public void setAccessPointColor(String accessPointColor) {
        this.accessPointColor = accessPointColor;
    }

    public void setSwitchContribution(int switchContribution) {
        this.switchContribution = switchContribution;
    }

    public void setSwitchColor(String switchColor) {
        this.switchColor = switchColor;
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

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}
