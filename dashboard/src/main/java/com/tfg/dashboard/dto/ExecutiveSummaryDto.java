package com.tfg.dashboard.dto;

import java.util.List;

public class ExecutiveSummaryDto {

    private String globalStatus;
    private List<String> affectedServices;
    private String mainAffectedPlatform;
    private String probableOrigin;
    private String impactLevel;
    private String estimatedAffectedUsers;
    private String priority;
    private String firstAction;
    private String trend;
    private String summaryText;
    private List<PlatformFindingDto> platformFindings = List.of();

    public ExecutiveSummaryDto() {
    }

    public String getGlobalStatus() {
        return globalStatus;
    }

    public List<String> getAffectedServices() {
        return affectedServices;
    }

    public String getMainAffectedPlatform() {
        return mainAffectedPlatform;
    }

    public String getProbableOrigin() {
        return probableOrigin;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public String getEstimatedAffectedUsers() {
        return estimatedAffectedUsers;
    }

    public String getPriority() {
        return priority;
    }

    public String getFirstAction() {
        return firstAction;
    }

    public String getTrend() {
        return trend;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public List<PlatformFindingDto> getPlatformFindings() {
        return platformFindings;
    }

    public void setGlobalStatus(String globalStatus) {
        this.globalStatus = globalStatus;
    }

    public void setAffectedServices(List<String> affectedServices) {
        this.affectedServices = affectedServices;
    }

    public void setMainAffectedPlatform(String mainAffectedPlatform) {
        this.mainAffectedPlatform = mainAffectedPlatform;
    }

    public void setProbableOrigin(String probableOrigin) {
        this.probableOrigin = probableOrigin;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public void setEstimatedAffectedUsers(String estimatedAffectedUsers) {
        this.estimatedAffectedUsers = estimatedAffectedUsers;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setFirstAction(String firstAction) {
        this.firstAction = firstAction;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public void setPlatformFindings(List<PlatformFindingDto> platformFindings) {
        this.platformFindings = platformFindings == null ? List.of() : platformFindings;
    }
}
