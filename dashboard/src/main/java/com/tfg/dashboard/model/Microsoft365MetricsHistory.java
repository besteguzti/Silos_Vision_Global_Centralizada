package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Snapshot histórico de métricas Microsoft 365 simuladas.
 *
 * Guarda señales de servicios, identidad, seguridad y dispositivos para reconstruir el índice de salud y los KPIs transversales.
 */
@Entity
@Table(name = "microsoft365_metrics_history")
public class Microsoft365MetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer activeUsers;
    private Integer unassignedLicenses;
    private String outlookStatus;
    private String teamsStatus;
    private String sharePointStatus;
    private Integer nearlyFullMailboxes;
    private Integer emailsQuarantined;
    private Integer sharePointStoragePercent;
    private Integer riskyUsers;
    private Integer failedSignIns;
    private Integer usersWithoutMfa;
    private Integer appsSecretsExpiringSoon;
    private Integer unusedApplications;
    private Integer highPrivilegeApplications;
    private Integer nonCompliantDevices;
    private Integer outdatedWindowsDevices;
    private Integer devicesWithoutEncryption;
    private Integer staleDevices;
    private String microsoft365Health;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public Microsoft365MetricsHistory() {
    }

    public Long getId() {
        return id;
    }

    public int getActiveUsers() {
        return activeUsers == null ? 0 : activeUsers;
    }

    public int getUnassignedLicenses() {
        return unassignedLicenses == null ? 0 : unassignedLicenses;
    }

    public String getOutlookStatus() {
        return outlookStatus;
    }

    public String getTeamsStatus() {
        return teamsStatus;
    }

    public String getSharePointStatus() {
        return sharePointStatus;
    }

    public int getNearlyFullMailboxes() {
        return nearlyFullMailboxes == null ? 0 : nearlyFullMailboxes;
    }

    public int getEmailsQuarantined() {
        return emailsQuarantined == null ? 0 : emailsQuarantined;
    }

    public int getSharePointStoragePercent() {
        return sharePointStoragePercent == null
                ? 0
                : sharePointStoragePercent;
    }

    public int getRiskyUsers() {
        return riskyUsers == null ? 0 : riskyUsers;
    }

    public int getFailedSignIns() {
        return failedSignIns == null ? 0 : failedSignIns;
    }

    public int getUsersWithoutMfa() {
        return usersWithoutMfa == null ? 0 : usersWithoutMfa;
    }

    public int getAppsSecretsExpiringSoon() {
        return appsSecretsExpiringSoon == null
                ? 0
                : appsSecretsExpiringSoon;
    }

    public int getUnusedApplications() {
        return unusedApplications == null ? 0 : unusedApplications;
    }

    public int getHighPrivilegeApplications() {
        return highPrivilegeApplications == null
                ? 0
                : highPrivilegeApplications;
    }

    public int getNonCompliantDevices() {
        return nonCompliantDevices == null ? 0 : nonCompliantDevices;
    }

    public int getOutdatedWindowsDevices() {
        return outdatedWindowsDevices == null
                ? 0
                : outdatedWindowsDevices;
    }

    public int getDevicesWithoutEncryption() {
        return devicesWithoutEncryption == null
                ? 0
                : devicesWithoutEncryption;
    }

    public int getStaleDevices() {
        return staleDevices == null ? 0 : staleDevices;
    }

    public String getMicrosoft365Health() {
        return microsoft365Health;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setActiveUsers(Integer activeUsers) {
        this.activeUsers = activeUsers;
    }

    public void setUnassignedLicenses(Integer unassignedLicenses) {
        this.unassignedLicenses = unassignedLicenses;
    }

    public void setOutlookStatus(String outlookStatus) {
        this.outlookStatus = outlookStatus;
    }

    public void setTeamsStatus(String teamsStatus) {
        this.teamsStatus = teamsStatus;
    }

    public void setSharePointStatus(String sharePointStatus) {
        this.sharePointStatus = sharePointStatus;
    }

    public void setNearlyFullMailboxes(Integer nearlyFullMailboxes) {
        this.nearlyFullMailboxes = nearlyFullMailboxes;
    }

    public void setEmailsQuarantined(Integer emailsQuarantined) {
        this.emailsQuarantined = emailsQuarantined;
    }

    public void setSharePointStoragePercent(Integer sharePointStoragePercent) {
        this.sharePointStoragePercent = sharePointStoragePercent;
    }

    public void setRiskyUsers(Integer riskyUsers) {
        this.riskyUsers = riskyUsers;
    }

    public void setFailedSignIns(Integer failedSignIns) {
        this.failedSignIns = failedSignIns;
    }

    public void setUsersWithoutMfa(Integer usersWithoutMfa) {
        this.usersWithoutMfa = usersWithoutMfa;
    }

    public void setAppsSecretsExpiringSoon(Integer appsSecretsExpiringSoon) {
        this.appsSecretsExpiringSoon = appsSecretsExpiringSoon;
    }

    public void setUnusedApplications(Integer unusedApplications) {
        this.unusedApplications = unusedApplications;
    }

    public void setHighPrivilegeApplications(Integer highPrivilegeApplications) {
        this.highPrivilegeApplications = highPrivilegeApplications;
    }

    public void setNonCompliantDevices(Integer nonCompliantDevices) {
        this.nonCompliantDevices = nonCompliantDevices;
    }

    public void setOutdatedWindowsDevices(Integer outdatedWindowsDevices) {
        this.outdatedWindowsDevices = outdatedWindowsDevices;
    }

    public void setDevicesWithoutEncryption(Integer devicesWithoutEncryption) {
        this.devicesWithoutEncryption = devicesWithoutEncryption;
    }

    public void setStaleDevices(Integer staleDevices) {
        this.staleDevices = staleDevices;
    }

    public void setMicrosoft365Health(String microsoft365Health) {
        this.microsoft365Health = microsoft365Health;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}

