package com.tfg.dashboard.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ManualSyncResponseDto {

    private String status;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<ManualSyncPlatformResultDto> platforms = new ArrayList<>();

    public ManualSyncResponseDto() {
    }

    public ManualSyncResponseDto(
            String status,
            String message,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            List<ManualSyncPlatformResultDto> platforms
    ) {
        this.status = status;
        this.message = message;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.platforms = platforms;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<ManualSyncPlatformResultDto> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<ManualSyncPlatformResultDto> platforms) {
        this.platforms = platforms;
    }
}
