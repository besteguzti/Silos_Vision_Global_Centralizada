package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

import com.tfg.dashboard.model.ArubaSwitchClientUsage;

//Uso de interfaces de un switch Aruba devuelto por la API.
public class ArubaSwitchClientUsageDto {

    private final String associatedDevice;
    private final String associatedDeviceName;
    private final String associatedDeviceMac;
    private final String deviceStatus;
    private final int downInterfaces;
    private final LocalDateTime updatedAt;

    public ArubaSwitchClientUsageDto(ArubaSwitchClientUsage usage) {
        this.associatedDevice = usage.getAssociatedDevice();
        this.associatedDeviceName = usage.getAssociatedDeviceName();
        this.associatedDeviceMac = usage.getAssociatedDeviceMac();
        this.deviceStatus = usage.getDeviceStatus();
        this.downInterfaces = usage.getDownInterfaces();
        this.updatedAt = usage.getUpdatedAt();
    }

    public String getAssociatedDevice() {
        return associatedDevice;
    }

    public String getAssociatedDeviceName() {
        return associatedDeviceName;
    }

    public String getAssociatedDeviceMac() {
        return associatedDeviceMac;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public int getDownInterfaces() {
        return downInterfaces;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
