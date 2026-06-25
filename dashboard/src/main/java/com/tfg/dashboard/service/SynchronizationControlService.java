package com.tfg.dashboard.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfg.dashboard.dto.SynchronizationControlDto;
import com.tfg.dashboard.model.SynchronizationControl;
import com.tfg.dashboard.repository.SynchronizationControlRepository;

/**
 * Gestiona el estado de la sincronizacion automatica.
 *
 * El estado se persiste en MySQL para que el scheduler respete una pausa
 * aunque la aplicacion se reinicie. La sincronizacion manual no consulta este
 * bloqueo, porque debe seguir funcionando como accion explicita del usuario.
 */
@Service
public class SynchronizationControlService {

    private static final Logger log =
            LoggerFactory.getLogger(SynchronizationControlService.class);

    private static final Long CONTROL_ID = 1L;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAUSED = "PAUSED";

    private final SynchronizationControlRepository repository;

    public SynchronizationControlService(
            SynchronizationControlRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public boolean isAutomaticSyncEnabled() {
        return control().getAutomaticSyncEnabled();
    }

    @Transactional
    public SynchronizationControlDto getStatus() {
        return toDto(control());
    }

    @Transactional
    public SynchronizationControlDto pauseAutomaticSync() {
        SynchronizationControl control =
                control();
        control.setAutomaticSyncEnabled(false);
        control.setUpdatedAt(LocalDateTime.now());
        SynchronizationControl saved =
                repository.save(control);

        log.info("Automatic synchronization paused by user");

        return toDto(saved);
    }

    @Transactional
    public SynchronizationControlDto resumeAutomaticSync() {
        SynchronizationControl control =
                control();
        control.setAutomaticSyncEnabled(true);
        control.setUpdatedAt(LocalDateTime.now());
        SynchronizationControl saved =
                repository.save(control);

        log.info("Automatic synchronization resumed by user");

        return toDto(saved);
    }

    private SynchronizationControl control() {
        return repository.findById(CONTROL_ID)
                .orElseGet(this::createDefaultControl);
    }

    private SynchronizationControl createDefaultControl() {
        SynchronizationControl control =
                new SynchronizationControl();
        control.setId(CONTROL_ID);
        control.setAutomaticSyncEnabled(true);
        control.setUpdatedAt(LocalDateTime.now());

        return repository.save(control);
    }

    private SynchronizationControlDto toDto(SynchronizationControl control) {
        boolean enabled =
                Boolean.TRUE.equals(control.getAutomaticSyncEnabled());
        String status =
                enabled ? STATUS_ACTIVE : STATUS_PAUSED;
        String message =
                enabled
                        ? "La sincronizacion automatica esta activa."
                        : "La sincronizacion automatica esta pausada.";

        return new SynchronizationControlDto(
                enabled,
                status,
                message,
                control.getUpdatedAt()
        );
    }
}
