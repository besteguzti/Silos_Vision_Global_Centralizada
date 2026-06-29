package com.tfg.dashboard.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.service.ArubaService;
import com.tfg.dashboard.service.SynchronizationControlService;

@ExtendWith(MockitoExtension.class)
class ArubaSchedulerTest {

    @Mock
    private ArubaService arubaService;

    @Mock
    private SynchronizationControlService synchronizationControlService;

    @Test
    void scheduledArubaSyncIsSkippedWhenAutomaticSynchronizationIsPaused() {
        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(false);

        ArubaScheduler scheduler =
                new ArubaScheduler(arubaService, synchronizationControlService);

        scheduler.syncAruba();

        verify(arubaService, never()).syncAll();
    }

    @Test
    void scheduledArubaSyncRunsWhenAutomaticSynchronizationIsActive() {
        when(synchronizationControlService.isAutomaticSyncEnabled())
                .thenReturn(true);

        ArubaScheduler scheduler =
                new ArubaScheduler(arubaService, synchronizationControlService);

        scheduler.syncAruba();

        verify(arubaService).syncAll();
    }
}
