package com.tfg.dashboard.dto;

import jakarta.validation.constraints.Size;

public class ArubaApAnnotationRequest {

    @Size(max = 1000, message = "La anotacion no puede superar 1000 caracteres.")
    private String annotation;

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
