package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

import com.tfg.dashboard.model.ArubaSwitch;

/**
 * Switches Aruba almacenados en MySQL que se devuelven desde la API.
 * Evita exponer directamente la entidad JPA.
 */
public class ArubaStoredSwitchDto {

    private final String serial;
    private final String macAddress;
    private final String hostname;
    private final String model;
    private final String deviceStatus;
    private final boolean upgradeRequired;
    private final String statusState;
    private final LocalDateTime firstSeenAt;
    private final LocalDateTime lastSeenAt;

    public ArubaStoredSwitchDto(ArubaSwitch arubaSwitch) {
        this.serial = arubaSwitch.getSerial();
        this.macAddress = arubaSwitch.getMacAddress();
        this.hostname = arubaSwitch.getHostname();
        this.model = arubaSwitch.getModel();
        this.deviceStatus = arubaSwitch.getDeviceStatus();
        this.upgradeRequired = arubaSwitch.isUpgradeRequired();
        this.statusState = arubaSwitch.getStatusState();
        this.firstSeenAt = arubaSwitch.getFirstSeenAt();
        this.lastSeenAt = arubaSwitch.getLastSeenAt();
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
}
