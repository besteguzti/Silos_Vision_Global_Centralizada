package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

import com.tfg.dashboard.model.AccessPoint;

/**
 * AP almacenado en MySQL que se devuelve desde la API.
 *
 * Evita exponer directamente la entidad JPA.
 */
public class ArubaStoredAccessPointDto {

    private final String name;
    private final String status;
    private final String ipAddress;
    private final String publicIpAddress;
    private final String serial;
    private final String site;
    private final String firmwareVersion;
    private final String macaddr;
    private final String swarmName;
    private final LocalDateTime firstSeenAt;
    private final LocalDateTime lastSeenAt;

    public ArubaStoredAccessPointDto(AccessPoint accessPoint) {
        this.name = accessPoint.getName();
        this.status = accessPoint.getStatus();
        this.ipAddress = accessPoint.getIpAddress();
        this.publicIpAddress = accessPoint.getPublicIpAddress();
        this.serial = accessPoint.getSerial();
        this.site = accessPoint.getSite();
        this.firmwareVersion = accessPoint.getFirmwareVersion();
        this.macaddr = accessPoint.getMacaddr();
        this.swarmName = accessPoint.getSwarmName();
        this.firstSeenAt = accessPoint.getFirstSeenAt();
        this.lastSeenAt = accessPoint.getLastSeenAt();
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public String getSerial() {
        return serial;
    }

    public String getSite() {
        return site;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getMacaddr() {
        return macaddr;
    }

    public String getSwarmName() {
        return swarmName;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
