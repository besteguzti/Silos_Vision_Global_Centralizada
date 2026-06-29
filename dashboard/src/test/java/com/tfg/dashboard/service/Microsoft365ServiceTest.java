package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.summary.Microsoft365Summary;
import com.tfg.dashboard.repository.Microsoft365MetricsHistoryRepository;

@ExtendWith(MockitoExtension.class)
class Microsoft365ServiceTest {

    @Mock
    private Microsoft365MetricsHistoryRepository metricsHistoryRepository;

    @Mock
    private GlpiPlatformTicketService glpiPlatformTicketService;

    @Test
    void simulatedIdentitySignalsStayWithinConfiguredRanges() {
        when(glpiPlatformTicketService.getMicrosoft365OpenTickets())
                .thenReturn(0);

        Microsoft365Service service =
                new Microsoft365Service(
                        metricsHistoryRepository,
                        glpiPlatformTicketService,
                        new KpiProperties());

        for (int attempt = 0; attempt < 200; attempt++) {
            Microsoft365Summary summary =
                    service.generateSimulatedSummary();

            assertThat(summary.getRiskyUsers())
                    .isBetween(0, 20);
            assertThat(summary.getFailedSignIns())
                    .isBetween(0, 30);
            assertThat(summary.getUsersWithoutMfa())
                    .isBetween(0, 10)
                    .isLessThanOrEqualTo(summary.getActiveUsers());
        }
    }
}
