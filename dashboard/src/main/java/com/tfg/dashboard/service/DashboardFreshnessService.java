package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.model.CitrixMetricsHistory;
import com.tfg.dashboard.model.GlpiMetricsHistory;
import com.tfg.dashboard.model.Microsoft365MetricsHistory;

/**
 * Evalúa la frescura de los datos que alimentan el dashboard principal.
 *
 * Distingue entre datos recientes, obsoletos y ausentes para evitar que el
 * dashboard presente como GREEN una plataforma sin snapshot válido.
 */
@Service
public class DashboardFreshnessService {

    private final KpiScoringService kpiScoringService;
    private final KpiProperties kpiProperties;

    public DashboardFreshnessService(
            KpiScoringService kpiScoringService,
            KpiProperties kpiProperties) {
        this.kpiScoringService = kpiScoringService;
        this.kpiProperties = kpiProperties;
    }

    /**
     * Calcula el dataStatus a partir del último snapshot persistido.
     */
    public String calculateDataStatus(Optional<? extends Object> snapshot) {

        if (snapshot.isEmpty()) {

            return "NO_DATA";
        }

        if (snapshot.get() instanceof CitrixMetricsHistory citrix) {

            return calculateDataStatus(
                    citrix.getCollectedAt(),
                    kpiProperties.getFreshness().getCitrixMinutes());
        }

        if (snapshot.get() instanceof Microsoft365MetricsHistory microsoft365) {

            return calculateDataStatus(
                    microsoft365.getCollectedAt(),
                    kpiProperties.getFreshness().getMicrosoft365Minutes());
        }

        if (snapshot.get() instanceof GlpiMetricsHistory glpi) {

            return calculateDataStatus(
                    glpi.getCollectedAt(),
                    kpiProperties.getFreshness().getGlpiMinutes());
        }

        return "NO_DATA";
    }

    /**
     * Clasifica una fecha de captura como OK, STALE o NO_DATA.
     */
    public String calculateDataStatus(LocalDateTime collectedAt) {

        return calculateDataStatus(
                collectedAt,
                defaultFreshnessMinutes());
    }

    private int defaultFreshnessMinutes() {

        return Math.max(
                Math.max(
                        kpiProperties.getFreshness().getArubaMinutes(),
                        kpiProperties.getFreshness().getCitrixMinutes()),
                Math.max(
                        kpiProperties.getFreshness().getMicrosoft365Minutes(),
                        kpiProperties.getFreshness().getGlpiMinutes()));
    }

    private String calculateDataStatus(
            LocalDateTime collectedAt,
            int freshnessMinutes) {

        // El margen supera ligeramente la cadencia horaria del scheduler para
        // evitar marcar STALE un snapshot valido entre sincronizaciones.

        if (collectedAt == null) {

            return "NO_DATA";
        }

        if (collectedAt.isAfter(
                LocalDateTime.now().minusMinutes(freshnessMinutes))) {

            return "OK";
        }

        return "STALE";
    }

    /**
     * Agrega la frescura de todas las plataformas; NO_DATA prevalece sobre
     * STALE y STALE prevalece sobre OK.
     */
    public String calculateGlobalDataStatus(
            String arubaDataStatus,
            String citrixDataStatus,
            String microsoft365DataStatus,
            String glpiDataStatus) {

        if ("NO_DATA".equalsIgnoreCase(arubaDataStatus)
                || "NO_DATA".equalsIgnoreCase(citrixDataStatus)
                || "NO_DATA".equalsIgnoreCase(microsoft365DataStatus)
                || "NO_DATA".equalsIgnoreCase(glpiDataStatus)) {

            return "NO_DATA";
        }

        if ("STALE".equalsIgnoreCase(arubaDataStatus)
                || "STALE".equalsIgnoreCase(citrixDataStatus)
                || "STALE".equalsIgnoreCase(microsoft365DataStatus)
                || "STALE".equalsIgnoreCase(glpiDataStatus)) {

            return "STALE";
        }

        return "OK";
    }

    /**
     * Devuelve la fecha más reciente entre Aruba y los snapshots simulados.
     */
    public LocalDateTime latestCollectedAt(
            LocalDateTime arubaLastUpdated,
            Optional<CitrixMetricsHistory> citrixSnapshot,
            Optional<Microsoft365MetricsHistory> microsoft365Snapshot,
            Optional<GlpiMetricsHistory> glpiSnapshot) {

        LocalDateTime latest = arubaLastUpdated;

        if (citrixSnapshot.isPresent()) {

            latest = newer(latest, citrixSnapshot.get().getCollectedAt());
        }

        if (microsoft365Snapshot.isPresent()) {

            latest = newer(latest,microsoft365Snapshot.get().getCollectedAt());
        }

        if (glpiSnapshot.isPresent()) {

            latest = newer(latest, glpiSnapshot.get().getCollectedAt());
        }

        return latest;
    }

    /**
     * Normaliza estados vacíos como NO_DATA para no ocultar ausencia de datos.
     */
    public String normalizeDataStatus(String dataStatus) {

        // Ante la duda se considera ausencia de datos.

        if (dataStatus == null || dataStatus.isBlank()) {

            return "NO_DATA";
        }

        return dataStatus;
    }

    /**
     * Aplica frescura al color calculado, manteniendo RED cuando ya existe una
     * condición crítica funcional.
     */
    public String applyFreshnessToColor(String color,String dataStatus) {

        if ("RED".equalsIgnoreCase(color)) {

            return color;
        }

        return kpiScoringService.statusFromFreshness(dataStatus, color);
    }

    private LocalDateTime newer(LocalDateTime current,LocalDateTime candidate) {

        if (candidate == null) {

            return current;
        }

        if (current == null || candidate.isAfter(current)) {

            return candidate;
        }

        return current;
    }
}

