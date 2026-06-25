package com.tfg.dashboard.service;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.repository.GlpiMetricsHistoryRepository;

/**
 * Centraliza la lectura del ultimo reparto de tickets GLPI por plataforma.
 *
 * Permite que las páginas de Aruba, Citrix y Microsoft 365 muestren su carga
 * operativa asociada sin duplicar consultas al histórico GLPI.
 */
@Service
public class GlpiPlatformTicketService {

    private final GlpiMetricsHistoryRepository glpiRepository;

    public GlpiPlatformTicketService(GlpiMetricsHistoryRepository glpiRepository) {
        this.glpiRepository = glpiRepository;
    }

    public int getArubaOpenTickets() {
        return latestGlpiSnapshot()
                .map(GlpiMetricsHistory::getArubaOpenTickets)
                .orElse(0);
    }

    public int getCitrixOpenTickets() {
        return latestGlpiSnapshot()
                .map(GlpiMetricsHistory::getCitrixOpenTickets)
                .orElse(0);
    }

    public int getMicrosoft365OpenTickets() {
        return latestGlpiSnapshot()
                .map(GlpiMetricsHistory::getMicrosoft365OpenTickets)
                .orElse(0);
    }

    private java.util.Optional<GlpiMetricsHistory> latestGlpiSnapshot() {
        return glpiRepository.findTopByOrderByCollectedAtDesc();
    }
}

