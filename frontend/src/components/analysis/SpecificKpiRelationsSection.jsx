import { useMemo, useState } from "react";

import AnalysisEmptyState from "./AnalysisEmptyState";
import PeriodSelector from "./PeriodSelector";
import { formatValue } from "./analysisUtils";

const DEFAULT_RELATION_CODE = "aruba_affectation_vs_wifi_clients";
const MIN_CHART_POINTS = 2;
const CHART_WIDTH = 720;
const CHART_HEIGHT = 340;
const CHART_PADDING = 56;
const AXIS_PADDING_RATIO = 0.15;
const FLAT_AXIS_PADDING = 5;
const PERCENT_AXIS_MIN = 0;
const PERCENT_AXIS_MAX = 100;
const PERCENT_AXIS_TICKS = [0, 33, 66, 100];
const PERCENT_REFERENCE_TICKS = [33, 66];
const EMPTY_RELATIONS = [];

function axisLabel(label, unit) {
  return unit ? `${label} (${unit})` : label;
}

function formatWithUnit(value, unit) {
  return unit ? `${formatValue(value)} ${unit}` : formatValue(value);
}

function normalizeRelation(relation = {}) {
  return {
    ...relation,
    xLabel: relation.xLabel ?? relation.xlabel ?? "Eje X",
    yLabel: relation.yLabel ?? relation.ylabel ?? "Eje Y",
    xUnit: relation.xUnit ?? relation.xunit ?? "",
    yUnit: relation.yUnit ?? relation.yunit ?? ""
  };
}

function buildRelationRecommendation(relationCode) {
  switch (relationCode) {
    case "aruba_affectation_vs_wifi_clients":
      return "Revisar conectividad, APs, clientes WiFi y eventos Aruba en los días donde sube la afectación y baja el uso de red.";
    case "aruba_affectation_vs_aruba_tickets":
      return "Revisar los días en los que coinciden afectación Aruba y tickets Aruba para priorizar incidencias de conectividad.";
    case "citrix_affectation_vs_citrix_tickets":
      return "Revisar sesiones, Delivery Controllers, errores de inicio y tickets Citrix en los días con mayor coincidencia.";
    case "microsoft365_affectation_vs_microsoft365_tickets":
      return "Revisar identidad, cumplimiento de dispositivos, SharePoint y tickets Microsoft 365 en los días con mayor coincidencia.";
    case "aruba_wifi_clients_vs_citrix_sessions":
      return "Revisar si los días con menor conectividad WiFi coinciden con menor uso de sesiones Citrix.";
    case "aruba_wifi_clients_vs_microsoft365_active_users":
      return "Revisar si los días con menor conectividad WiFi coinciden con menos usuarios activos en Microsoft 365.";
    case "citrix_delivery_controllers_vs_failed_logons":
      return "Revisar disponibilidad de Delivery Controllers y errores de inicio Citrix en los días con peor combinación.";
    case "citrix_delivery_controllers_vs_sessions":
      return "Revisar si la disponibilidad de Delivery Controllers coincide con cambios relevantes en las sesiones Citrix.";
    case "aruba_down_switches_vs_down_aps":
      return "Revisar switching y APs caídos en los días donde ambos indicadores empeoran a la vez.";
    case "glpi_pressure_vs_operational_backlog":
      return "Revisar si la presión operativa GLPI coincide con acumulación de backlog, especialmente cuando Aruba, Citrix y Microsoft 365 permanecen estables.";
    case "glpi_pressure_vs_open_tickets":
      return "Revisar si la presión operativa GLPI se corresponde con incremento de tickets abiertos y capacidad de soporte tensionada.";
    case "glpi_created_vs_closed_tickets":
      return "Revisar si los tickets creados superan a los cerrados de forma persistente, porque esa brecha explica acumulación de trabajo pendiente.";
    case "microsoft365_active_users_vs_citrix_sessions":
      return "Revisar si la actividad de Microsoft 365 y las sesiones Citrix evolucionan de forma parecida en el periodo.";
    default:
      return "Revisar los datos y considerar si el patrón observado se reproduce en más histórico.";
  }
}

function buildRelationReport(relation, period, periods) {
  const points = (relation.points ?? []).filter(
    (point) => point && Number.isFinite(Number(point.x)) && Number.isFinite(Number(point.y))
  );
  const pointCount = points.length;
  const periodLabel = periods?.find((item) => item.value === period)?.label ?? period;
  const xValues = points.map((point) => Number(point.x));
  const yValues = points.map((point) => Number(point.y));
  const xAverage = xValues.length > 0 ? xValues.reduce((sum, value) => sum + value, 0) / xValues.length : 0;
  const yAverage = yValues.length > 0 ? yValues.reduce((sum, value) => sum + value, 0) / yValues.length : 0;
  const xMax = xValues.length > 0 ? Math.max(...xValues) : 0;
  const yMax = yValues.length > 0 ? Math.max(...yValues) : 0;
  const xMin = xValues.length > 0 ? Math.min(...xValues) : 0;
  const yMin = yValues.length > 0 ? Math.min(...yValues) : 0;
  const xHasVariation = xMax - xMin >= 5;
  const yHasVariation = yMax - yMin >= 5;
  const xHighThreshold = relation.xUnit === "%" ? 67 : xMin + (xMax - xMin) * 0.67;
  const yHighThreshold = relation.yUnit === "%" ? 67 : yMin + (yMax - yMin) * 0.67;
  const highHighCount = points.filter(
    (point) => Number(point.x) >= xHighThreshold && Number(point.y) >= yHighThreshold
  ).length;
  const highHighPercentage = pointCount > 0 ? Math.round((highHighCount * 100) / pointCount) : 0;
  const hasEnoughData = pointCount >= MIN_CHART_POINTS;
  const hasSufficientVariation = xHasVariation && yHasVariation;
  const hasSamples = points.some((point) => Number.isFinite(Number(point.samplesUsed)));
  const totalSamples = points.reduce(
    (acc, point) => acc + (Number.isFinite(Number(point.samplesUsed)) ? Number(point.samplesUsed) : 0),
    0
  );

  const patternObserved = (() => {
    if (!hasEnoughData) {
      return "No hay datos suficientes para interpretar una tendencia clara.";
    }

    if (!hasSufficientVariation) {
      return "No hay variación suficiente para interpretar una tendencia clara.";
    }

    if (highHighPercentage >= 60) {
      return `Se observa co-ocurrencia alta-alta en el ${highHighPercentage}% de los días analizados.`;
    }

    if (highHighPercentage >= 30) {
      return "Se observa una relación aparente moderada, con coincidencias en parte del periodo.";
    }

    return "La relación aparente es baja en los datos disponibles.";
  })();

  const conclusion = (() => {
    if (!hasEnoughData) {
      return "No hay datos suficientes para una conclusión sólida. Este informe es exploratorio y no demuestra causalidad.";
    }

    if (!hasSufficientVariation) {
      return "La interpretación es limitada por la poca variación observada. No demuestra causalidad y solo puede orientar la revisión.";
    }

    if (highHighPercentage >= 60) {
      return "Los datos sugieren una relación aparente alta. No demuestra causalidad, pero puede orientar al responsable IT a revisar primero los días con valores altos.";
    }

    if (highHighPercentage >= 30) {
      return "Los datos sugieren una relación aparente moderada. No demuestra causalidad, pero puede orientar la revisión.";
    }

    return "Los datos sugieren una relación aparente baja. No demuestra causalidad y puede indicar que este patrón no es dominante en el periodo.";
  })();

  return {
    pointCount,
    periodLabel,
    xAverage,
    yAverage,
    xMax,
    yMax,
    xMin,
    yMin,
    highHighPercentage,
    xHighThreshold,
    yHighThreshold,
    hasSamples,
    totalSamples,
    patternObserved,
    conclusion,
    recommendation: buildRelationRecommendation(relation.code),
    hasEnoughData,
    hasSufficientVariation
  };
}

function buildAxis(points, key, unit) {
  const isPercent = unit === "%";

  if (isPercent) {
    return {
      min: PERCENT_AXIS_MIN,
      max: PERCENT_AXIS_MAX,
      ticks: PERCENT_AXIS_TICKS,
      referenceTicks: PERCENT_REFERENCE_TICKS
    };
  }

  const values = points.map((point) => Number(point[key] ?? 0));
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = maxValue - minValue;
  const padding = range === 0
    ? FLAT_AXIS_PADDING
    : Math.max(1, range * AXIS_PADDING_RATIO);
  const min = Math.max(0, minValue - padding);
  const max = maxValue + padding;
  const safeMax = max === min ? min + FLAT_AXIS_PADDING : max;

  return {
    min,
    max: safeMax,
    ticks: [min, (min + safeMax) / 2, safeMax],
    referenceTicks: []
  };
}

function SpecificRelationScatterChart({ relation }) {
  const points = relation.points ?? [];
  const xAxis = buildAxis(points, "x", relation.xUnit);
  const yAxis = buildAxis(points, "y", relation.yUnit);

  const scaleX = (value) =>
    CHART_PADDING
    + ((Number(value ?? 0) - xAxis.min) / (xAxis.max - xAxis.min))
      * (CHART_WIDTH - CHART_PADDING * 2);

  const scaleY = (value) =>
    CHART_HEIGHT
    - CHART_PADDING
    - ((Number(value ?? 0) - yAxis.min) / (yAxis.max - yAxis.min))
      * (CHART_HEIGHT - CHART_PADDING * 2);

  if (points.length < MIN_CHART_POINTS) {
    return <AnalysisEmptyState />;
  }

  return (
    <svg
      className="analysis-chart"
      viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
      role="img"
      aria-label={`${axisLabel(relation.xLabel, relation.xUnit)} frente a ${axisLabel(relation.yLabel, relation.yUnit)}`}
    >
      <line
        x1={CHART_PADDING}
        y1={CHART_HEIGHT - CHART_PADDING}
        x2={CHART_WIDTH - CHART_PADDING}
        y2={CHART_HEIGHT - CHART_PADDING}
      />
      <line
        x1={CHART_PADDING}
        y1={CHART_PADDING}
        x2={CHART_PADDING}
        y2={CHART_HEIGHT - CHART_PADDING}
      />

      {xAxis.ticks.map((tick) => (
        <g key={`specific-x-${tick}`}>
          <line
            className={xAxis.referenceTicks.includes(tick) ? "analysis-reference-line" : "analysis-grid-line"}
            x1={scaleX(tick)}
            y1={CHART_PADDING}
            x2={scaleX(tick)}
            y2={CHART_HEIGHT - CHART_PADDING}
          />
          <text
            x={scaleX(tick)}
            y={CHART_HEIGHT - CHART_PADDING + 24}
            textAnchor="middle"
          >
            {formatWithUnit(tick, relation.xUnit)}
          </text>
        </g>
      ))}

      {yAxis.ticks.map((tick) => (
        <g key={`specific-y-${tick}`}>
          <line
            className={yAxis.referenceTicks.includes(tick) ? "analysis-reference-line" : "analysis-grid-line"}
            x1={CHART_PADDING}
            y1={scaleY(tick)}
            x2={CHART_WIDTH - CHART_PADDING}
            y2={scaleY(tick)}
          />
          <text
            x={CHART_PADDING - 12}
            y={scaleY(tick) + 4}
            textAnchor="end"
          >
            {formatWithUnit(tick, relation.yUnit)}
          </text>
        </g>
      ))}

      <text x={CHART_WIDTH / 2} y={CHART_HEIGHT - 14} textAnchor="middle">
        {axisLabel(relation.xLabel, relation.xUnit)}
      </text>
      <text
        x={18}
        y={CHART_HEIGHT / 2}
        textAnchor="middle"
        transform={`rotate(-90 18 ${CHART_HEIGHT / 2})`}
      >
        {axisLabel(relation.yLabel, relation.yUnit)}
      </text>

      {points.map((point, index) => (
        <circle
          key={`${relation.code}-${point.timestamp}-${index}`}
          cx={scaleX(point.x)}
          cy={scaleY(point.y)}
          r="6"
        >
          <title>
            {`${new Date(point.timestamp).toLocaleDateString()} | ${relation.xLabel}: ${formatWithUnit(point.x, relation.xUnit)} | ${relation.yLabel}: ${formatWithUnit(point.y, relation.yUnit)} | Muestras usadas: ${point.samplesUsed ?? 1}${point.generatedScenario ? " | escenario de prueba" : ""}`}
          </title>
        </circle>
      ))}
    </svg>
  );
}

function SpecificKpiRelationsSection({ relations, periods, selectedPeriod, onPeriodChange }) {
  const availableRelations = relations ?? EMPTY_RELATIONS;
  const [selectedCode, setSelectedCode] = useState(DEFAULT_RELATION_CODE);

  const selectedRelation = useMemo(
    () =>
      normalizeRelation(
        availableRelations.find((relation) => relation.code === selectedCode)
        ?? availableRelations.find((relation) => relation.code === DEFAULT_RELATION_CODE)
        ?? availableRelations[0]
      ),
    [availableRelations, selectedCode]
  );

  const report = useMemo(
    () => buildRelationReport(selectedRelation, selectedPeriod, periods),
    [selectedRelation, selectedPeriod, periods]
  );

  const hasRelations = availableRelations.length > 0;
  const hasEnoughData =
    Boolean(selectedRelation?.hasEnoughData)
    && (selectedRelation?.points?.length ?? 0) >= MIN_CHART_POINTS;

  return (
    <section className="dashboard-section">
      <div className="dashboard-header">
        <div>
          <h2>Relaciones específicas entre indicadores</h2>
          <p className="section-subtitle">
            Esta sección permite comparar indicadores concretos de distintas
            plataformas. Cada punto representa un snapshot histórico. La
            gráfica ayuda a ver coincidencias aparentes entre dos indicadores,
            y debe leerse como apoyo exploratorio para orientar la revisión.
          </p>
          <p className="section-subtitle">
            Esta gráfica muestra coincidencias aparentes entre dos indicadores,
            pero no representa la secuencia temporal. Para analizar evolución,
            degradación o recuperación, consulte la evolución temporal del periodo.
          </p>
        </div>

        <div className="analysis-actions">
          {hasRelations && (
            <label>
              Relación a analizar
              <select
                value={selectedRelation?.code ?? ""}
                onChange={(event) => setSelectedCode(event.target.value)}
              >
                {availableRelations.map((relation) => (
                  <option key={relation.code} value={relation.code}>
                    {relation.title}
                  </option>
                ))}
              </select>
            </label>
          )}

          {periods && (
            <PeriodSelector
              periods={periods}
              selectedPeriod={selectedPeriod}
              onChange={onPeriodChange}
            />
          )}
        </div>
      </div>

      {!hasRelations ? (
        <AnalysisEmptyState />
      ) : (
        <div className="analysis-layout">
          <div className="analysis-chart-card">
            <p className="analysis-chart-explanation">
              {selectedRelation.description}
            </p>

            {hasEnoughData ? (
              <SpecificRelationScatterChart relation={selectedRelation} />
            ) : (
              <AnalysisEmptyState message="Sin datos suficientes para generar esta comparación." />
            )}
          </div>

          <aside className="analysis-summary">
            <h2>Informe de la relación</h2>
            <h3>{selectedRelation.title}</h3>

            <div className="report-block">
              <strong>Qué se compara</strong>
              <p>Eje X: {axisLabel(selectedRelation.xLabel, selectedRelation.xUnit)}</p>
              <p>Eje Y: {axisLabel(selectedRelation.yLabel, selectedRelation.yUnit)}</p>
            </div>

            <div className="report-block">
              <strong>Lectura del periodo</strong>
              <p>
                En el periodo seleccionado ({report.periodLabel}) se han analizado {report.pointCount} puntos diarios.
              </p>
              <p>
                La media de {selectedRelation.xLabel.toLowerCase()} ha sido de {formatWithUnit(report.xAverage, selectedRelation.xUnit)} y la media de {selectedRelation.yLabel.toLowerCase()} ha sido de {formatWithUnit(report.yAverage, selectedRelation.yUnit)}.
              </p>
              <p>
                El máximo de {selectedRelation.xLabel.toLowerCase()} es {formatWithUnit(report.xMax, selectedRelation.xUnit)} y el máximo de {selectedRelation.yLabel.toLowerCase()} es {formatWithUnit(report.yMax, selectedRelation.yUnit)}.
              </p>
              {report.hasSamples && (
                <p>Muestras usadas: {report.totalSamples}</p>
              )}
            </div>

            <div className="report-block">
              <strong>Patrón observado</strong>
              <p>{report.patternObserved}</p>
              {report.hasEnoughData && report.hasSufficientVariation && (
                <p>
                  Co-ocurrencia alta-alta: {report.highHighPercentage}% de los días analizados.
                </p>
              )}
            </div>

            <div className="report-block">
              <strong>Conclusión</strong>
              <p>{report.conclusion}</p>
            </div>

            <div className="report-block">
              <strong>Qué revisar primero</strong>
              <p>{report.recommendation}</p>
            </div>

            <p className="analysis-test-note">
              El informe es exploratorio y no demuestra causalidad.
            </p>
          </aside>
        </div>
      )}
    </section>
  );
}

export default SpecificKpiRelationsSection;

