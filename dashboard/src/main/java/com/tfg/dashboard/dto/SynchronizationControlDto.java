package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

public class SynchronizationControlDto {

    private boolean automaticSyncEnabled;
    private String status;
    private String message;
    private LocalDateTime updatedAt;

    public SynchronizationControlDto() {
    }

    public SynchronizationControlDto(
            boolean automaticSyncEnabled,
            String status,
            String message,
            LocalDateTime updatedAt
    ) {
        this.automaticSyncEnabled = automaticSyncEnabled;
        this.status = status;
        this.message = message;
        this.updatedAt = updatedAt;
    }

    public boolean isAutomaticSyncEnabled() {
        return automaticSyncEnabled;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setAutomaticSyncEnabled(boolean automaticSyncEnabled) {
        this.automaticSyncEnabled = automaticSyncEnabled;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
