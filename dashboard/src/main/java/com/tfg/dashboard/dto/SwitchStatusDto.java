package com.tfg.dashboard.dto;

import java.util.List;

public class SwitchStatusDto {

    private int percentageContribution;
    private String color;
    private int totalSwitches;
    private int downSwitches;
    private int pendingFirmwareSwitches;
    private List<String> reasons;

    public int getPercentageContribution() {
        return percentageContribution;
    }

    public String getColor() {
        return color;
    }

    public int getTotalSwitches() {
        return totalSwitches;
    }

    public int getDownSwitches() {
        return downSwitches;
    }

    public int getPendingFirmwareSwitches() {
        return pendingFirmwareSwitches;
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

    public void setTotalSwitches(int totalSwitches) {
        this.totalSwitches = totalSwitches;
    }

    public void setDownSwitches(int downSwitches) {
        this.downSwitches = downSwitches;
    }

    public void setPendingFirmwareSwitches(int pendingFirmwareSwitches) {
        this.pendingFirmwareSwitches = pendingFirmwareSwitches;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
