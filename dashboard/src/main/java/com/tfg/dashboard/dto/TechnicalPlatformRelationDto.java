package com.tfg.dashboard.dto;

public class TechnicalPlatformRelationDto {

    private String relation;
    private String origin;
    private String target;
    private Integer cooccurrencePercentage;
    private Integer averageIncrease;
    private String reading;
    private String readingStatus;
    private int originAffectedSnapshots;
    private int originNormalSnapshots;

    public String getRelation() {
        return relation;
    }

    public String getOrigin() {
        return origin;
    }

    public String getTarget() {
        return target;
    }

    public Integer getCooccurrencePercentage() {
        return cooccurrencePercentage;
    }

    public Integer getAverageIncrease() {
        return averageIncrease;
    }

    public String getReading() {
        return reading;
    }

    public String getReadingStatus() {
        return readingStatus;
    }

    public int getOriginAffectedSnapshots() {
        return originAffectedSnapshots;
    }

    public int getOriginNormalSnapshots() {
        return originNormalSnapshots;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setCooccurrencePercentage(Integer cooccurrencePercentage) {
        this.cooccurrencePercentage = cooccurrencePercentage;
    }

    public void setAverageIncrease(Integer averageIncrease) {
        this.averageIncrease = averageIncrease;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public void setReadingStatus(String readingStatus) {
        this.readingStatus = readingStatus;
    }

    public void setOriginAffectedSnapshots(int originAffectedSnapshots) {
        this.originAffectedSnapshots = originAffectedSnapshots;
    }

    public void setOriginNormalSnapshots(int originNormalSnapshots) {
        this.originNormalSnapshots = originNormalSnapshots;
    }
}
