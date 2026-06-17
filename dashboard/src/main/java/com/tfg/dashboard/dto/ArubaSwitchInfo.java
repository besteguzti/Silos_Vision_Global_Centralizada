package com.tfg.dashboard.dto;

public class ArubaSwitchInfo {

    private String serial;
    private String macAddress;
    private String hostname;
    private String model;
    private String deviceStatus;
    private boolean upgradeRequired;
    private String statusState;

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
}
