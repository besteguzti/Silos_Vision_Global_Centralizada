package com.tfg.dashboard.dto;

import java.util.List;

public class AccessPointStatusDto {

    private int percentageContribution;
    private String color;
    private int totalAps;
    private int downAps;
    private int inactiveAps;
    private int pendingFirmwareAps;
    private int totalWifiClients;
    private int mutualiaApsClients;
    private int mutualiaWifiClients;
    private List<String> reasons;

    public int getPercentageContribution() {
        return percentageContribution;
    }

    public String getColor() {
        return color;
    }

    public int getTotalAps() {
        return totalAps;
    }

    public int getDownAps() {
        return downAps;
    }

    public int getInactiveAps() {
        return inactiveAps;
    }

    public int getPendingFirmwareAps() {
        return pendingFirmwareAps;
    }

    public int getTotalWifiClients() {
        return totalWifiClients;
    }

    public int getMutualiaApsClients() {
        return mutualiaApsClients;
    }

    public int getMutualiaWifiClients() {
        return mutualiaWifiClients;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setPercentageContribution(int percentageContribution) {
        this.percentageContribution = percentageContribution;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTotalAps(int totalAps) {
        this.totalAps = totalAps;
    }

    public void setDownAps(int downAps) {
        this.downAps = downAps;
    }

    public void setInactiveAps(int inactiveAps) {
        this.inactiveAps = inactiveAps;
    }

    public void setPendingFirmwareAps(int pendingFirmwareAps) {
        this.pendingFirmwareAps = pendingFirmwareAps;
    }

    public void setTotalWifiClients(int totalWifiClients) {
        this.totalWifiClients = totalWifiClients;
    }

    public void setMutualiaApsClients(int mutualiaApsClients) {
        this.mutualiaApsClients = mutualiaApsClients;
    }

    public void setMutualiaWifiClients(int mutualiaWifiClients) {
        this.mutualiaWifiClients = mutualiaWifiClients;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
