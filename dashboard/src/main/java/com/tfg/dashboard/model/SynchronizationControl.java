package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Estado persistido de la sincronizacion automatica.
 *
 * Permite mantener la pausa aunque se reinicie Spring. No afecta a la sincronización manual.
 */
@Entity
@Table(name = "synchronization_control")
public class SynchronizationControl {

    @Id
    private Long id;

    @Column(nullable = false)
    private Boolean automaticSyncEnabled;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Boolean getAutomaticSyncEnabled() {
        return automaticSyncEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAutomaticSyncEnabled(Boolean automaticSyncEnabled) {
        this.automaticSyncEnabled = automaticSyncEnabled;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
