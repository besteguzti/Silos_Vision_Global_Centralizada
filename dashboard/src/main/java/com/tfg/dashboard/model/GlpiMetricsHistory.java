package com.tfg.dashboard.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Snapshot histórico de métricas GLPI simuladas.
 *
 * Conserva tickets, SLA y capacidad de cierre para calcular presión operativa, frescura y análisis transversal.
 */
@Entity
@Table(name = "glpi_metrics_history")
public class GlpiMetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer openTickets;
    private Integer arubaOpenTickets;
    private Integer citrixOpenTickets;
    private Integer microsoft365OpenTickets;
    private Integer criticalOpenTickets;
    private Integer slaBreachedTickets;
    private Integer averageResolutionHours;
    private Integer createdToday;
    private Integer closedToday;
    private Integer createdThisWeek;
    private Integer closedThisWeek;
    private Integer operationalBacklog;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    public GlpiMetricsHistory() {
    }

    public Long getId() {
        return id;
    }

    public int getOpenTickets() {
        return openTickets == null ? 0 : openTickets;
    }

    public int getArubaOpenTickets() {
        return arubaOpenTickets == null ? 0 : arubaOpenTickets;
    }

    public Integer getArubaOpenTicketsRaw() {
        return arubaOpenTickets;
    }

    public int getCitrixOpenTickets() {
        return citrixOpenTickets == null ? 0 : citrixOpenTickets;
    }

    public Integer getCitrixOpenTicketsRaw() {
        return citrixOpenTickets;
    }

    public int getMicrosoft365OpenTickets() {
        return microsoft365OpenTickets == null ? 0 : microsoft365OpenTickets;
    }

    public Integer getMicrosoft365OpenTicketsRaw() {
        return microsoft365OpenTickets;
    }

    public int getCriticalOpenTickets() {
        return criticalOpenTickets == null ? 0 : criticalOpenTickets;
    }

    public int getSlaBreachedTickets() {
        return slaBreachedTickets == null ? 0 : slaBreachedTickets;
    }

    public int getAverageResolutionHours() {
        return averageResolutionHours == null
                ? 0
                : averageResolutionHours;
    }

    public int getCreatedToday() {
        return createdToday == null ? 0 : createdToday;
    }

    public int getClosedToday() {
        return closedToday == null ? 0 : closedToday;
    }

    public int getCreatedThisWeek() {
        return createdThisWeek == null ? 0 : createdThisWeek;
    }

    public int getClosedThisWeek() {
        return closedThisWeek == null ? 0 : closedThisWeek;
    }

    public int getOperationalBacklog() {
        return operationalBacklog == null ? 0 : operationalBacklog;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOpenTickets(Integer openTickets) {
        this.openTickets = openTickets;
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

    public void setCriticalOpenTickets(Integer criticalOpenTickets) {
        this.criticalOpenTickets = criticalOpenTickets;
    }

    public void setSlaBreachedTickets(Integer slaBreachedTickets) {
        this.slaBreachedTickets = slaBreachedTickets;
    }

    public void setAverageResolutionHours(Integer averageResolutionHours) {
        this.averageResolutionHours = averageResolutionHours;
    }

    public void setCreatedToday(Integer createdToday) {
        this.createdToday = createdToday;
    }

    public void setClosedToday(Integer closedToday) {
        this.closedToday = closedToday;
    }

    public void setCreatedThisWeek(Integer createdThisWeek) {
        this.createdThisWeek = createdThisWeek;
    }

    public void setClosedThisWeek(Integer closedThisWeek) {
        this.closedThisWeek = closedThisWeek;
    }

    public void setOperationalBacklog(Integer operationalBacklog) {
        this.operationalBacklog = operationalBacklog;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}

