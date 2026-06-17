package com.tfg.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ThresholdValueDto {

    @NotBlank
    private String key;
    private String label;
    @NotNull
    @PositiveOrZero
    private Integer value;
    private Integer defaultValue;
    private String unit;
    private String description;

    public ThresholdValueDto() {
    }

    public ThresholdValueDto(
            String key,
            String label,
            Integer value,
            Integer defaultValue,
            String unit,
            String description) {

        this.key = key;
        this.label = label;
        this.value = value;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public Integer getValue() {
        return value;
    }

    public Integer getDefaultValue() {
        return defaultValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setDefaultValue(Integer defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
