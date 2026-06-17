package com.tfg.dashboard.dto;

import com.tfg.dashboard.dto.summary.MainDashboardSummary;

public class TestScenarioEvaluationResponse {

    private MainDashboardSummary summary;
    private PlatformStatus platformStatus;
    private ExecutiveSummaryDto operationalSummary;

    public TestScenarioEvaluationResponse() {
    }

    public MainDashboardSummary getSummary() {
        return summary;
    }

    public void setSummary(MainDashboardSummary summary) {
        this.summary = summary;
    }

    public PlatformStatus getPlatformStatus() {
        return platformStatus;
    }

    public void setPlatformStatus(PlatformStatus platformStatus) {
        this.platformStatus = platformStatus;
    }

    public ExecutiveSummaryDto getOperationalSummary() {
        return operationalSummary;
    }

    public void setOperationalSummary(ExecutiveSummaryDto operationalSummary) {
        this.operationalSummary = operationalSummary;
    }

    public static class PlatformStatus {

        private String aruba;
        private String citrix;
        private String microsoft365;
        private String glpi;

        public PlatformStatus() {
        }

        public String getAruba() {
            return aruba;
        }

        public void setAruba(String aruba) {
            this.aruba = aruba;
        }

        public String getCitrix() {
            return citrix;
        }

        public void setCitrix(String citrix) {
            this.citrix = citrix;
        }

        public String getMicrosoft365() {
            return microsoft365;
        }

        public void setMicrosoft365(String microsoft365) {
            this.microsoft365 = microsoft365;
        }

        public String getGlpi() {
            return glpi;
        }

        public void setGlpi(String glpi) {
            this.glpi = glpi;
        }
    }
}
