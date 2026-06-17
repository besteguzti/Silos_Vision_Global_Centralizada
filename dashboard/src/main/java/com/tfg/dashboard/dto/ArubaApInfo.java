package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArubaApInfo {

    private String name;
    private String status;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("public_ip_address")
    private String publicIpAddress;
    private String serial;
    private String site;

    @JsonProperty("firmware_version")
    private String firmwareVersion;
    private String macaddr;

    @JsonProperty("swarm_name")
    private String swarmName;
    private LocalDateTime lastSeenAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }   

    public String getFirmwareVersion() {
        return firmwareVersion;
    }   

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getMacaddr() {
        return macaddr;
    }

    public void setMacaddr(String macaddr) {
        this.macaddr = macaddr;
    }

    public String getSwarmName() {
        return swarmName;
    }

    public void setSwarmName(String swarmName) {
        this.swarmName = swarmName;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

}
