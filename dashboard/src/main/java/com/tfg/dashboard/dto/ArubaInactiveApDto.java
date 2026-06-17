package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

/**
 * AP considerado inactivo en Aruba.
 * Se usa para explicar el contador de APs inactivos. El cálculo se basa en lastSeenAt y no en la fecha de guardado local.
 */
public class ArubaInactiveApDto {

    private String serial;
    private String name;
    private String status;
    private String site;
    private String swarmName;
    private LocalDateTime lastSeenAt;
    private long daysInactive;
    private String annotation;

    public ArubaInactiveApDto() {
    }

    public ArubaInactiveApDto(
            String serial,
            String name,
            String status,
            String site,
            String swarmName,
            LocalDateTime lastSeenAt,
            long daysInactive,
            String annotation) {

        this.serial = serial;
        this.name = name;
        this.status = status;
        this.site = site;
        this.swarmName = swarmName;
        this.lastSeenAt = lastSeenAt;
        this.daysInactive = daysInactive;
        this.annotation = annotation;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

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

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
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

    public long getDaysInactive() {
        return daysInactive;
    }

    public void setDaysInactive(long daysInactive) {
        this.daysInactive = daysInactive;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
