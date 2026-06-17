package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

public class ArubaApAnnotationDto {

    private String serial;
    private String annotation;
    private LocalDateTime updatedAt;

    public ArubaApAnnotationDto() {
    }

    public ArubaApAnnotationDto(String serial,String annotation,LocalDateTime updatedAt) {
        this.serial = serial;
        this.annotation = annotation;
        this.updatedAt = updatedAt;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
