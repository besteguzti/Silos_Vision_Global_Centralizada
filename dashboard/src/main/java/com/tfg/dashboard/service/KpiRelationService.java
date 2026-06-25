package com.tfg.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiRelationDto;
import com.tfg.dashboard.dto.KpiRelationPointDto;
import com.tfg.dashboard.model.AnalysisSnapshot;

/**
 * Construye relaciones exploratorias entre indicadores concretos.
 *
 * Los puntos se agregan por dia para evitar nubes de snapshots demasiado densas
 * y la lectura distingue co-ocurrencia de tendencia visual.
 */
@Service
public class KpiRelationService {

    private static final int MINIMUM_POINTS = 3;
    private static final int HOURS_PER_BUCKET = 6;
    private static final int HIGH_RELATION_PERCENT = 60;
    private static final int MODERATE_RELATION_PERCENT = 35;
    private static final double MINIMUM_ABSOLUTE_VARIATION = 5.0;
    private static final double MINIMUM_RELATIVE_VARIATION = 0.10;
    private static final double STRONG_POSITIVE_TREND = 0.45;
    private static final double MODERATE_POSITIVE_TREND = 0.25;
    private static final String UNKNOWN_STATUS = "UNKNOWN";

    private final KpiProperties kpiProperties;
    private final KpiScoringService kpiScoringService;

    public KpiRelationService(
            KpiProperties kpiProperties,
            KpiScoringService kpiScoringService) {

        this.kpiProperties = kpiProperties;
        this.kpiScoringService = kpiScoringService;
    }

    /**
     * Devuelve el catalogo final de relaciones especificas del panel de analisis.
     *
     * El orden de la lista coincide con el orden alfabetico visible en el selector.
     */
    public List<KpiRelationDto> buildRelations(List<AnalysisSnapshot> snapshots) {
        return List.of(
                highLowRelation(
                        "aruba_affectation_vs_wifi_clients",
                        "Afectación Aruba vs clientes WiFi",
                        "Afectación Aruba",
                        "Clientes WiFi Aruba",
                        "%",
                        "",
                        "Compara la afección normalizada de Aruba con el volumen de clientes WiFi conectados.",
                        "Si aumenta la afección Aruba y bajan los clientes WiFi, puede orientar la revisión de conectividad sin demostrar causalidad.",
                        snapshots,
                        AnalysisSnapshot::getArubaHealth,
                        AnalysisSnapshot::getArubaWifiClients,
                        (double) kpiProperties.getStatus().getYellowMin()),
                highHighRelation(
                        "aruba_affectation_vs_aruba_tickets",
                        "Afectación Aruba vs tickets Aruba",
                        "Afectación Aruba",
                        "Tickets abiertos Aruba",
                        "%",
                        "",
                        "Relaciona la afección de red Aruba con tickets GLPI clasificados como Aruba.",
                        "Una coincidencia alta puede orientar la revisión de conectividad y soporte asociado a Aruba, sin afirmar causalidad.",
                        snapshots,
                        AnalysisSnapshot::getArubaHealth,
                        AnalysisSnapshot::getArubaOpenTickets,
                        (double) kpiProperties.getStatus().getYellowMin(),
                        (double) kpiProperties.getAruba().getArubaOpenTicketsYellowMin()),
                highHighRelation(
                        "citrix_affectation_vs_citrix_tickets",
                        "Afectación Citrix vs tickets Citrix",
                        "Afectación Citrix",
                        "Tickets abiertos Citrix",
                        "%",
                        "",
                        "Relaciona la afección normalizada Citrix con tickets GLPI clasificados como Citrix.",
                        "La coincidencia puede orientar la revisión de acceso a aplicaciones y soporte Citrix sin afirmar causa directa.",
                        snapshots,
                        AnalysisSnapshot::getCitrixHealth,
                        AnalysisSnapshot::getCitrixOpenTickets,
                        (double) kpiProperties.getStatus().getYellowMin(),
                        (double) kpiProperties.getCitrix().getCitrixOpenTicketsYellowMin()),
                highHighRelation(
                        "microsoft365_affectation_vs_microsoft365_tickets",
                        "Afectación Microsoft 365 vs tickets Microsoft 365",
                        "Afectación Microsoft 365",
                        "Tickets abiertos Microsoft 365",
                        "%",
                        "",
                        "Relaciona la afección normalizada Microsoft 365 con tickets GLPI clasificados como Microsoft 365.",
                        "La coincidencia puede orientar la revisión de servicios cloud, identidad o puesto de usuario sin afirmar causalidad.",
                        snapshots,
                        AnalysisSnapshot::getMicrosoft365Health,
                        AnalysisSnapshot::getMicrosoft365OpenTickets,
                        (double) kpiProperties.getStatus().getYellowMin(),
                        (double) kpiProperties.getMicrosoft365().getMicrosoft365OpenTicketsYellowMin()),
                dynamicHighHighRelation(
                        "aruba_wifi_clients_vs_citrix_sessions",
                        "Clientes WiFi Aruba vs sesiones Citrix",
                        "Clientes WiFi Aruba",
                        "Sesiones Citrix",
                        "",
                        "",
                        "Compara el volumen de clientes WiFi con las sesiones activas Citrix.",
                        "Si ambos volúmenes se mueven juntos, puede orientar la revisión de uso y acceso corporativo, sin afirmar causalidad.",
                        snapshots,
                        AnalysisSnapshot::getArubaWifiClients,
                        AnalysisSnapshot::getCitrixActiveSessions),
                dynamicHighHighRelation(
                        "aruba_wifi_clients_vs_microsoft365_active_users",
                        "Clientes WiFi Aruba vs usuarios activos Microsoft 365",
                        "Clientes WiFi Aruba",
                        "Usuarios activos Microsoft 365",
                        "",
                        "",
                        "Compara clientes WiFi Aruba con actividad de usuarios Microsoft 365.",
                        "La relación puede ayudar a ver si el uso de red y servicios cloud evoluciona de forma parecida en el histórico.",
                        snapshots,
                        AnalysisSnapshot::getArubaWifiClients,
                        AnalysisSnapshot::getMicrosoft365ActiveUsers),
                lowHighRelation(
                        "citrix_delivery_controllers_vs_failed_logons",
                        "Delivery Controllers disponibles vs errores de inicio Citrix",
                        "Delivery Controllers disponibles",
                        "Errores de inicio Citrix",
                        "",
                        "",
                        "Compara disponibilidad de Delivery Controllers con errores de inicio Citrix.",
                        "Si hay menos controladores disponibles y más errores, puede orientar la revisión de acceso Citrix sin demostrar causalidad.",
                        snapshots,
                        AnalysisSnapshot::getCitrixAvailableDeliveryControllers,
                        AnalysisSnapshot::getCitrixFailedLogons,
                        (double) kpiProperties.getCitrix().getFailedLogonsYellowAbove() + 1),
                dynamicHighHighRelation(
                        "citrix_delivery_controllers_vs_sessions",
                        "Delivery Controllers disponibles vs sesiones Citrix",
                        "Delivery Controllers disponibles",
                        "Sesiones Citrix",
                        "",
                        "",
                        "Relaciona la disponibilidad de Delivery Controllers con las sesiones activas Citrix.",
                        "Puede orientar la lectura de capacidad y uso de Citrix, manteniendo una interpretación exploratoria.",
                        snapshots,
                        AnalysisSnapshot::getCitrixAvailableDeliveryControllers,
                        AnalysisSnapshot::getCitrixActiveSessions),
                highHighRelation(
                        "glpi_pressure_vs_operational_backlog",
                        "Presión operativa GLPI vs backlog operativo",
                        "Presión operativa GLPI",
                        "Backlog operativo",
                        "%",
                        "",
                        "Compara la presión operativa de GLPI con el volumen de trabajo pendiente acumulado.",
                        "Esta relación permite observar si el incremento de presión operativa está asociado a una acumulación de backlog, incluso cuando las plataformas técnicas no presentan degradación clara.",
                        snapshots,
                        AnalysisSnapshot::getGlpiOperationalPressure,
                        AnalysisSnapshot::getGlpiOperationalBacklog,
                        (double) kpiProperties.getStatus().getYellowMin(),
                        (double) kpiProperties.getGlpi().getOpenTicketsYellowMin()),
                highHighRelation(
                        "glpi_pressure_vs_open_tickets",
                        "Presión operativa GLPI vs tickets abiertos GLPI",
                        "Presión operativa GLPI",
                        "Tickets abiertos GLPI",
                        "%",
                        "",
                        "Relaciona la presión operativa calculada en GLPI con el volumen total de tickets abiertos.",
                        "Esta relación permite comprobar si el aumento de presión operativa se corresponde con un incremento del volumen de tickets abiertos en GLPI.",
                        snapshots,
                        AnalysisSnapshot::getGlpiOperationalPressure,
                        AnalysisSnapshot::getGlpiOpenTickets,
                        (double) kpiProperties.getStatus().getYellowMin(),
                        (double) kpiProperties.getGlpi().getOpenTicketsYellowMin()),
                highHighRelation(
                        "aruba_down_switches_vs_down_aps",
                        "Switches apagados vs APs caídos",
                        "Switches apagados",
                        "APs caídos",
                        "",
                        "",
                        "Compara switches Aruba apagados con APs caídos.",
                        "La coincidencia puede orientar la revisión de conectividad y dependencias de red, sin afirmar causa directa.",
                        snapshots,
                        AnalysisSnapshot::getArubaDownSwitches,
                        AnalysisSnapshot::getArubaDownAps,
                        (double) kpiProperties.getAruba().getSwitchDownYellowAbove() + 1,
                        1.0),
                buildRelation(
                        "glpi_created_vs_closed_tickets",
                        "Tickets creados GLPI vs tickets cerrados GLPI",
                        "Tickets creados GLPI",
                        "Tickets cerrados GLPI",
                        "",
                        "",
                        "Compara la entrada diaria/semanal de tickets con la capacidad de cierre del equipo de soporte.",
                        "Esta relación permite observar si el volumen de tickets creados supera al volumen de tickets cerrados, lo que puede anticipar acumulación de backlog y presión operativa.",
                        snapshots,
                        AnalysisSnapshot::getGlpiCreatedToday,
                        AnalysisSnapshot::getGlpiClosedToday,
                        values -> values.stream()
                                .filter(point -> point.getX() > point.getY())
                                .count(),
                        "días con más tickets creados que cerrados"),
                dynamicHighHighRelation(
                        "microsoft365_active_users_vs_citrix_sessions",
                        "Usuarios activos Microsoft 365 vs sesiones Citrix",
                        "Usuarios activos Microsoft 365",
                        "Sesiones Citrix",
                        "",
                        "",
                        "Compara actividad Microsoft 365 con sesiones Citrix.",
                        "Ayuda a ver si el uso de servicios cloud y acceso a aplicaciones corporativas evoluciona de forma parecida.",
                        snapshots,
                        AnalysisSnapshot::getMicrosoft365ActiveUsers,
                        AnalysisSnapshot::getCitrixActiveSessions));
    }

    private KpiRelationDto highHighRelation(
            String code,
            String title,
            String xLabel,
            String yLabel,
            String xUnit,
            String yUnit,
            String description,
            String interpretation,
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> xExtractor,
            Function<AnalysisSnapshot, Integer> yExtractor,
            Double xHighThreshold,
            Double yHighThreshold) {

        return buildRelation(
                code,
                title,
                xLabel,
                yLabel,
                xUnit,
                yUnit,
                description,
                interpretation,
                snapshots,
                xExtractor,
                yExtractor,
                values -> values.stream()
                        .filter(point -> point.getX() >= xHighThreshold && point.getY() >= yHighThreshold)
                        .count(),
                "co-ocurrencia de valores altos");
    }

    private KpiRelationDto dynamicHighHighRelation(
            String code,
            String title,
            String xLabel,
            String yLabel,
            String xUnit,
            String yUnit,
            String description,
            String interpretation,
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> xExtractor,
            Function<AnalysisSnapshot, Integer> yExtractor) {

        return buildRelation(
                code,
                title,
                xLabel,
                yLabel,
                xUnit,
                yUnit,
                description,
                interpretation,
                snapshots,
                xExtractor,
                yExtractor,
                values -> values.stream()
                        .filter(point -> point.getX() >= dynamicHighThreshold(values, KpiRelationPointDto::getX)
                                && point.getY() >= dynamicHighThreshold(values, KpiRelationPointDto::getY))
                        .count(),
                "co-ocurrencia de valores altos");
    }

    private KpiRelationDto highLowRelation(
            String code,
            String title,
            String xLabel,
            String yLabel,
            String xUnit,
            String yUnit,
            String description,
            String interpretation,
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> xExtractor,
            Function<AnalysisSnapshot, Integer> yExtractor,
            Double xHighThreshold) {

        return buildRelation(
                code,
                title,
                xLabel,
                yLabel,
                xUnit,
                yUnit,
                description,
                interpretation,
                snapshots,
                xExtractor,
                yExtractor,
                values -> values.stream()
                        .filter(point -> point.getX() >= xHighThreshold
                                && point.getY() <= dynamicLowThreshold(values, KpiRelationPointDto::getY))
                        .count(),
                "coincidencia de mayor afección y menor volumen");
    }

    private KpiRelationDto lowHighRelation(
            String code,
            String title,
            String xLabel,
            String yLabel,
            String xUnit,
            String yUnit,
            String description,
            String interpretation,
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> xExtractor,
            Function<AnalysisSnapshot, Integer> yExtractor,
            Double yHighThreshold) {

        return buildRelation(
                code,
                title,
                xLabel,
                yLabel,
                xUnit,
                yUnit,
                description,
                interpretation,
                snapshots,
                xExtractor,
                yExtractor,
                values -> values.stream()
                        .filter(point -> point.getX() <= dynamicLowThreshold(values, KpiRelationPointDto::getX)
                                && point.getY() >= yHighThreshold)
                        .count(),
                "coincidencia de baja disponibilidad y valores altos");
    }

    private KpiRelationDto buildRelation(
            String code,
            String title,
            String xLabel,
            String yLabel,
            String xUnit,
            String yUnit,
            String description,
            String interpretation,
            List<AnalysisSnapshot> snapshots,
            Function<AnalysisSnapshot, Integer> xExtractor,
            Function<AnalysisSnapshot, Integer> yExtractor,
            Function<List<KpiRelationPointDto>, Long> matchingCounter,
            String coincidenceLabel) {

        List<KpiRelationPointDto> points = aggregateDaily(
                snapshots.stream()
                        .filter(snapshot -> snapshot.getTimestamp() != null)
                        .filter(snapshot -> xExtractor.apply(snapshot) != null && yExtractor.apply(snapshot) != null)
                        .map(snapshot -> new KpiRelationPointDto(
                                snapshot.getTimestamp(),
                                xExtractor.apply(snapshot).doubleValue(),
                                yExtractor.apply(snapshot).doubleValue(),
                                1,
                                Boolean.TRUE.equals(snapshot.isGeneratedScenario())))
                        .toList());

        KpiRelationDto relation = new KpiRelationDto();
        relation.setCode(code);
        relation.setTitle(title);
        relation.setXLabel(xLabel);
        relation.setYLabel(yLabel);
        relation.setXUnit(xUnit);
        relation.setYUnit(yUnit);
        relation.setDescription(description);
        relation.setPoints(points);
        relation.setHasEnoughData(points.size() >= MINIMUM_POINTS);

        if (!relation.isHasEnoughData()) {
            relation.setReading("Sin datos suficientes para valorar esta relación.");
            relation.setReadingStatus(KpiScoringService.NO_DATA);
            return relation;
        }

        boolean xHasVariation = hasMeaningfulVariation(points, KpiRelationPointDto::getX);
        boolean yHasVariation = hasMeaningfulVariation(points, KpiRelationPointDto::getY);

        if (!xHasVariation || !yHasVariation) {
            relation.setReading(insufficientVariationText(xHasVariation, yHasVariation));
            relation.setReadingStatus(UNKNOWN_STATUS);
            return relation;
        }

        int coincidencePercent = kpiScoringService.clampToInt(
                matchingCounter.apply(points) * 100.0 / points.size());
        double positiveTrend = pearson(points);

        relation.setReading(readingText(
                coincidencePercent,
                positiveTrend,
                interpretation,
                coincidenceLabel));
        relation.setReadingStatus(kpiScoringService.statusFromAffection(coincidencePercent));

        return relation;
    }

    private List<KpiRelationPointDto> aggregateDaily(List<KpiRelationPointDto> rawPoints) {
        return rawPoints.stream()
                .collect(Collectors.groupingBy(point -> point.getTimestamp().toLocalDate()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> dailyPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private KpiRelationPointDto dailyPoint(
            LocalDate day,
            List<KpiRelationPointDto> dayPoints) {

        List<KpiRelationPointDto> bucketPoints = dayPoints.stream()
                .collect(Collectors.groupingBy(point -> point.getTimestamp().getHour() / HOURS_PER_BUCKET))
                .values()
                .stream()
                .map(this::averageBucket)
                .sorted(Comparator.comparing(KpiRelationPointDto::getTimestamp))
                .toList();

        return new KpiRelationPointDto(
                day.atStartOfDay(),
                average(bucketPoints, KpiRelationPointDto::getX),
                average(bucketPoints, KpiRelationPointDto::getY),
                bucketPoints.size(),
                bucketPoints.stream().anyMatch(KpiRelationPointDto::isGeneratedScenario));
    }

    private KpiRelationPointDto averageBucket(List<KpiRelationPointDto> bucketPoints) {
        LocalDateTime timestamp = bucketPoints.get(0).getTimestamp();

        return new KpiRelationPointDto(
                timestamp,
                average(bucketPoints, KpiRelationPointDto::getX),
                average(bucketPoints, KpiRelationPointDto::getY),
                bucketPoints.size(),
                bucketPoints.stream().anyMatch(KpiRelationPointDto::isGeneratedScenario));
    }

    private String readingText(
            int coincidencePercent,
            double positiveTrend,
            String interpretation,
            String coincidenceLabel) {

        if (coincidencePercent >= HIGH_RELATION_PERCENT
                && positiveTrend >= STRONG_POSITIVE_TREND) {

            return "Relación alta: hay " + coincidenceLabel
                    + " y una tendencia visual positiva. "
                    + interpretation
                    + " Coincidencia observada: " + coincidencePercent
                    + "%. Es una relación aparente para orientar la revisión.";
        }

        if (coincidencePercent >= HIGH_RELATION_PERCENT) {
            return "Hay " + coincidenceLabel
                    + " en varios días, pero no una tendencia clara. "
                    + "Coincidencia observada: " + coincidencePercent
                    + "%. Puede orientar la revisión.";
        }

        if (coincidencePercent >= MODERATE_RELATION_PERCENT
                || positiveTrend >= STRONG_POSITIVE_TREND) {

            return "Relación moderada: se aprecia cierta coincidencia aparente entre ambos indicadores. "
                    + interpretation
                    + " Coincidencia observada: " + coincidencePercent
                    + "%. Sirve como señal exploratoria.";
        }

        if (positiveTrend >= MODERATE_POSITIVE_TREND) {
            return "Tendencia visual débil: algunos días apuntan en la misma dirección, "
                    + "pero la coincidencia observada es solo del " + coincidencePercent
                    + "%. Se necesita más histórico para interpretar la relación.";
        }

        return "Relación baja: no se aprecia un patrón claro en las capturas disponibles. "
                + "La coincidencia observada es del " + coincidencePercent
                + "% y solo debe usarse como referencia exploratoria.";
    }

    private String insufficientVariationText(boolean xHasVariation, boolean yHasVariation) {
        if (!xHasVariation && !yHasVariation) {
            return "No hay variación suficiente para interpretar una relación clara. "
                    + "Los snapshots tienen valores muy parecidos en ambos ejes; se necesita más histórico "
                    + "o mayor variación en los datos.";
        }

        if (!xHasVariation) {
            return "No hay variación suficiente para interpretar una relación clara. "
                    + "Los snapshots tienen valores muy parecidos en el eje X, por lo que no se puede "
                    + "interpretar una tendencia clara. Se necesita más histórico o mayor variación.";
        }

        return "No hay variación suficiente para interpretar una relación clara. "
                + "Los snapshots tienen valores muy parecidos en el eje Y, por lo que no se puede "
                + "interpretar una tendencia clara. Se necesita más histórico o mayor variación.";
    }

    private boolean hasMeaningfulVariation(
            List<KpiRelationPointDto> points,
            Function<KpiRelationPointDto, Double> extractor) {

        double min = points.stream()
                .mapToDouble(extractor::apply)
                .min()
                .orElse(0);
        double max = points.stream()
                .mapToDouble(extractor::apply)
                .max()
                .orElse(0);
        double range = max - min;
        double reference = Math.max(1.0, Math.max(Math.abs(min), Math.abs(max)));

        return range >= MINIMUM_ABSOLUTE_VARIATION
                || range / reference >= MINIMUM_RELATIVE_VARIATION;
    }

    private double dynamicHighThreshold(
            List<KpiRelationPointDto> points,
            Function<KpiRelationPointDto, Double> extractor) {

        double min = points.stream()
                .mapToDouble(extractor::apply)
                .min()
                .orElse(0);
        double max = points.stream()
                .mapToDouble(extractor::apply)
                .max()
                .orElse(0);

        return min + (max - min) * 0.67;
    }

    private double dynamicLowThreshold(
            List<KpiRelationPointDto> points,
            Function<KpiRelationPointDto, Double> extractor) {

        double min = points.stream()
                .mapToDouble(extractor::apply)
                .min()
                .orElse(0);
        double max = points.stream()
                .mapToDouble(extractor::apply)
                .max()
                .orElse(0);

        return min + (max - min) * 0.33;
    }

    private double pearson(List<KpiRelationPointDto> points) {
        double averageX = average(points, KpiRelationPointDto::getX);
        double averageY = average(points, KpiRelationPointDto::getY);
        double numerator = 0;
        double xVariance = 0;
        double yVariance = 0;

        for (KpiRelationPointDto point : points) {
            double xDiff = point.getX() - averageX;
            double yDiff = point.getY() - averageY;

            numerator += xDiff * yDiff;
            xVariance += xDiff * xDiff;
            yVariance += yDiff * yDiff;
        }

        double denominator = Math.sqrt(xVariance * yVariance);

        return denominator == 0 ? 0 : numerator / denominator;
    }

    private double average(
            List<KpiRelationPointDto> points,
            Function<KpiRelationPointDto, Double> extractor) {

        return points.stream()
                .mapToDouble(extractor::apply)
                .average()
                .orElse(0);
    }
}

