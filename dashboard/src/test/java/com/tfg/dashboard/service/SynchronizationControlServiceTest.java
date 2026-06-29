package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.dto.SynchronizationControlDto;
import com.tfg.dashboard.model.SynchronizationControl;
import com.tfg.dashboard.repository.SynchronizationControlRepository;

@ExtendWith(MockitoExtension.class)
class SynchronizationControlServiceTest {

    @Mock
    private SynchronizationControlRepository repository;

    @Test
    void defaultStateIsActiveAndPersistedWhenConfigurationDoesNotExist() {
        when(repository.findById(1L))
                .thenReturn(Optional.empty());
        when(repository.save(any(SynchronizationControl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizationControlService service =
                new SynchronizationControlService(repository);

        SynchronizationControlDto status =
                service.getStatus();

        assertThat(status.isAutomaticSyncEnabled()).isTrue();
        assertThat(status.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<SynchronizationControl> captor =
                ArgumentCaptor.forClass(SynchronizationControl.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAutomaticSyncEnabled()).isTrue();
    }

    @Test
    void pauseChangesStateToPaused() {
        SynchronizationControl stored =
                control(true);
        when(repository.findById(1L))
                .thenReturn(Optional.of(stored));
        when(repository.save(any(SynchronizationControl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizationControlService service =
                new SynchronizationControlService(repository);

        SynchronizationControlDto status =
                service.pauseAutomaticSync();

        assertThat(status.isAutomaticSyncEnabled()).isFalse();
        assertThat(status.getStatus()).isEqualTo("PAUSED");
        verify(repository).save(stored);
    }

    @Test
    void resumeChangesStateToActive() {
        SynchronizationControl stored =
                control(false);
        when(repository.findById(1L))
                .thenReturn(Optional.of(stored));
        when(repository.save(any(SynchronizationControl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizationControlService service =
                new SynchronizationControlService(repository);

        SynchronizationControlDto status =
                service.resumeAutomaticSync();

        assertThat(status.isAutomaticSyncEnabled()).isTrue();
        assertThat(status.getStatus()).isEqualTo("ACTIVE");
        verify(repository).save(stored);
    }

    private SynchronizationControl control(boolean enabled) {
        SynchronizationControl control =
                new SynchronizationControl();
        control.setId(1L);
        control.setAutomaticSyncEnabled(enabled);
        control.setUpdatedAt(LocalDateTime.parse("2026-06-05T12:30:00"));
        return control;
    }
}
