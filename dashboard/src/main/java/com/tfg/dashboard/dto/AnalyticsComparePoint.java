package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

public class AnalyticsComparePoint {

    private LocalDateTime timestamp;

    private Double x;

    private Double y;

    public AnalyticsComparePoint() {
    }

    public AnalyticsComparePoint(LocalDateTime timestamp,Double x,Double y) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(Double y) {
        this.y = y;
    }
}
