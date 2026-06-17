package com.tfg.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ThresholdSectionDto {

    private String key;
    private String title;
    private String description;
    @Valid
    @NotNull
    private List<ThresholdValueDto> values = new ArrayList<>();

    public ThresholdSectionDto() {
    }

    public ThresholdSectionDto(
            String key,
            String title,
            String description,
            List<ThresholdValueDto> values) {

        this.key = key;
        this.title = title;
        this.description = description;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<ThresholdValueDto> getValues() {
        return values;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValues(List<ThresholdValueDto> values) {
        this.values = values;
    }
}
