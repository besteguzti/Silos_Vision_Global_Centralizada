package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;

class KpiScoringServiceTest {

    private final KpiProperties kpiProperties =
            new KpiProperties();

    private final KpiScoringService service =
            new KpiScoringService(kpiProperties);

    @Test
    void statusFromAffectionUsesCommonThresholds() {

        assertThat(service.statusFromAffection(33))
                .isEqualTo("GREEN");
        assertThat(service.statusFromAffection(34))
                .isEqualTo("YELLOW");
        assertThat(service.statusFromAffection(67))
                .isEqualTo("RED");
    }

    @Test
    void clampToIntLimitsValuesBetweenZeroAndOneHundred() {

        assertThat(service.clampToInt(-10))
                .isZero();
        assertThat(service.clampToInt(42.4))
                .isEqualTo(42);
        assertThat(service.clampToInt(150))
                .isEqualTo(100);
    }

    @Test
    void staleFreshnessDoesNotKeepGreenStatus() {

        assertThat(service.statusFromFreshness("STALE", "GREEN"))
                .isEqualTo("YELLOW");
        assertThat(service.statusFromFreshness("NO_DATA", "GREEN"))
                .isEqualTo("NO_DATA");
    }

    @Test
    void changingGlobalStatusThresholdDoesNotChangeCriticalityThreshold() {

        kpiProperties.getTransversal().getGlobalStatus().setYellowMin(80);
        kpiProperties.getTransversal().getGlobalStatus().setRedMin(90);

        assertThat(service.statusFromTransversalKpi("transversal.globalStatus", 50))
                .isEqualTo("GREEN");
        assertThat(service.statusFromTransversalKpi("transversal.globalCriticality", 50))
                .isEqualTo("YELLOW");
    }

    @Test
    void changingCriticalityThresholdDoesNotChangeGlobalStatusThreshold() {

        kpiProperties.getTransversal().getGlobalCriticality().setYellowMin(80);
        kpiProperties.getTransversal().getGlobalCriticality().setRedMin(90);

        assertThat(service.statusFromTransversalKpi("transversal.globalCriticality", 50))
                .isEqualTo("GREEN");
        assertThat(service.statusFromTransversalKpi("transversal.globalStatus", 50))
                .isEqualTo("YELLOW");
    }

    @Test
    void healthTransversalKpiTreatsZeroAsRed() {

        assertThat(service.statusFromTransversalKpi("transversal.globalAvailability", 0))
                .isEqualTo("RED");
        assertThat(service.statusFromTransversalKpi("transversal.globalAvailability", 50))
                .isEqualTo("YELLOW");
        assertThat(service.statusFromTransversalKpi("transversal.globalAvailability", 67))
                .isEqualTo("GREEN");
    }

    @Test
    void riskTransversalKpiTreatsZeroAsGreen() {

        assertThat(service.statusFromTransversalKpi("transversal.globalStatus", 0))
                .isEqualTo("GREEN");
        assertThat(service.statusFromTransversalKpi("transversal.operationalPressure", 0))
                .isEqualTo("GREEN");
    }
}
