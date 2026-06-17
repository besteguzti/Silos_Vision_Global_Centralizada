package com.tfg.dashboard.dto;

import java.util.List;

public class OperationalImpactAnalysisResponse {

    private List<TechnicalPlatformRelationDto> technicalRelations;
    private List<TimelinePointDto> technicalTimeline;
    private List<KpiRelationDto> specificKpiRelations;

    public List<TechnicalPlatformRelationDto> getTechnicalRelations() {
        return technicalRelations;
    }

    public List<TimelinePointDto> getTechnicalTimeline() {
        return technicalTimeline;
    }

    public List<KpiRelationDto> getSpecificKpiRelations() {
        return specificKpiRelations;
    }

    public void setTechnicalRelations(List<TechnicalPlatformRelationDto> technicalRelations) {
        this.technicalRelations = technicalRelations;
    }

    public void setTechnicalTimeline(List<TimelinePointDto> technicalTimeline) {
        this.technicalTimeline = technicalTimeline;
    }

    public void setSpecificKpiRelations(List<KpiRelationDto> specificKpiRelations) {
        this.specificKpiRelations = specificKpiRelations;
    }
}
