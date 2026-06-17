package com.tfg.dashboard.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración común para pesos y umbrales de KPIs.
 */
@Component
@ConfigurationProperties(prefix = "kpi")
public class KpiProperties {

        private Status status = new Status();
        private Affection affection = new Affection();
        private Weights weights = new Weights();
        private Citrix citrix = new Citrix();
        private Microsoft365 microsoft365 = new Microsoft365();
        private Glpi glpi = new Glpi();
        private Aruba aruba = new Aruba();
        private Freshness freshness = new Freshness();
        private Transversal transversal = new Transversal();
        private Executive executive = new Executive();

        public Status getStatus() {
                return status;
        }

        public void setStatus(Status status) {
                this.status = status;
        }

        public Affection getAffection() {
                return affection;
        }

        public void setAffection(Affection affection) {
                this.affection = affection;
        }

        public Weights getWeights() {
                return weights;
        }

        public void setWeights(Weights weights) {
                this.weights = weights;
        }

        public Citrix getCitrix() {
                return citrix;
        }

        public void setCitrix(Citrix citrix) {
                this.citrix = citrix;
        }

        public Microsoft365 getMicrosoft365() {
                return microsoft365;
        }

        public void setMicrosoft365(Microsoft365 microsoft365) {
                this.microsoft365 = microsoft365;
        }

        public Glpi getGlpi() {
                return glpi;
        }

        public void setGlpi(Glpi glpi) {
                this.glpi = glpi;
        }

        public Aruba getAruba() {
                return aruba;
        }

        public void setAruba(Aruba aruba) {
                this.aruba = aruba;
        }

        public Freshness getFreshness() {
                return freshness;
        }

        public void setFreshness(Freshness freshness) {
                this.freshness = freshness;
        }

        public Transversal getTransversal() {
                return transversal;
        }

        public void setTransversal(Transversal transversal) {
                this.transversal = transversal;
        }

        public Executive getExecutive() {
                return executive;
        }

        public void setExecutive(Executive executive) {
                this.executive = executive;
        }

        public int asWeightPercent(double weight) {

                return (int) Math.round(weight * 100);
        }

        public String formatWeight(double weight) {

                return String.format(java.util.Locale.US, "%.2f", weight);
        }

        public static class Status {

                private int yellowMin = 34;
                private int redMin = 67;
                private int max = 100;

                public int getYellowMin() {
                        return yellowMin;
                }

                public void setYellowMin(int yellowMin) {
                        this.yellowMin = yellowMin;
                }

                public int getRedMin() {
                        return redMin;
                }

                public void setRedMin(int redMin) {
                        this.redMin = redMin;
                }

                public int getMax() {
                        return max;
                }

                public void setMax(int max) {
                        this.max = max;
                }
        }

        public static class Affection {

                private int green = 0;
                private int yellow = 50;
                private int red = 100;

                public int getGreen() {
                        return green;
                }

                public void setGreen(int green) {
                        this.green = green;
                }

                public int getYellow() {
                        return yellow;
                }

                public void setYellow(int yellow) {
                        this.yellow = yellow;
                }

                public int getRed() {
                        return red;
                }

                public void setRed(int red) {
                        this.red = red;
                }
        }

        public static class Freshness {

                private int arubaMinutes = 70;
                private int citrixMinutes = 70;
                private int microsoft365Minutes = 70;
                private int glpiMinutes = 70;

                public int getArubaMinutes() {
                        return arubaMinutes;
                }

                public void setArubaMinutes(int arubaMinutes) {
                        this.arubaMinutes = arubaMinutes;
                }

                public int getCitrixMinutes() {
                        return citrixMinutes;
                }

                public void setCitrixMinutes(int citrixMinutes) {
                        this.citrixMinutes = citrixMinutes;
                }

                public int getMicrosoft365Minutes() {
                        return microsoft365Minutes;
                }

                public void setMicrosoft365Minutes(int microsoft365Minutes) {
                        this.microsoft365Minutes = microsoft365Minutes;
                }

                public int getGlpiMinutes() {
                        return glpiMinutes;
                }

                public void setGlpiMinutes(int glpiMinutes) {
                        this.glpiMinutes = glpiMinutes;
                }
        }

        public static class Weights {

                private PlatformWeights globalStatus = new PlatformWeights(0.40, 0.30, 0.20, 0.10);
                private PlatformWeights availability = new PlatformWeights(0.45, 0.35, 0.15, 0.05);
                private PlatformWeights operationalPressure = new PlatformWeights(0.10, 0.20, 0.20, 0.50);
                private PlatformWeights technicalDegradation = new PlatformWeights(0.30, 0.30, 0.30, 0.10);
                private PlatformWeights slaRisk = new PlatformWeights(0.30, 0.35, 0.10, 0.25);
                private PlatformWeights operationalBacklog = new PlatformWeights(0.10, 0.05, 0.15, 0.70);
                private PlatformWeights userImpact = new PlatformWeights(0.35, 0.35, 0.20, 0.10);
                private PlatformWeights analysisTechnicalDegradation = new PlatformWeights(0.35, 0.35, 0.30, 0.0);
                private PlatformWeights analysisUserImpact = new PlatformWeights(0.30, 0.35, 0.20, 0.15);
                private GlpiPressureWeights glpiOperationalPressure =
                                new GlpiPressureWeights(0.40, 0.30, 0.20, 0.10);

                public PlatformWeights getGlobalStatus() {
                        return globalStatus;
                }

                public void setGlobalStatus(PlatformWeights globalStatus) {
                        this.globalStatus = globalStatus;
                }

                public PlatformWeights getAvailability() {
                        return availability;
                }

                public void setAvailability(PlatformWeights availability) {
                        this.availability = availability;
                }

                public PlatformWeights getOperationalPressure() {
                        return operationalPressure;
                }

                public void setOperationalPressure(PlatformWeights operationalPressure) {
                        this.operationalPressure = operationalPressure;
                }

                public PlatformWeights getTechnicalDegradation() {
                        return technicalDegradation;
                }

                public void setTechnicalDegradation(PlatformWeights technicalDegradation) {
                        this.technicalDegradation = technicalDegradation;
                }

                public PlatformWeights getSlaRisk() {
                        return slaRisk;
                }

                public void setSlaRisk(PlatformWeights slaRisk) {
                        this.slaRisk = slaRisk;
                }

                public PlatformWeights getOperationalBacklog() {
                        return operationalBacklog;
                }

                public void setOperationalBacklog(PlatformWeights operationalBacklog) {
                        this.operationalBacklog = operationalBacklog;
                }

                public PlatformWeights getUserImpact() {
                        return userImpact;
                }

                public void setUserImpact(PlatformWeights userImpact) {
                        this.userImpact = userImpact;
                }

                public PlatformWeights getAnalysisTechnicalDegradation() {
                        return analysisTechnicalDegradation;
                }

                public void setAnalysisTechnicalDegradation(PlatformWeights analysisTechnicalDegradation) {
                        this.analysisTechnicalDegradation = analysisTechnicalDegradation;
                }

                public PlatformWeights getAnalysisUserImpact() {
                        return analysisUserImpact;
                }

                public void setAnalysisUserImpact(PlatformWeights analysisUserImpact) {
                        this.analysisUserImpact = analysisUserImpact;
                }

                public GlpiPressureWeights getGlpiOperationalPressure() {
                        return glpiOperationalPressure;
                }

                public void setGlpiOperationalPressure(GlpiPressureWeights glpiOperationalPressure) {
                        this.glpiOperationalPressure = glpiOperationalPressure;
                }
        }

        public static class PlatformWeights {

                private double aruba;
                private double citrix;
                private double microsoft365;
                private double glpi;

                public PlatformWeights() {
                }

                public PlatformWeights(double aruba,double citrix,double microsoft365,double glpi) {
                        this.aruba = aruba;
                        this.citrix = citrix;
                        this.microsoft365 = microsoft365;
                        this.glpi = glpi;
                }

                public double getAruba() {
                        return aruba;
                }

                public void setAruba(double aruba) {
                        this.aruba = aruba;
                }

                public double getCitrix() {
                        return citrix;
                }

                public void setCitrix(double citrix) {
                        this.citrix = citrix;
                }

                public double getMicrosoft365() {
                        return microsoft365;
                }

                public void setMicrosoft365(double microsoft365) {
                        this.microsoft365 = microsoft365;
                }

                public double getGlpi() {
                        return glpi;
                }

                public void setGlpi(double glpi) {
                        this.glpi = glpi;
                }
        }

        public static class GlpiPressureWeights {

                private double openTickets;
                private double closedTodayPercent;
                private double criticalTickets;
                private double closedWeekPercent;

                public GlpiPressureWeights() {
                }

                public GlpiPressureWeights(
                                double openTickets,
                                double closedTodayPercent,
                                double criticalTickets,
                                double closedWeekPercent) {
                        this.openTickets = openTickets;
                        this.closedTodayPercent = closedTodayPercent;
                        this.criticalTickets = criticalTickets;
                        this.closedWeekPercent = closedWeekPercent;
                }

                public double getOpenTickets() {
                        return openTickets;
                }

                public void setOpenTickets(double openTickets) {
                        this.openTickets = openTickets;
                }

                public double getClosedTodayPercent() {
                        return closedTodayPercent;
                }

                public void setClosedTodayPercent(double closedTodayPercent) {
                        this.closedTodayPercent = closedTodayPercent;
                }

                public double getCriticalTickets() {
                        return criticalTickets;
                }

                public void setCriticalTickets(double criticalTickets) {
                        this.criticalTickets = criticalTickets;
                }

                public double getClosedWeekPercent() {
                        return closedWeekPercent;
                }

                public void setClosedWeekPercent(double closedWeekPercent) {
                        this.closedWeekPercent = closedWeekPercent;
                }
        }

        public static class Citrix {

                private int deliveryControllerYellowBelowPercent = 67;
                private int logonDurationYellowAboveSeconds = 20;
                private int logonDurationRedAboveSeconds = 60;
                private int serverLoadYellowMin = 80;
                private int serverLoadRedMin = 90;
                private int failedLogonsYellowAbove = 5;
                private int failedLogonsRedAbove = 20;
                private int activeLicensesYellowBelow = 20;
                private int activeLicensesRedBelowOrEqual = 2;
                private int citrixOpenTicketsYellowMin = 100;
                private int citrixOpenTicketsRedMin = 200;

                public int getDeliveryControllerYellowBelowPercent() {
                        return deliveryControllerYellowBelowPercent;
                }

                public void setDeliveryControllerYellowBelowPercent(int deliveryControllerYellowBelowPercent) {
                        this.deliveryControllerYellowBelowPercent = deliveryControllerYellowBelowPercent;
                }

                public int getLogonDurationYellowAboveSeconds() {
                        return logonDurationYellowAboveSeconds;
                }

                public void setLogonDurationYellowAboveSeconds(int logonDurationYellowAboveSeconds) {
                        this.logonDurationYellowAboveSeconds = logonDurationYellowAboveSeconds;
                }

                public int getLogonDurationRedAboveSeconds() {
                        return logonDurationRedAboveSeconds;
                }

                public void setLogonDurationRedAboveSeconds(int logonDurationRedAboveSeconds) {
                        this.logonDurationRedAboveSeconds = logonDurationRedAboveSeconds;
                }

                public int getServerLoadYellowMin() {
                        return serverLoadYellowMin;
                }

                public void setServerLoadYellowMin(int serverLoadYellowMin) {
                        this.serverLoadYellowMin = serverLoadYellowMin;
                }

                public int getServerLoadRedMin() {
                        return serverLoadRedMin;
                }

                public void setServerLoadRedMin(int serverLoadRedMin) {
                        this.serverLoadRedMin = serverLoadRedMin;
                }

                public int getFailedLogonsYellowAbove() {
                        return failedLogonsYellowAbove;
                }

                public void setFailedLogonsYellowAbove(int failedLogonsYellowAbove) {
                        this.failedLogonsYellowAbove = failedLogonsYellowAbove;
                }

                public int getFailedLogonsRedAbove() {
                        return failedLogonsRedAbove;
                }

                public void setFailedLogonsRedAbove(int failedLogonsRedAbove) {
                        this.failedLogonsRedAbove = failedLogonsRedAbove;
                }

                public int getActiveLicensesYellowBelow() {
                        return activeLicensesYellowBelow;
                }

                public void setActiveLicensesYellowBelow(int activeLicensesYellowBelow) {
                        this.activeLicensesYellowBelow = activeLicensesYellowBelow;
                }

                public int getActiveLicensesRedBelowOrEqual() {
                        return activeLicensesRedBelowOrEqual;
                }

                public void setActiveLicensesRedBelowOrEqual(int activeLicensesRedBelowOrEqual) {
                        this.activeLicensesRedBelowOrEqual = activeLicensesRedBelowOrEqual;
                }

                public int getCitrixOpenTicketsYellowMin() {
                        return citrixOpenTicketsYellowMin;
                }

                public void setCitrixOpenTicketsYellowMin(int citrixOpenTicketsYellowMin) {
                        this.citrixOpenTicketsYellowMin = citrixOpenTicketsYellowMin;
                }

                public int getCitrixOpenTicketsRedMin() {
                        return citrixOpenTicketsRedMin;
                }

                public void setCitrixOpenTicketsRedMin(int citrixOpenTicketsRedMin) {
                        this.citrixOpenTicketsRedMin = citrixOpenTicketsRedMin;
                }
        }

        public static class Microsoft365 {

                private int unassignedLicensesYellowBelow = 20;
                private int unassignedLicensesRedBelowOrEqual = 2;
                private int sharePointYellowMin = 80;
                private int sharePointRedAbove = 90;
                private int riskyUsersYellowAbove = 0;
                private int riskyUsersRedAbove = 9;
                private int failedSignInsYellowMin = 10;
                private int failedSignInsRedMin = 20;
                private int usersWithoutMfaYellowAbove = 0;
                private int usersWithoutMfaRedAbove = 4;
                private int secretsYellowAbove = 0;
                private int unusedApplicationsYellowAbove = 0;
                private int highPrivilegeApplicationsYellowAbove = 0;
                private int nonCompliantDevicesYellowAbove = 30;
                private int nonCompliantDevicesRedAbove = 50;
                private int microsoft365OpenTicketsYellowMin = 100;
                private int microsoft365OpenTicketsRedMin = 200;
                private int outdatedWindowsYellowAbove = 0;
                private int devicesWithoutEncryptionYellowAbove = 0;
                private int devicesWithoutEncryptionRedAbove = 0;
                private int staleDevicesRedAbove = 0;

                public int getUnassignedLicensesYellowBelow() {
                        return unassignedLicensesYellowBelow;
                }

                public void setUnassignedLicensesYellowBelow(int unassignedLicensesYellowBelow) {
                        this.unassignedLicensesYellowBelow = unassignedLicensesYellowBelow;
                }

                public int getUnassignedLicensesRedBelowOrEqual() {
                        return unassignedLicensesRedBelowOrEqual;
                }

                public void setUnassignedLicensesRedBelowOrEqual(int unassignedLicensesRedBelowOrEqual) {
                        this.unassignedLicensesRedBelowOrEqual = unassignedLicensesRedBelowOrEqual;
                }

                public int getSharePointYellowMin() {
                        return sharePointYellowMin;
                }

                public void setSharePointYellowMin(int sharePointYellowMin) {
                        this.sharePointYellowMin = sharePointYellowMin;
                }

                public int getSharePointRedAbove() {
                        return sharePointRedAbove;
                }

                public void setSharePointRedAbove(int sharePointRedAbove) {
                        this.sharePointRedAbove = sharePointRedAbove;
                }

                public int getRiskyUsersYellowAbove() {
                        return riskyUsersYellowAbove;
                }

                public void setRiskyUsersYellowAbove(int riskyUsersYellowAbove) {
                        this.riskyUsersYellowAbove = riskyUsersYellowAbove;
                }

                public int getRiskyUsersRedAbove() {
                        return riskyUsersRedAbove;
                }

                public void setRiskyUsersRedAbove(int riskyUsersRedAbove) {
                        this.riskyUsersRedAbove = riskyUsersRedAbove;
                }

                public int getFailedSignInsYellowMin() {
                        return failedSignInsYellowMin;
                }

                public void setFailedSignInsYellowMin(int failedSignInsYellowMin) {
                        this.failedSignInsYellowMin = failedSignInsYellowMin;
                }

                public int getFailedSignInsRedMin() {
                        return failedSignInsRedMin;
                }

                public void setFailedSignInsRedMin(int failedSignInsRedMin) {
                        this.failedSignInsRedMin = failedSignInsRedMin;
                }

                public int getUsersWithoutMfaYellowAbove() {
                        return usersWithoutMfaYellowAbove;
                }

                public void setUsersWithoutMfaYellowAbove(int usersWithoutMfaYellowAbove) {
                        this.usersWithoutMfaYellowAbove = usersWithoutMfaYellowAbove;
                }

                public int getUsersWithoutMfaRedAbove() {
                        return usersWithoutMfaRedAbove;
                }

                public void setUsersWithoutMfaRedAbove(int usersWithoutMfaRedAbove) {
                        this.usersWithoutMfaRedAbove = usersWithoutMfaRedAbove;
                }

                public int getSecretsYellowAbove() {
                        return secretsYellowAbove;
                }

                public void setSecretsYellowAbove(int secretsYellowAbove) {
                        this.secretsYellowAbove = secretsYellowAbove;
                }

                public int getUnusedApplicationsYellowAbove() {
                        return unusedApplicationsYellowAbove;
                }

                public void setUnusedApplicationsYellowAbove(int unusedApplicationsYellowAbove) {
                        this.unusedApplicationsYellowAbove = unusedApplicationsYellowAbove;
                }

                public int getHighPrivilegeApplicationsYellowAbove() {
                        return highPrivilegeApplicationsYellowAbove;
                }

                public void setHighPrivilegeApplicationsYellowAbove(int highPrivilegeApplicationsYellowAbove) {
                        this.highPrivilegeApplicationsYellowAbove = highPrivilegeApplicationsYellowAbove;
                }

                public int getNonCompliantDevicesYellowAbove() {
                        return nonCompliantDevicesYellowAbove;
                }

                public void setNonCompliantDevicesYellowAbove(int nonCompliantDevicesYellowAbove) {
                        this.nonCompliantDevicesYellowAbove = nonCompliantDevicesYellowAbove;
                }

                public int getNonCompliantDevicesRedAbove() {
                        return nonCompliantDevicesRedAbove;
                }

                public void setNonCompliantDevicesRedAbove(int nonCompliantDevicesRedAbove) {
                        this.nonCompliantDevicesRedAbove = nonCompliantDevicesRedAbove;
                }

                public int getMicrosoft365OpenTicketsYellowMin() {
                        return microsoft365OpenTicketsYellowMin;
                }

                public void setMicrosoft365OpenTicketsYellowMin(int microsoft365OpenTicketsYellowMin) {
                        this.microsoft365OpenTicketsYellowMin = microsoft365OpenTicketsYellowMin;
                }

                public int getMicrosoft365OpenTicketsRedMin() {
                        return microsoft365OpenTicketsRedMin;
                }

                public void setMicrosoft365OpenTicketsRedMin(int microsoft365OpenTicketsRedMin) {
                        this.microsoft365OpenTicketsRedMin = microsoft365OpenTicketsRedMin;
                }

                public int getOutdatedWindowsYellowAbove() {
                        return outdatedWindowsYellowAbove;
                }

                public void setOutdatedWindowsYellowAbove(int outdatedWindowsYellowAbove) {
                        this.outdatedWindowsYellowAbove = outdatedWindowsYellowAbove;
                }

                public int getDevicesWithoutEncryptionRedAbove() {
                        return devicesWithoutEncryptionRedAbove;
                }

                public int getDevicesWithoutEncryptionYellowAbove() {
                        return devicesWithoutEncryptionYellowAbove;
                }

                public void setDevicesWithoutEncryptionYellowAbove(int devicesWithoutEncryptionYellowAbove) {
                        this.devicesWithoutEncryptionYellowAbove = devicesWithoutEncryptionYellowAbove;
                }

                public void setDevicesWithoutEncryptionRedAbove(int devicesWithoutEncryptionRedAbove) {
                        this.devicesWithoutEncryptionRedAbove = devicesWithoutEncryptionRedAbove;
                }

                public int getStaleDevicesRedAbove() {
                        return staleDevicesRedAbove;
                }

                public void setStaleDevicesRedAbove(int staleDevicesRedAbove) {
                        this.staleDevicesRedAbove = staleDevicesRedAbove;
                }
        }

        public static class Glpi {

                private int openTicketsYellowMin = 101;
                private int openTicketsRedMin = 201;
                private int criticalTicketsYellowAbove = 0;
                private int criticalTicketsRedAbove = 10;
                private int slaBreachedTicketsYellowAbove = 0;
                private int slaBreachedTicketsRedAbove = 10;
                private int closedPercentGreenMin = 50;

                public int getOpenTicketsYellowMin() {
                        return openTicketsYellowMin;
                }

                public void setOpenTicketsYellowMin(int openTicketsYellowMin) {
                        this.openTicketsYellowMin = openTicketsYellowMin;
                }

                public int getOpenTicketsRedMin() {
                        return openTicketsRedMin;
                }

                public void setOpenTicketsRedMin(int openTicketsRedMin) {
                        this.openTicketsRedMin = openTicketsRedMin;
                }

                public int getCriticalTicketsYellowAbove() {
                        return criticalTicketsYellowAbove;
                }

                public void setCriticalTicketsYellowAbove(int criticalTicketsYellowAbove) {
                        this.criticalTicketsYellowAbove = criticalTicketsYellowAbove;
                }

                public int getCriticalTicketsRedAbove() {
                        return criticalTicketsRedAbove;
                }

                public void setCriticalTicketsRedAbove(int criticalTicketsRedAbove) {
                        this.criticalTicketsRedAbove = criticalTicketsRedAbove;
                }

                public int getSlaBreachedTicketsYellowAbove() {
                        return slaBreachedTicketsYellowAbove;
                }

                public void setSlaBreachedTicketsYellowAbove(int slaBreachedTicketsYellowAbove) {
                        this.slaBreachedTicketsYellowAbove = slaBreachedTicketsYellowAbove;
                }

                public int getSlaBreachedTicketsRedAbove() {
                        return slaBreachedTicketsRedAbove;
                }

                public void setSlaBreachedTicketsRedAbove(int slaBreachedTicketsRedAbove) {
                        this.slaBreachedTicketsRedAbove = slaBreachedTicketsRedAbove;
                }

                public int getClosedPercentGreenMin() {
                        return closedPercentGreenMin;
                }

                public void setClosedPercentGreenMin(int closedPercentGreenMin) {
                        this.closedPercentGreenMin = closedPercentGreenMin;
                }
        }

        public static class Aruba {

                private int freshnessMinutes = 70;
                private int underusedSwitchDownInterfaceLimit = 17;
                private int underusedSwitchDays = 30;
                private int inactiveApDaysThreshold = 30;
                private int arubaOpenTicketsYellowMin = 100;
                private int arubaOpenTicketsRedMin = 200;
                private int arubaOpenTicketsYellowAffection = 2;
                private int arubaOpenTicketsRedAffection = 5;
                private int accessPointDownYellowPercent = 50;
                private int accessPointDownRedPercent = 100;
                private int accessPointDownYellowAffection = 20;
                private int accessPointDownRedAffection = 80;
                private int switchDownYellowAbove = 1;
                private int switchDownYellowAffection = 20;
                private int switchDownRedAffection = 80;
                private int switchUpgradeYellowMin = 1;
                private int switchUpgradeYellowAffection = 2;
                private int pendingFirmwareApsYellowMin = 1;
                private int pendingFirmwareApsYellowAffection = 2;
                private int inactiveApsYellowMin = 1;
                private int inactiveApsYellowAffection = 10;
                private int criticalClientsGreenAbove = 0;
                private int totalWifiClientsRedAffection = 20;
                private int underusedSwitchesYellowAbove = 1;
                private int underusedSwitchesRedAbove = 5;
                private int underusedSwitchesYellowAffection = 2;
                private int underusedSwitchesRedAffection = 5;

                public int getFreshnessMinutes() {
                        return freshnessMinutes;
                }

                public void setFreshnessMinutes(int freshnessMinutes) {
                        this.freshnessMinutes = freshnessMinutes;
                }

                public int getUnderusedSwitchDownInterfaceLimit() {
                        return underusedSwitchDownInterfaceLimit;
                }

                public void setUnderusedSwitchDownInterfaceLimit(int underusedSwitchDownInterfaceLimit) {
                        this.underusedSwitchDownInterfaceLimit = underusedSwitchDownInterfaceLimit;
                }

                public int getUnderusedSwitchDays() {
                        return underusedSwitchDays;
                }

                public void setUnderusedSwitchDays(int underusedSwitchDays) {
                        this.underusedSwitchDays = underusedSwitchDays;
                }

                public int getInactiveApDaysThreshold() {
                        return inactiveApDaysThreshold;
                }

                public void setInactiveApDaysThreshold(int inactiveApDaysThreshold) {
                        this.inactiveApDaysThreshold = inactiveApDaysThreshold;
                }

                public int getArubaOpenTicketsYellowMin() {
                        return arubaOpenTicketsYellowMin;
                }

                public void setArubaOpenTicketsYellowMin(int arubaOpenTicketsYellowMin) {
                        this.arubaOpenTicketsYellowMin = arubaOpenTicketsYellowMin;
                }

                public int getArubaOpenTicketsRedMin() {
                        return arubaOpenTicketsRedMin;
                }

                public void setArubaOpenTicketsRedMin(int arubaOpenTicketsRedMin) {
                        this.arubaOpenTicketsRedMin = arubaOpenTicketsRedMin;
                }

                public int getArubaOpenTicketsYellowAffection() {
                        return arubaOpenTicketsYellowAffection;
                }

                public void setArubaOpenTicketsYellowAffection(int arubaOpenTicketsYellowAffection) {
                        this.arubaOpenTicketsYellowAffection = arubaOpenTicketsYellowAffection;
                }

                public int getArubaOpenTicketsRedAffection() {
                        return arubaOpenTicketsRedAffection;
                }

                public void setArubaOpenTicketsRedAffection(int arubaOpenTicketsRedAffection) {
                        this.arubaOpenTicketsRedAffection = arubaOpenTicketsRedAffection;
                }

                public int getAccessPointDownRedPercent() {
                        return accessPointDownRedPercent;
                }

                public int getAccessPointDownYellowPercent() {
                        return accessPointDownYellowPercent;
                }

                public void setAccessPointDownYellowPercent(int accessPointDownYellowPercent) {
                        this.accessPointDownYellowPercent = accessPointDownYellowPercent;
                }

                public void setAccessPointDownRedPercent(int accessPointDownRedPercent) {
                        this.accessPointDownRedPercent = accessPointDownRedPercent;
                }

                public int getAccessPointDownYellowAffection() {
                        return accessPointDownYellowAffection;
                }

                public void setAccessPointDownYellowAffection(int accessPointDownYellowAffection) {
                        this.accessPointDownYellowAffection = accessPointDownYellowAffection;
                }

                public int getAccessPointDownRedAffection() {
                        return accessPointDownRedAffection;
                }

                public void setAccessPointDownRedAffection(int accessPointDownRedAffection) {
                        this.accessPointDownRedAffection = accessPointDownRedAffection;
                }

                public int getSwitchDownYellowAbove() {
                        return switchDownYellowAbove;
                }

                public void setSwitchDownYellowAbove(int switchDownYellowAbove) {
                        this.switchDownYellowAbove = switchDownYellowAbove;
                }

                public int getSwitchDownYellowAffection() {
                        return switchDownYellowAffection;
                }

                public void setSwitchDownYellowAffection(int switchDownYellowAffection) {
                        this.switchDownYellowAffection = switchDownYellowAffection;
                }

                public int getSwitchDownRedAffection() {
                        return switchDownRedAffection;
                }

                public void setSwitchDownRedAffection(int switchDownRedAffection) {
                        this.switchDownRedAffection = switchDownRedAffection;
                }

                public int getSwitchUpgradeYellowMin() {
                        return switchUpgradeYellowMin;
                }

                public void setSwitchUpgradeYellowMin(int switchUpgradeYellowMin) {
                        this.switchUpgradeYellowMin = switchUpgradeYellowMin;
                }

                public int getSwitchUpgradeYellowAffection() {
                        return switchUpgradeYellowAffection;
                }

                public void setSwitchUpgradeYellowAffection(int switchUpgradeYellowAffection) {
                        this.switchUpgradeYellowAffection = switchUpgradeYellowAffection;
                }

                public int getPendingFirmwareApsYellowMin() {
                        return pendingFirmwareApsYellowMin;
                }

                public void setPendingFirmwareApsYellowMin(int pendingFirmwareApsYellowMin) {
                        this.pendingFirmwareApsYellowMin = pendingFirmwareApsYellowMin;
                }

                public int getPendingFirmwareApsYellowAffection() {
                        return pendingFirmwareApsYellowAffection;
                }

                public void setPendingFirmwareApsYellowAffection(int pendingFirmwareApsYellowAffection) {
                        this.pendingFirmwareApsYellowAffection = pendingFirmwareApsYellowAffection;
                }

                public int getInactiveApsYellowMin() {
                        return inactiveApsYellowMin;
                }

                public void setInactiveApsYellowMin(int inactiveApsYellowMin) {
                        this.inactiveApsYellowMin = inactiveApsYellowMin;
                }

                public int getInactiveApsYellowAffection() {
                        return inactiveApsYellowAffection;
                }

                public void setInactiveApsYellowAffection(int inactiveApsYellowAffection) {
                        this.inactiveApsYellowAffection = inactiveApsYellowAffection;
                }

                public int getCriticalClientsGreenAbove() {
                        return criticalClientsGreenAbove;
                }

                public void setCriticalClientsGreenAbove(int criticalClientsGreenAbove) {
                        this.criticalClientsGreenAbove = criticalClientsGreenAbove;
                }

                public int getTotalWifiClientsRedAffection() {
                        return totalWifiClientsRedAffection;
                }

                public void setTotalWifiClientsRedAffection(int totalWifiClientsRedAffection) {
                        this.totalWifiClientsRedAffection = totalWifiClientsRedAffection;
                }

                public int getUnderusedSwitchesYellowAbove() {
                        return underusedSwitchesYellowAbove;
                }

                public void setUnderusedSwitchesYellowAbove(int underusedSwitchesYellowAbove) {
                        this.underusedSwitchesYellowAbove = underusedSwitchesYellowAbove;
                }

                public int getUnderusedSwitchesRedAbove() {
                        return underusedSwitchesRedAbove;
                }

                public void setUnderusedSwitchesRedAbove(int underusedSwitchesRedAbove) {
                        this.underusedSwitchesRedAbove = underusedSwitchesRedAbove;
                }

                public int getUnderusedSwitchesYellowAffection() {
                        return underusedSwitchesYellowAffection;
                }

                public void setUnderusedSwitchesYellowAffection(int underusedSwitchesYellowAffection) {
                        this.underusedSwitchesYellowAffection = underusedSwitchesYellowAffection;
                }

                public int getUnderusedSwitchesRedAffection() {
                        return underusedSwitchesRedAffection;
                }

                public void setUnderusedSwitchesRedAffection(int underusedSwitchesRedAffection) {
                        this.underusedSwitchesRedAffection = underusedSwitchesRedAffection;
                }

        }

        public static class Transversal {

                private TransversalKpiThreshold globalStatus = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold globalCriticality = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold globalAvailability = TransversalKpiThreshold.health();
                private TransversalKpiThreshold operationalPressure = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold technicalDegradation = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold slaRisk = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold operationalBacklog = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold userImpact = TransversalKpiThreshold.risk();
                private TransversalKpiThreshold affectedServices = TransversalKpiThreshold.risk();

                public TransversalKpiThreshold thresholdFor(String metricKey) {
                        return switch (metricKey) {
                                case "transversal.globalStatus" -> globalStatus;
                                case "transversal.globalCriticality" -> globalCriticality;
                                case "transversal.globalAvailability" -> globalAvailability;
                                case "transversal.operationalPressure" -> operationalPressure;
                                case "transversal.technicalDegradation" -> technicalDegradation;
                                case "transversal.slaRisk" -> slaRisk;
                                case "transversal.operationalBacklog" -> operationalBacklog;
                                case "transversal.userImpact" -> userImpact;
                                case "transversal.affectedServices" -> affectedServices;
                                default -> globalStatus;
                        };
                }

                public TransversalKpiThreshold getGlobalStatus() {
                        return globalStatus;
                }

                public void setGlobalStatus(TransversalKpiThreshold globalStatus) {
                        this.globalStatus = globalStatus;
                }

                public TransversalKpiThreshold getGlobalCriticality() {
                        return globalCriticality;
                }

                public void setGlobalCriticality(TransversalKpiThreshold globalCriticality) {
                        this.globalCriticality = globalCriticality;
                }

                public TransversalKpiThreshold getGlobalAvailability() {
                        return globalAvailability;
                }

                public void setGlobalAvailability(TransversalKpiThreshold globalAvailability) {
                        this.globalAvailability = globalAvailability;
                }

                public TransversalKpiThreshold getOperationalPressure() {
                        return operationalPressure;
                }

                public void setOperationalPressure(TransversalKpiThreshold operationalPressure) {
                        this.operationalPressure = operationalPressure;
                }

                public TransversalKpiThreshold getTechnicalDegradation() {
                        return technicalDegradation;
                }

                public void setTechnicalDegradation(TransversalKpiThreshold technicalDegradation) {
                        this.technicalDegradation = technicalDegradation;
                }

                public TransversalKpiThreshold getSlaRisk() {
                        return slaRisk;
                }

                public void setSlaRisk(TransversalKpiThreshold slaRisk) {
                        this.slaRisk = slaRisk;
                }

                public TransversalKpiThreshold getOperationalBacklog() {
                        return operationalBacklog;
                }

                public void setOperationalBacklog(TransversalKpiThreshold operationalBacklog) {
                        this.operationalBacklog = operationalBacklog;
                }

                public TransversalKpiThreshold getUserImpact() {
                        return userImpact;
                }

                public void setUserImpact(TransversalKpiThreshold userImpact) {
                        this.userImpact = userImpact;
                }

                public TransversalKpiThreshold getAffectedServices() {
                        return affectedServices;
                }

                public void setAffectedServices(TransversalKpiThreshold affectedServices) {
                        this.affectedServices = affectedServices;
                }
        }

        public static class TransversalKpiThreshold {

                private String direction = "RISK";
                private int yellowMin = 34;
                private int redMin = 67;
                private int greenMin = 67;

                public static TransversalKpiThreshold risk() {
                        TransversalKpiThreshold threshold = new TransversalKpiThreshold();
                        threshold.setDirection("RISK");
                        threshold.setYellowMin(34);
                        threshold.setRedMin(67);
                        return threshold;
                }

                public static TransversalKpiThreshold health() {
                        TransversalKpiThreshold threshold = new TransversalKpiThreshold();
                        threshold.setDirection("HEALTH");
                        threshold.setYellowMin(34);
                        threshold.setGreenMin(67);
                        return threshold;
                }

                public String getDirection() {
                        return direction;
                }

                public void setDirection(String direction) {
                        this.direction = direction;
                }

                public int getYellowMin() {
                        return yellowMin;
                }

                public void setYellowMin(int yellowMin) {
                        this.yellowMin = yellowMin;
                }

                public int getRedMin() {
                        return redMin;
                }

                public void setRedMin(int redMin) {
                        this.redMin = redMin;
                }

                public int getGreenMin() {
                        return greenMin;
                }

                public void setGreenMin(int greenMin) {
                        this.greenMin = greenMin;
                }
        }

        public static class Executive {

                private int trendDifferenceThreshold = 10;

                public int getTrendDifferenceThreshold() {
                        return trendDifferenceThreshold;
                }

                public void setTrendDifferenceThreshold(int trendDifferenceThreshold) {
                        this.trendDifferenceThreshold = trendDifferenceThreshold;
                }
        }
}

