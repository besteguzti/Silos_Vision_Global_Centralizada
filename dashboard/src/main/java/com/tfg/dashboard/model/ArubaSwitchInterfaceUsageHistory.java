package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aruba_switch_interface_usage_history")
public class ArubaSwitchInterfaceUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "associated_device", nullable = false)
    private String associatedDevice;

    @Column(name = "associated_device_name")
    private String associatedDeviceName;

    @Column(name = "associated_device_mac")
    private String associatedDeviceMac;

    @Column(name = "device_status")
    private String deviceStatus;

    @Column(name = "down_interfaces")
    private Integer downInterfaces;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    public Long getId() {
        return id;
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
        return downInterfaces == null ? 0 : downInterfaces;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setAssociatedDevice(String associatedDevice) {
        this.associatedDevice = associatedDevice;
    }

    public void setAssociatedDeviceName(String associatedDeviceName) {
        this.associatedDeviceName = associatedDeviceName;
    }

    public void setAssociatedDeviceMac(String associatedDeviceMac) {
        this.associatedDeviceMac = associatedDeviceMac;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public void setDownInterfaces(int downInterfaces) {
        this.downInterfaces = downInterfaces;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }
}
