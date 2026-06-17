package com.tfg.dashboard.dto;

public class ManualSyncPlatformResultDto {

    private String name;
    private String status;
    private String message;

    public ManualSyncPlatformResultDto() {
    }

    public ManualSyncPlatformResultDto(String name, String status, String message) {
        this.name = name;
        this.status = status;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
