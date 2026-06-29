package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;
import com.tfg.dashboard.repository.CitrixMetricsHistoryRepository;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

@ExtendWith(MockitoExtension.class)
class DataFreshnessServiceTest {

    @Mock
    private CitrixMetricsHistoryRepository citrixRepository;

    @Mock
    private Microsoft365MetricsHistoryRepository microsoft365Repository;

    @Mock
    private GlpiMetricsHistoryRepository glpiRepository;

    @Mock
    private GlpiPlatformTicketService glpiPlatformTicketService;

    private final KpiProperties kpiProperties =
            new KpiProperties();

    @Test
    void citrixRecentSnapshotReturnsOk() {

        CitrixMetricsHistory history =
                new CitrixMetricsHistory();
        history.setCollectedAt(LocalDateTime.now());
        history.setCitrixHealth("GREEN");

        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        CitrixService service =
                new CitrixService(citrixRepository, glpiPlatformTicketService, kpiProperties);

        assertThat(service.getSummary().getDataStatus())
                .isEqualTo("OK");
    }

    @Test
    void citrixWithoutSnapshotReturnsNoData() {

        when(citrixRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.empty());

        CitrixService service =
                new CitrixService(citrixRepository, glpiPlatformTicketService, kpiProperties);

        assertThat(service.getSummary().getDataStatus())
                .isEqualTo("NO_DATA");
    }

    @Test
    void microsoft365OldSnapshotReturnsStale() {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();
        history.setCollectedAt(LocalDateTime.now().minusMinutes(
                kpiProperties.getFreshness().getMicrosoft365Minutes() + 1));
        history.setMicrosoft365Health("GREEN");

        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        Microsoft365Service service =
                new Microsoft365Service(microsoft365Repository, glpiPlatformTicketService, kpiProperties);

        assertThat(service.getSummary().getDataStatus())
                .isEqualTo("STALE");
    }

    @Test
    void microsoft365SnapshotInsideHourlyFreshnessWindowReturnsOk() {

        Microsoft365MetricsHistory history =
                new Microsoft365MetricsHistory();
        history.setCollectedAt(LocalDateTime.now().minusMinutes(
                kpiProperties.getFreshness().getMicrosoft365Minutes() - 1));
        history.setMicrosoft365Health("GREEN");

        when(microsoft365Repository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        Microsoft365Service service =
                new Microsoft365Service(microsoft365Repository, glpiPlatformTicketService, kpiProperties);

        assertThat(service.getSummary().getDataStatus())
                .isEqualTo("OK");
    }

    @Test
    void glpiRecentSnapshotReturnsOk() {

        GlpiMetricsHistory history =
                new GlpiMetricsHistory();
        history.setCollectedAt(LocalDateTime.now());

        when(glpiRepository.findTopByOrderByCollectedAtDesc())
                .thenReturn(Optional.of(history));

        GlpiService service =
                new GlpiService(glpiRepository, kpiProperties);

        assertThat(service.getSummary().getDataStatus())
                .isEqualTo("OK");
    }
}
