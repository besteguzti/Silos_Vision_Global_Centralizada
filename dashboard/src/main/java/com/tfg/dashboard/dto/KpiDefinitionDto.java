package com.tfg.dashboard.dto;

import java.util.List;

public class KpiDefinitionDto {

    private String id;
    private String name;
    private String type;
    private String platform;
    private String description;
    private String formula;
    private KpiThresholdDto thresholds;
    private List<String> sources;

    public KpiDefinitionDto() {
    }

    public KpiDefinitionDto(
            String id,
            String name,
            String type,
            String platform,
            String description,
            String formula,
            KpiThresholdDto thresholds,
            List<String> sources
    ) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.platform = platform;
        this.description = description;
        this.formula = formula;
        this.thresholds = thresholds;
        this.sources = sources;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDescription() {
        return description;
    }

    public String getFormula() {
        return formula;
    }

    public KpiThresholdDto getThresholds() {
        return thresholds;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public void setThresholds(KpiThresholdDto thresholds) {
        this.thresholds = thresholds;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }
}
