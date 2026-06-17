package com.tfg.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

// Resumen de avisos detectados para una plataforma.
public class PlatformFindingDto {

    private String platform;
    private String status;
    private List<String> findings = new ArrayList<>();

    public PlatformFindingDto() {
    }

    public PlatformFindingDto(String platform, String status, List<String> findings) {
        this.platform = platform;
        this.status = status;
        this.findings = findings == null ? new ArrayList<>() : new ArrayList<>(findings);
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getFindings() {
        return findings;
    }

    public void setFindings(List<String> findings) {
        this.findings = findings == null ? new ArrayList<>() : new ArrayList<>(findings);
    }
}
