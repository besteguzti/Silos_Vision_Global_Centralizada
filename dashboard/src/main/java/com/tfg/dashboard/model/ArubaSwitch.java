package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "aruba_switches", uniqueConstraints = @UniqueConstraint(name = "uk_aruba_switch_serial", columnNames = "serial"))

public class ArubaSwitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serial;
    private String macAddress;
    private String hostname;
    private String model;
    private String deviceStatus;
    private boolean upgradeRequired;
    private String statusState;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    public Long getId() {
        return id;
    }

    public String getSerial() {
        return serial;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public String getModel() {
        return model;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public boolean isUpgradeRequired() {
        return upgradeRequired;
    }

    public String getStatusState() {
        return statusState;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public void setUpgradeRequired(boolean upgradeRequired) {
        this.upgradeRequired = upgradeRequired;
    }

    public void setStatusState(String statusState) {
        this.statusState = statusState;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
