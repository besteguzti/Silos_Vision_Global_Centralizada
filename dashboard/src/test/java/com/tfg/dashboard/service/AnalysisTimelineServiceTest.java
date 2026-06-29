package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tfg.dashboard.dto.TimelinePointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

class AnalysisTimelineServiceTest {

    private final AnalysisTimelineService service = new AnalysisTimelineService();

    @Test
    void aggregatesSnapshotsByDayAndKeepsTransversalIndicators() {

        AnalysisSnapshot firstMorning = snapshot(
                LocalDateTime.of(2026, 5, 1, 6, 0),
                10,
                20,
                30,
                40,
                50,
                60,
                70,
                false
        );
        AnalysisSnapshot firstAfternoon = snapshot(
                LocalDateTime.of(2026, 5, 1, 18, 0),
                30,
                40,
                50,
                60,
                70,
                80,
                90,
                true
        );
        AnalysisSnapshot secondDay = snapshot(
                LocalDateTime.of(2026, 5, 2, 12, 0),
                70,
                65,
                55,
                45,
                35,
                25,
                15,
                false
        );

        List<TimelinePointDto> result =
                service.buildPlatformEvolution(List.of(
                        firstAfternoon,
                        secondDay,
                        firstMorning
                ));

        assertThat(result).hasSize(2);

        TimelinePointDto firstDay = result.get(0);
        assertThat(firstDay.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(firstDay.getAruba()).isEqualTo(20.0);
        assertThat(firstDay.getCitrix()).isEqualTo(30.0);
        assertThat(firstDay.getMicrosoft365()).isEqualTo(40.0);
        assertThat(firstDay.getTechnicalDegradation()).isEqualTo(50.0);
        assertThat(firstDay.getGlpiOperationalPressure()).isEqualTo(60.0);
        assertThat(firstDay.getUserImpact()).isEqualTo(70.0);
        assertThat(firstDay.getGlobalStatus()).isEqualTo(80.0);
        assertThat(firstDay.isGeneratedScenario()).isTrue();

        TimelinePointDto lastDay = result.get(1);
        assertThat(lastDay.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 5, 2, 0, 0));
        assertThat(lastDay.getTechnicalDegradation()).isEqualTo(45.0);
        assertThat(lastDay.isGeneratedScenario()).isFalse();
    }

    private AnalysisSnapshot snapshot(
            LocalDateTime timestamp,
            Integer aruba,
            Integer citrix,
            Integer microsoft365,
            Integer technicalDegradation,
            Integer glpiOperationalPressure,
            Integer userImpact,
            Integer globalStatus,
            boolean generatedScenario
    ) {

        AnalysisSnapshot snapshot = new AnalysisSnapshot();
        snapshot.setTimestamp(timestamp);
        snapshot.setArubaHealth(aruba);
        snapshot.setCitrixHealth(citrix);
        snapshot.setMicrosoft365Health(microsoft365);
        snapshot.setTechnicalDegradation(technicalDegradation);
        snapshot.setGlpiOperationalPressure(glpiOperationalPressure);
        snapshot.setUserImpact(userImpact);
        snapshot.setGlobalStatus(globalStatus);
        snapshot.setGeneratedScenario(generatedScenario);
        return snapshot;
    }
}
