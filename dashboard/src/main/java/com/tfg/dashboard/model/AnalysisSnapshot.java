package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Snapshot persistido del panel de análisis.
 * Guarda valores diarios usados por las gráficas, relaciones e informes del panel.
 * generatedScenario permite distinguir datos reales de datos cargados para pruebas.
 */
@Entity
@Table(name = "analysis_snapshots")
public class AnalysisSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime timestamp;

    // Nombres heredados por compatibilidad con la tabla. En estos campos, un valor más alto significa más afección.
     
    private Integer arubaHealth;
    private Integer citrixHealth;
    private Integer microsoft365Health;
    private Integer glpiHealth;
    private Integer glpiOperationalPressure;
    private Integer technicalDegradation;
    private Integer userImpact;
    // Nombre heredado. Es un valor numérico 0-100 del estado global ponderado, no un texto tipo GREEN/YELLOW/RED.
        private Integer globalStatus;
    private Integer arubaWifiClients;
    private Integer arubaInactiveAps;
    private Integer arubaDownSwitches;
    @Column(name = "aruba_down_aps")
    private Integer arubaDownAps;
    private Integer citrixAverageLogonDurationSeconds;
    private Integer citrixActiveSessions;
    @Column(name = "citrix_available_delivery_controllers")
    private Integer citrixAvailableDeliveryControllers;
    private Integer citrixServerLoadPercent;
    private Integer citrixFailedLogons;
    private Integer glpiOpenTickets;
    @Column(name = "glpi_created_today")
    private Integer glpiCreatedToday;
    @Column(name = "glpi_closed_today")
    private Integer glpiClosedToday;
    @Column(name = "glpi_created_this_week")
    private Integer glpiCreatedThisWeek;
    @Column(name = "glpi_closed_this_week")
    private Integer glpiClosedThisWeek;
    @Column(name = "glpi_operational_backlog")
    private Integer glpiOperationalBacklog;
    private Integer arubaOpenTickets;
    private Integer citrixOpenTickets;
    private Integer microsoft365OpenTickets;
    @Column(name = "microsoft365_active_users")
    private Integer microsoft365ActiveUsers;
    private Integer microsoft365NonCompliantDevices;
    private Integer microsoft365UsersWithoutMfa;
    private Integer microsoft365FailedSignIns;
    private Integer affectedServicesPercent;
    private String arubaStatus;
    private String citrixStatus;
    private String microsoft365Status;
    private String glpiStatus;
    private Boolean generatedScenario;

    public Long getId() {
        return id;
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

    public Integer getArubaInactiveAps() {
        return arubaInactiveAps;
    }

    public Integer getArubaDownSwitches() {
        return arubaDownSwitches;
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

    public Integer getCitrixServerLoadPercent() {
        return citrixServerLoadPercent;
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

    public Boolean isGeneratedScenario() {
        return generatedScenario;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setArubaHealth(Integer arubaHealth) {
        this.arubaHealth = arubaHealth;
    }

    public void setCitrixHealth(Integer citrixHealth) {
        this.citrixHealth = citrixHealth;
    }

    public void setMicrosoft365Health(Integer microsoft365Health) {
        this.microsoft365Health = microsoft365Health;
    }

    public void setGlpiHealth(Integer glpiHealth) {
        this.glpiHealth = glpiHealth;
    }

    public void setGlpiOperationalPressure(Integer glpiOperationalPressure) {
        this.glpiOperationalPressure = glpiOperationalPressure;
    }

    public void setTechnicalDegradation(Integer technicalDegradation) {
        this.technicalDegradation = technicalDegradation;
    }

    public void setUserImpact(Integer userImpact) {
        this.userImpact = userImpact;
    }

    public void setGlobalStatus(Integer globalStatus) {
        this.globalStatus = globalStatus;
    }

    public void setArubaWifiClients(Integer arubaWifiClients) {
        this.arubaWifiClients = arubaWifiClients;
    }

    public void setArubaInactiveAps(Integer arubaInactiveAps) {
        this.arubaInactiveAps = arubaInactiveAps;
    }

    public void setArubaDownSwitches(Integer arubaDownSwitches) {
        this.arubaDownSwitches = arubaDownSwitches;
    }

    public void setArubaDownAps(Integer arubaDownAps) {
        this.arubaDownAps = arubaDownAps;
    }

    public void setCitrixAverageLogonDurationSeconds(Integer citrixAverageLogonDurationSeconds) {
        this.citrixAverageLogonDurationSeconds = citrixAverageLogonDurationSeconds;
    }

    public void setCitrixActiveSessions(Integer citrixActiveSessions) {
        this.citrixActiveSessions = citrixActiveSessions;
    }

    public void setCitrixAvailableDeliveryControllers(Integer citrixAvailableDeliveryControllers) {
        this.citrixAvailableDeliveryControllers = citrixAvailableDeliveryControllers;
    }

    public void setCitrixServerLoadPercent(Integer citrixServerLoadPercent) {
        this.citrixServerLoadPercent = citrixServerLoadPercent;
    }

    public void setCitrixFailedLogons(Integer citrixFailedLogons) {
        this.citrixFailedLogons = citrixFailedLogons;
    }

    public void setGlpiOpenTickets(Integer glpiOpenTickets) {
        this.glpiOpenTickets = glpiOpenTickets;
    }

    public void setGlpiCreatedToday(Integer glpiCreatedToday) {
        this.glpiCreatedToday = glpiCreatedToday;
    }

    public void setGlpiClosedToday(Integer glpiClosedToday) {
        this.glpiClosedToday = glpiClosedToday;
    }

    public void setGlpiCreatedThisWeek(Integer glpiCreatedThisWeek) {
        this.glpiCreatedThisWeek = glpiCreatedThisWeek;
    }

    public void setGlpiClosedThisWeek(Integer glpiClosedThisWeek) {
        this.glpiClosedThisWeek = glpiClosedThisWeek;
    }

    public void setGlpiOperationalBacklog(Integer glpiOperationalBacklog) {
        this.glpiOperationalBacklog = glpiOperationalBacklog;
    }

    public void setArubaOpenTickets(Integer arubaOpenTickets) {
        this.arubaOpenTickets = arubaOpenTickets;
    }

    public void setCitrixOpenTickets(Integer citrixOpenTickets) {
        this.citrixOpenTickets = citrixOpenTickets;
    }

    public void setMicrosoft365OpenTickets(Integer microsoft365OpenTickets) {
        this.microsoft365OpenTickets = microsoft365OpenTickets;
    }

    public void setMicrosoft365ActiveUsers(Integer microsoft365ActiveUsers) {
        this.microsoft365ActiveUsers = microsoft365ActiveUsers;
    }

    public void setMicrosoft365NonCompliantDevices(Integer microsoft365NonCompliantDevices) {
        this.microsoft365NonCompliantDevices = microsoft365NonCompliantDevices;
    }

    public void setMicrosoft365UsersWithoutMfa(Integer microsoft365UsersWithoutMfa) {
        this.microsoft365UsersWithoutMfa = microsoft365UsersWithoutMfa;
    }

    public void setMicrosoft365FailedSignIns(Integer microsoft365FailedSignIns) {
        this.microsoft365FailedSignIns = microsoft365FailedSignIns;
    }

    public void setAffectedServicesPercent(Integer affectedServicesPercent) {
        this.affectedServicesPercent = affectedServicesPercent;
    }

    public void setArubaStatus(String arubaStatus) {
        this.arubaStatus = arubaStatus;
    }

    public void setCitrixStatus(String citrixStatus) {
        this.citrixStatus = citrixStatus;
    }

    public void setMicrosoft365Status(String microsoft365Status) {
        this.microsoft365Status = microsoft365Status;
    }

    public void setGlpiStatus(String glpiStatus) {
        this.glpiStatus = glpiStatus;
    }

    public void setGeneratedScenario(Boolean generatedScenario) {
        this.generatedScenario = generatedScenario;
    }
}

