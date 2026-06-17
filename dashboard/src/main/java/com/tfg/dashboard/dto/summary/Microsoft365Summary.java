package com.tfg.dashboard.dto.summary;

import java.time.LocalDateTime;

import com.tfg.dashboard.dto.KpiResultDto;
import com.tfg.dashboard.dto.Microsoft365HealthStatusDto;

//Respuesta de la vista Microsoft 365.

public class Microsoft365Summary {

    private int activeUsers;
    private int unassignedLicenses;
    private String outlookStatus;
    private String teamsStatus;
    private String sharePointStatus;
    private int nearlyFullMailboxes;
    private int emailsQuarantined;
    private int sharePointStoragePercent;
    private int riskyUsers;
    private int failedSignIns;
    private int usersWithoutMfa;
    private int appsSecretsExpiringSoon;
    private int unusedApplications;
    private int highPrivilegeApplications;
    private int nonCompliantDevices;
    private int microsoft365OpenTickets;
    private int outdatedWindowsDevices;
    private int devicesWithoutEncryption;
    private int staleDevices;
    private Microsoft365HealthStatusDto microsoft365HealthDetails;
    private KpiResultDto microsoft365HealthKpi;
    private LocalDateTime lastUpdated;
    private String dataStatus;

    public Microsoft365Summary() {
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getUnassignedLicenses() {
        return unassignedLicenses;
    }

    public void setUnassignedLicenses(int unassignedLicenses) {
        this.unassignedLicenses = unassignedLicenses;
    }

    public String getOutlookStatus() {
        return outlookStatus;
    }

    public void setOutlookStatus(String outlookStatus) {
        this.outlookStatus = outlookStatus;
    }

    public String getTeamsStatus() {
        return teamsStatus;
    }

    public void setTeamsStatus(String teamsStatus) {
        this.teamsStatus = teamsStatus;
    }

    public String getSharePointStatus() {
        return sharePointStatus;
    }

    public void setSharePointStatus(String sharePointStatus) {
        this.sharePointStatus = sharePointStatus;
    }

    public int getNearlyFullMailboxes() {
        return nearlyFullMailboxes;
    }

    public void setNearlyFullMailboxes(int nearlyFullMailboxes) {
        this.nearlyFullMailboxes = nearlyFullMailboxes;
    }

    public int getEmailsQuarantined() {
        return emailsQuarantined;
    }

    public void setEmailsQuarantined(int emailsQuarantined) {
        this.emailsQuarantined = emailsQuarantined;
    }

    public int getSharePointStoragePercent() {
        return sharePointStoragePercent;
    }

    public void setSharePointStoragePercent(int sharePointStoragePercent) {
        this.sharePointStoragePercent = sharePointStoragePercent;
    }

    public int getRiskyUsers() {
        return riskyUsers;
    }

    public void setRiskyUsers(int riskyUsers) {
        this.riskyUsers = riskyUsers;
    }

    public int getFailedSignIns() {
        return failedSignIns;
    }

    public void setFailedSignIns(int failedSignIns) {
        this.failedSignIns = failedSignIns;
    }

    public int getUsersWithoutMfa() {
        return usersWithoutMfa;
    }

    public void setUsersWithoutMfa(int usersWithoutMfa) {
        this.usersWithoutMfa = usersWithoutMfa;
    }

    public int getAppsSecretsExpiringSoon() {
        return appsSecretsExpiringSoon;
    }

    public void setAppsSecretsExpiringSoon(int appsSecretsExpiringSoon) {
        this.appsSecretsExpiringSoon = appsSecretsExpiringSoon;
    }

    public int getUnusedApplications() {
        return unusedApplications;
    }

    public void setUnusedApplications(int unusedApplications) {
        this.unusedApplications = unusedApplications;
    }

    public int getHighPrivilegeApplications() {
        return highPrivilegeApplications;
    }

    public void setHighPrivilegeApplications(int highPrivilegeApplications) {
        this.highPrivilegeApplications = highPrivilegeApplications;
    }

    public int getNonCompliantDevices() {
        return nonCompliantDevices;
    }

    public int getMicrosoft365OpenTickets() {
        return microsoft365OpenTickets;
    }

    public void setNonCompliantDevices(int nonCompliantDevices) {
        this.nonCompliantDevices = nonCompliantDevices;
    }

    public void setMicrosoft365OpenTickets(int microsoft365OpenTickets) {
        this.microsoft365OpenTickets = microsoft365OpenTickets;
    }

    public int getOutdatedWindowsDevices() {
        return outdatedWindowsDevices;
    }

    public void setOutdatedWindowsDevices(int outdatedWindowsDevices) {
        this.outdatedWindowsDevices = outdatedWindowsDevices;
    }

    public int getDevicesWithoutEncryption() {
        return devicesWithoutEncryption;
    }

    public void setDevicesWithoutEncryption(int devicesWithoutEncryption) {
        this.devicesWithoutEncryption = devicesWithoutEncryption;
    }

    public int getStaleDevices() {
        return staleDevices;
    }

    public void setStaleDevices(int staleDevices) {
        this.staleDevices = staleDevices;
    }

    public Microsoft365HealthStatusDto getMicrosoft365HealthDetails() {
        return microsoft365HealthDetails;
    }

    public KpiResultDto getMicrosoft365HealthKpi() {
        return microsoft365HealthKpi;
    }

    public void setMicrosoft365HealthDetails(Microsoft365HealthStatusDto microsoft365HealthDetails) {
        this.microsoft365HealthDetails = microsoft365HealthDetails;
    }

    public void setMicrosoft365HealthKpi(KpiResultDto microsoft365HealthKpi) {
        this.microsoft365HealthKpi = microsoft365HealthKpi;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDataStatus() {
        return dataStatus;
    }

    public void setDataStatus(String dataStatus) {
        this.dataStatus = dataStatus;
    }
}
