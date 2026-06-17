package com.tfg.dashboard.dto;

import java.time.LocalDateTime;

import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Respuesta publica para snapshots del panel de analisis.
 *
 * Mantiene los campos historicos que consume el panel, pero no expone el identificador interno de la entidad JPA.
 */
public class AnalysisSnapshotDto {

    private final LocalDateTime timestamp;
    private final Integer arubaHealth;
    private final Integer citrixHealth;
    private final Integer microsoft365Health;
    private final Integer glpiHealth;
    private final Integer glpiOperationalPressure;
    private final Integer technicalDegradation;
    private final Integer userImpact;
    private final Integer globalStatus;
    private final Integer arubaWifiClients;
    private final Integer arubaDownAps;
    private final Integer citrixAverageLogonDurationSeconds;
    private final Integer citrixActiveSessions;
    private final Integer citrixAvailableDeliveryControllers;
    private final Integer citrixFailedLogons;
    private final Integer glpiOpenTickets;
    private final Integer glpiCreatedToday;
    private final Integer glpiClosedToday;
    private final Integer glpiCreatedThisWeek;
    private final Integer glpiClosedThisWeek;
    private final Integer glpiOperationalBacklog;
    private final Integer arubaOpenTickets;
    private final Integer citrixOpenTickets;
    private final Integer microsoft365OpenTickets;
    private final Integer microsoft365ActiveUsers;
    private final Integer microsoft365NonCompliantDevices;
    private final Integer microsoft365UsersWithoutMfa;
    private final Integer microsoft365FailedSignIns;
    private final Integer affectedServicesPercent;
    private final String arubaStatus;
    private final String citrixStatus;
    private final String microsoft365Status;
    private final String glpiStatus;
    private final Boolean generatedScenario;

    public AnalysisSnapshotDto(AnalysisSnapshot snapshot) {
        this.timestamp = snapshot.getTimestamp();
        this.arubaHealth = snapshot.getArubaHealth();
        this.citrixHealth = snapshot.getCitrixHealth();
        this.microsoft365Health = snapshot.getMicrosoft365Health();
        this.glpiHealth = snapshot.getGlpiHealth();
        this.glpiOperationalPressure = snapshot.getGlpiOperationalPressure();
        this.technicalDegradation = snapshot.getTechnicalDegradation();
        this.userImpact = snapshot.getUserImpact();
        this.globalStatus = snapshot.getGlobalStatus();
        this.arubaWifiClients = snapshot.getArubaWifiClients();
        this.arubaDownAps = snapshot.getArubaDownAps();
        this.citrixAverageLogonDurationSeconds = snapshot.getCitrixAverageLogonDurationSeconds();
        this.citrixActiveSessions = snapshot.getCitrixActiveSessions();
        this.citrixAvailableDeliveryControllers = snapshot.getCitrixAvailableDeliveryControllers();
        this.citrixFailedLogons = snapshot.getCitrixFailedLogons();
        this.glpiOpenTickets = snapshot.getGlpiOpenTickets();
        this.glpiCreatedToday = snapshot.getGlpiCreatedToday();
        this.glpiClosedToday = snapshot.getGlpiClosedToday();
        this.glpiCreatedThisWeek = snapshot.getGlpiCreatedThisWeek();
        this.glpiClosedThisWeek = snapshot.getGlpiClosedThisWeek();
        this.glpiOperationalBacklog = snapshot.getGlpiOperationalBacklog();
        this.arubaOpenTickets = snapshot.getArubaOpenTickets();
        this.citrixOpenTickets = snapshot.getCitrixOpenTickets();
        this.microsoft365OpenTickets = snapshot.getMicrosoft365OpenTickets();
        this.microsoft365ActiveUsers = snapshot.getMicrosoft365ActiveUsers();
        this.microsoft365NonCompliantDevices = snapshot.getMicrosoft365NonCompliantDevices();
        this.microsoft365UsersWithoutMfa = snapshot.getMicrosoft365UsersWithoutMfa();
        this.microsoft365FailedSignIns = snapshot.getMicrosoft365FailedSignIns();
        this.affectedServicesPercent = snapshot.getAffectedServicesPercent();
        this.arubaStatus = snapshot.getArubaStatus();
        this.citrixStatus = snapshot.getCitrixStatus();
        this.microsoft365Status = snapshot.getMicrosoft365Status();
        this.glpiStatus = snapshot.getGlpiStatus();
        this.generatedScenario = snapshot.isGeneratedScenario();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getArubaHealth() {
        return arubaHealth;
    }

    public Integer getCitrixHealth() {
        return citrixHealth;
    }

    public Integer getMicrosoft365Health() {
        return microsoft365Health;
    }

    public Integer getGlpiHealth() {
        return glpiHealth;
    }

    public Integer getGlpiOperationalPressure() {
        return glpiOperationalPressure;
    }

    public Integer getTechnicalDegradation() {
        return technicalDegradation;
    }

    public Integer getUserImpact() {
        return userImpact;
    }

    public Integer getGlobalStatus() {
        return globalStatus;
    }

    public Integer getArubaWifiClients() {
        return arubaWifiClients;
    }

    public Integer getArubaDownAps() {
        return arubaDownAps;
    }

    public Integer getCitrixAverageLogonDurationSeconds() {
        return citrixAverageLogonDurationSeconds;
    }

    public Integer getCitrixActiveSessions() {
        return citrixActiveSessions;
    }

    public Integer getCitrixAvailableDeliveryControllers() {
        return citrixAvailableDeliveryControllers;
    }

    public Integer getCitrixFailedLogons() {
        return citrixFailedLogons;
    }

    public Integer getGlpiOpenTickets() {
        return glpiOpenTickets;
    }

    public Integer getGlpiCreatedToday() {
        return glpiCreatedToday;
    }

    public Integer getGlpiClosedToday() {
        return glpiClosedToday;
    }

    public Integer getGlpiCreatedThisWeek() {
        return glpiCreatedThisWeek;
    }

    public Integer getGlpiClosedThisWeek() {
        return glpiClosedThisWeek;
    }

    public Integer getGlpiOperationalBacklog() {
        return glpiOperationalBacklog;
    }

    public Integer getArubaOpenTickets() {
        return arubaOpenTickets;
    }

    public Integer getCitrixOpenTickets() {
        return citrixOpenTickets;
    }

    public Integer getMicrosoft365OpenTickets() {
        return microsoft365OpenTickets;
    }

    public Integer getMicrosoft365ActiveUsers() {
        return microsoft365ActiveUsers;
    }

    public Integer getMicrosoft365NonCompliantDevices() {
        return microsoft365NonCompliantDevices;
    }

    public Integer getMicrosoft365UsersWithoutMfa() {
        return microsoft365UsersWithoutMfa;
    }

    public Integer getMicrosoft365FailedSignIns() {
        return microsoft365FailedSignIns;
    }

    public Integer getAffectedServicesPercent() {
        return affectedServicesPercent;
    }

    public String getArubaStatus() {
        return arubaStatus;
    }

    public String getCitrixStatus() {
        return citrixStatus;
    }

    public String getMicrosoft365Status() {
        return microsoft365Status;
    }

    public String getGlpiStatus() {
        return glpiStatus;
    }

    public Boolean getGeneratedScenario() {
        return generatedScenario;
    }
}
