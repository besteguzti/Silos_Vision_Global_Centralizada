package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {
        ManualSyncService.class,
        ManualSyncServiceSpringContextTest.TestConfig.class
})
class ManualSyncServiceSpringContextTest {

    @Autowired
    private ManualSyncService manualSyncService;

    @Test
    void loadsManualSyncServiceInSpringContext() {
        assertThat(manualSyncService).isNotNull();
    }

    @Configuration
    static class TestConfig {

        @Bean
        MetricsSyncService metricsSyncService() {
            return mock(MetricsSyncService.class);
        }

        @Bean
        ArubaService arubaService() {
            return mock(ArubaService.class);
        }

        @Bean
        SynchronizationControlService synchronizationControlService() {
            return mock(SynchronizationControlService.class);
        }
    }
}
