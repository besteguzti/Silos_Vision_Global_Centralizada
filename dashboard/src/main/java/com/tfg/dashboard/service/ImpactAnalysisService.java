package com.tfg.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.AnalyticsComparePoint;
import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Calcula la relación entre degradación técnica e impacto en usuarios.
 *
 * Usa Aruba, Citrix y Microsoft 365 como señales técnicas, y GLPI como señal
 * operativa, siempre en escala de afección 0-100.
 */
@Service
public class ImpactAnalysisService {

    private final KpiProperties kpiProperties;

    public ImpactAnalysisService(KpiProperties kpiProperties) {

        this.kpiProperties = kpiProperties;
    }

    /**
     * Calcula degradación técnica sin incluir GLPI, porque GLPI representa la
     * consecuencia operativa.
     */
    public int calculateTechnicalDegradation(
            double aruba,
            double citrix,
            double microsoft365
    ) {

        KpiProperties.PlatformWeights weights =
                kpiProperties.getWeights().getAnalysisTechnicalDegradation();

        return clampToInt(
                aruba * weights.getAruba()
                        + citrix * weights.getCitrix()
                        + microsoft365 * weights.getMicrosoft365()
        );
    }

    /**
     * Calcula impacto en usuarios combinando señales técnicas y presión GLPI.
     */
    public int calculateUserImpact(
            double aruba,
            double citrix,
            double microsoft365,
            double glpi
    ) {

        KpiProperties.PlatformWeights weights =
                kpiProperties.getWeights().getAnalysisUserImpact();

        return clampToInt(
                aruba * weights.getAruba()
                        + citrix * weights.getCitrix()
                        + microsoft365 * weights.getMicrosoft365()
                        + glpi * weights.getGlpi()
        );
    }

    /**
     * Traduce el cuadrante técnico/usuario a una lectura funcional sencilla.
     */
    public String technicalImpactInterpretation(
            int technicalDegradation,
            int userImpact
    ) {

        // La lectura funcional se genera
        // en backend para que React solo
        // represente la interpretacion
        // recibida.

        boolean technicalAffected =
                technicalDegradation >= kpiProperties.getStatus().getYellowMin();

        boolean userAffected =
                userImpact >= kpiProperties.getStatus().getYellowMin();

        if (technicalAffected && userAffected) {

            return "Existe degradación técnica con impacto operativo visible sobre usuarios.";
        }

        if (technicalAffected) {

            return "Hay degradación técnica, pero no se observa impacto visible alto en usuarios.";
        }

        if (userAffected) {

            return "El impacto en usuarios es alto aunque la degradación técnica monitorizada sea baja; puede existir una causa no detectada.";
        }

        return "Ambos indicadores son bajos; la situacion se interpreta como normal.";
    }

    /**
     * Prepara los puntos de la gráfica degradación técnica vs impacto usuarios.
     */
    public List<AnalyticsComparePoint> buildTechnicalImpactPoints(
            List<AnalysisSnapshot> snapshots
    ) {

        return snapshots.stream()
                .map(snapshot -> new AnalyticsComparePoint(
                        snapshot.getTimestamp(),
                        (double) calculateTechnicalDegradation(
                                safeInt(snapshot.getArubaHealth()),
                                safeInt(snapshot.getCitrixHealth()),
                                safeInt(snapshot.getMicrosoft365Health())
                        ),
                        (double) calculateUserImpact(
                                safeInt(snapshot.getArubaHealth()),
                                safeInt(snapshot.getCitrixHealth()),
                                safeInt(snapshot.getMicrosoft365Health()),
                                safeInt(snapshot.getGlpiOperationalPressure())
                        )
                ))
                .toList();
    }

    private int clampToInt(double value) {

        return (int) Math.round(clamp(value));
    }

    private double clamp(double value) {

        if (value < 0) {

            return 0;
        }

        if (value > kpiProperties.getStatus().getMax()) {

            return kpiProperties.getStatus().getMax();
        }

        return value;
    }

    private int safeInt(Integer value) {

        return value != null
                ? value
                : 0;
    }
}

