package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Anotación manual asociada a un AP Aruba.
 *
 * Se guarda separada del inventario sincronizado para que las sincronizaciones
 * no sobrescriban notas escritas por el usuario.
 */
@Entity
@Table(
        name = "aruba_ap_annotations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aruba_ap_annotation_serial",
                columnNames = "serial"))
public class ArubaApAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serial;

    @Column(length = 1000)
    private String annotation;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getSerial() {
        return serial;
    }

    public String getAnnotation() {
        return annotation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

