package com.tfg.dashboard.dto;

public class Microsoft365IndicatorStatusDto {

    private String name;
    private String color;
    private int affectionPercent;
    private String reason;

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getAffectionPercent() {
        return affectionPercent;
    }

    public String getReason() {
        return reason;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setAffectionPercent(int affectionPercent) {
        this.affectionPercent = affectionPercent;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
