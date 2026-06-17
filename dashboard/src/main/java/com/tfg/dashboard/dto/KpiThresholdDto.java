package com.tfg.dashboard.dto;

public class KpiThresholdDto {

    private String green;
    private String yellow;
    private String red;

    public KpiThresholdDto() {
    }

    public KpiThresholdDto(String green, String yellow, String red) {

        this.green = green;
        this.yellow = yellow;
        this.red = red;
    }

    public String getGreen() {
        return green;
    }

    public String getYellow() {
        return yellow;
    }

    public String getRed() {
        return red;
    }

    public void setGreen(String green) {
        this.green = green;
    }

    public void setYellow(String yellow) {
        this.yellow = yellow;
    }

    public void setRed(String red) {
        this.red = red;
    }
}
