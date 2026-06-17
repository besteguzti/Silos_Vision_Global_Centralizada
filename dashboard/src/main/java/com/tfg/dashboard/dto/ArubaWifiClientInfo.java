package com.tfg.dashboard.dto;

public class ArubaWifiClientInfo {

    private String associatedDevice;
    private String associatedDeviceMac;
    private String associatedDeviceName;
    private String groupName;
    private String hostname;
    private String ipAddress;
    private Long lastConnectionTime;
    private String macaddr;
    private String network;
    private String osType;

    public String getAssociatedDevice() {
        return associatedDevice;
    }

    public String getAssociatedDeviceMac() {
        return associatedDeviceMac;
    }

    public String getAssociatedDeviceName() {
        return associatedDeviceName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getLastConnectionTime() {
        return lastConnectionTime;
    }

    public String getMacaddr() {
        return macaddr;
    }

    public String getNetwork() {
        return network;
    }

    public String getOsType() {
        return osType;
    }

    public void setAssociatedDevice(String associatedDevice) {
        this.associatedDevice = associatedDevice;
    }

    public void setAssociatedDeviceMac(String associatedDeviceMac) {
        this.associatedDeviceMac = associatedDeviceMac;
    }

    public void setAssociatedDeviceName(String associatedDeviceName) {
        this.associatedDeviceName = associatedDeviceName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setLastConnectionTime(Long lastConnectionTime) {
        this.lastConnectionTime = lastConnectionTime;
    }

    public void setMacaddr(String macaddr) {
        this.macaddr = macaddr;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }
}
