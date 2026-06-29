import { useMemo, useState } from "react";

import AnalysisEmptyState from "./AnalysisEmptyState";
import PeriodSelector from "./PeriodSelector";
import { formatValue } from "./analysisUtils";

const CHART_WIDTH = 780;
const CHART_HEIGHT = 360;
const CHART_PADDING_LEFT = 62;
const CHART_PADDING_RIGHT = 28;
const CHART_PADDING_TOP = 28;
const CHART_PADDING_BOTTOM = 58;
const Y_AXIS_MIN = 0;
const Y_AXIS_MAX = 100;
const Y_AXIS_TICKS = [0, 33, 66, 100];
const REFERENCE_TICKS = [33, 66];
const MIN_POINTS = 2;

const SERIES = [
  {
    key: "technicalDegradation",
    label: "Degradación técnica",
    color: "#315c9c"
  },
  {
    key: "glpiOperationalPressure",
    label: "Presión operativa GLPI",
    color: "#b42318"
  },
  {
    key: "userImpact",
    label: "Impacto en usuarios",
    color: "#7c3aed"
  },
  {
    key: "aruba",
    label: "Afectación Aruba",
    color: "#0f766e"
  },
  {
    key: "citrix",
    label: "Afectación Citrix",
    color: "#d97706"
  },
  {
    key: "globalStatus",
    label: "Estado global",
    color: "#475569"
  }
];

const INITIAL_SELECTED_SERIES_KEYS = SERIES.map((serie) => serie.key);

function numericValue(value) {
  const parsed = Number(value);

  return Number.isFinite(parsed) ? parsed : null;
}

function formatPercent(value) {
  const parsed = numericValue(value);

  return parsed === null ? "sin datos" : `${formatValue(parsed)} %`;
}

function formatDate(timestamp) {
  if (!timestamp) {
    return "Sin fecha";
  }

  return new Date(timestamp).toLocaleDateString("es-ES", {
    day: "2-digit",
    month: "2-digit"
  });
}

function normalizeTimeline(timeline = []) {
  return timeline
    .filter((point) => point?.timestamp)
    .map((point) => ({
      ...point,
      aruba: numericValue(point.aruba),
      citrix: numericValue(point.citrix),
      technicalDegradation: numericValue(point.technicalDegradation),
      glpiOperationalPressure: numericValue(point.glpiOperationalPressure),
      userImpact: numericValue(point.userImpact),
      globalStatus: numericValue(point.globalStatus),
      generatedScenario: Boolean(point.generatedScenario)
    }))
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
}

function clampPercent(value) {
  return Math.max(Y_AXIS_MIN, Math.min(Y_AXIS_MAX, value));
}

function buildStats(points, serie) {
  const values = points
    .map((point, index) => ({
      index,
      timestamp: point.timestamp,
      value: point[serie.key]
    }))
    .filter((point) => Number.isFinite(point.value));

  if (values.length < MIN_POINTS) {
    return null;
  }

  const initial = values[0];
  const final = values[values.length - 1];
  const max = values.reduce(
    (candidate, point) => (point.value > candidate.value ? point : candidate),
    values[0]
  );
  const referenceIndex = Math.max(0, values.length - Math.max(2, Math.round(values.length * 0.15)));
  const finalReference = values[referenceIndex];
  const finalDifference = final.value - finalReference.value;
  const recoveryFromMax = max.value - final.value;

  let trend = "estable";

  if (finalDifference >= 10) {
    trend = "empeorando";
  } else if (finalDifference <= -10) {
    trend = "mejorando";
  }

  return {
    key: serie.key,
    label: serie.label,
    color: serie.color,
    initial: initial.value,
    max: max.value,
    final: final.value,
    peakTimestamp: max.timestamp,
    peakIndex: max.index,
    trend,
    recoveryFromMax
  };
}

function peakPeriodLabel(peakIndex, totalPoints) {
  if (totalPoints <= 1) {
    return "periodo analizado";
  }

  const position = peakIndex / (totalPoints - 1);

  if (position < 0.33) {
    return "parte inicial del periodo";
  }

  if (position < 0.66) {
    return "parte central del periodo";
  }

  return "parte final del periodo";
}

function buildReading(mainStats, points, periodLabel) {
  if (!mainStats) {
    return "No hay datos temporales suficientes para interpretar una evolución clara del periodo.";
  }

  const peakLabel = peakPeriodLabel(mainStats.peakIndex, points.length);
  const recoveryText = mainStats.recoveryFromMax >= 10
    ? "En la parte final se aprecia una recuperación respecto al máximo alcanzado."
    : "La parte final no muestra una recuperación clara respecto al máximo del periodo.";

  return `Durante el periodo ${periodLabel} se observa una evolución temporal con valor inicial ${formatPercent(mainStats.initial)}, máximo ${formatPercent(mainStats.max)} y valor final ${formatPercent(mainStats.final)} en ${mainStats.label.toLowerCase()}. El pico se sitúa en la ${peakLabel}. La tendencia final aparece ${mainStats.trend}. ${recoveryText}`;
}

function TimelineChart({ points, visibleSeries }) {
  const drawableWidth = CHART_WIDTH - CHART_PADDING_LEFT - CHART_PADDING_RIGHT;
  const drawableHeight = CHART_HEIGHT - CHART_PADDING_TOP - CHART_PADDING_BOTTOM;

  const scaleX = (index) => {
    if (points.length <= 1) {
      return CHART_PADDING_LEFT;
    }

    return CHART_PADDING_LEFT + (index / (points.length - 1)) * drawableWidth;
  };

  const scaleY = (value) =>
    CHART_PADDING_TOP
    + ((Y_AXIS_MAX - clampPercent(value)) / (Y_AXIS_MAX - Y_AXIS_MIN))
      * drawableHeight;

  const xTickIndexes = Array.from(
    new Set([
      0,
      Math.floor((points.length - 1) / 2),
      points.length - 1
    ])
  );

  return (
    <svg
      className="analysis-chart"
      viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
      role="img"
      aria-label="Evolución temporal de KPIs transversales"
    >
      <line
        x1={CHART_PADDING_LEFT}
        y1={CHART_HEIGHT - CHART_PADDING_BOTTOM}
        x2={CHART_WIDTH - CHART_PADDING_RIGHT}
        y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
      />
      <line
        x1={CHART_PADDING_LEFT}
        y1={CHART_PADDING_TOP}
        x2={CHART_PADDING_LEFT}
        y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
      />

      {Y_AXIS_TICKS.map((tick) => (
        <g key={`timeline-y-${tick}`}>
          <line
            className={REFERENCE_TICKS.includes(tick) ? "analysis-reference-line" : "analysis-grid-line"}
            x1={CHART_PADDING_LEFT}
            y1={scaleY(tick)}
            x2={CHART_WIDTH - CHART_PADDING_RIGHT}
            y2={scaleY(tick)}
          />
          <text
            x={CHART_PADDING_LEFT - 12}
            y={scaleY(tick) + 4}
            textAnchor="end"
          >
            {tick} %
          </text>
        </g>
      ))}

      {xTickIndexes.map((index) => (
        <g key={`timeline-x-${index}`}>
          <line
            className="analysis-grid-line"
            x1={scaleX(index)}
            y1={CHART_PADDING_TOP}
            x2={scaleX(index)}
            y2={CHART_HEIGHT - CHART_PADDING_BOTTOM}
          />
          <text
            x={scaleX(index)}
            y={CHART_HEIGHT - CHART_PADDING_BOTTOM + 26}
            textAnchor="middle"
          >
            {formatDate(points[index]?.timestamp)}
          </text>
        </g>
      ))}

      <text x={CHART_WIDTH / 2} y={CHART_HEIGHT - 14} textAnchor="middle">
        Días del periodo
      </text>
      <text
        x={18}
        y={CHART_HEIGHT / 2}
        textAnchor="middle"
        transform={`rotate(-90 18 ${CHART_HEIGHT / 2})`}
      >
        Valor normalizado 0-100 %
      </text>

      {visibleSeries.map((serie) => {
        const seriePoints = points
          .map((point, index) => ({
            x: scaleX(index),
            y: scaleY(point[serie.key]),
            raw: point
          }))
          .filter((point) => Number.isFinite(point.raw[serie.key]));

        if (seriePoints.length < MIN_POINTS) {
          return null;
        }

        return (
          <g key={serie.key}>
            <polyline
              className="analysis-timeline-line"
              points={seriePoints.map((point) => `${point.x},${point.y}`).join(" ")}
              style={{ stroke: serie.color }}
            />
            {seriePoints.map((point, index) => (
              <circle
                key={`${serie.key}-${point.raw.timestamp}-${index}`}
                className="analysis-timeline-dot"
                cx={point.x}
                cy={point.y}
                r="3"
                style={{ fill: serie.color }}
              >
                <title>
                  {`${formatDate(point.raw.timestamp)} | ${serie.label}: ${formatPercent(point.raw[serie.key])}${point.raw.generatedScenario ? " | escenario generado" : ""}`}
                </title>
              </circle>
            ))}
          </g>
        );
      })}
    </svg>
  );
}

function SeriesToggleControls({
  availableSeries,
  selectedSeriesKeys,
  onToggle,
  warning
}) {
  const selectedAvailableCount = availableSeries.filter((serie) =>
    selectedSeriesKeys.includes(serie.key)
  ).length;

  return (
    <div
      className="timeline-series-controls"
      role="group"
      aria-label="Series temporales visibles"
    >
      <span className="timeline-series-controls-title">KPIs visibles</span>

      <div className="timeline-series-toggle-list">
        {availableSeries.map((serie) => {
          const active = selectedSeriesKeys.includes(serie.key);
          const disabled = active && selectedAvailableCount <= 1;

          return (
            <button
              key={serie.key}
              type="button"
              className={`timeline-series-toggle${active ? " active" : ""}`}
              style={{ "--series-color": serie.color }}
              aria-pressed={active}
              disabled={disabled}
              onClick={() => onToggle(serie.key)}
            >
              <span className="timeline-series-toggle-dot" />
              {serie.label}
            </button>
          );
        })}
      </div>

      {warning && <p className="timeline-series-warning">{warning}</p>}
    </div>
  );
}

function TemporalTrendSection({ timeline, periods, selectedPeriod, onPeriodChange }) {
  const [selectedSeriesKeys, setSelectedSeriesKeys] = useState(INITIAL_SELECTED_SERIES_KEYS);
  const [toggleWarning, setToggleWarning] = useState(null);
  const points = useMemo(() => normalizeTimeline(timeline), [timeline]);
  const availableSeries = useMemo(
    () =>
      SERIES.filter((serie) =>
        points.some((point) => Number.isFinite(point[serie.key]))
      ),
    [points]
  );
  const availableSeriesKeys = useMemo(
    () => availableSeries.map((serie) => serie.key),
    [availableSeries]
  );
  const effectiveSelectedSeriesKeys = useMemo(() => {
    const selectedAvailableKeys = selectedSeriesKeys.filter((key) =>
      availableSeriesKeys.includes(key)
    );

    if (selectedAvailableKeys.length > 0 || availableSeriesKeys.length === 0) {
      return selectedAvailableKeys;
    }

    return availableSeriesKeys;
  }, [availableSeriesKeys, selectedSeriesKeys]);
  const visibleSeries = useMemo(
    () =>
      availableSeries.filter((serie) =>
        effectiveSelectedSeriesKeys.includes(serie.key)
      ),
    [availableSeries, effectiveSelectedSeriesKeys]
  );
  const stats = useMemo(
    () =>
      visibleSeries
        .map((serie) => buildStats(points, serie))
        .filter(Boolean),
    [points, visibleSeries]
  );
  const periodLabel =
    periods?.find((item) => item.value === selectedPeriod)?.label ?? selectedPeriod;
  const mainStats =
    stats.find((item) => item.label === "Degradación técnica")
    ?? stats.find((item) => item.label === "Estado global")
    ?? stats[0];
  const reading = buildReading(mainStats, points, periodLabel);
  const hasGeneratedData = points.some((point) => point.generatedScenario);
  const hasEnoughData = points.length >= MIN_POINTS && visibleSeries.length > 0;
  const handleSeriesToggle = (serieKey) => {
    const isSelected = effectiveSelectedSeriesKeys.includes(serieKey);

    if (isSelected && effectiveSelectedSeriesKeys.length <= 1) {
      setToggleWarning("Debe quedar al menos un KPI seleccionado.");
      return;
    }

    setToggleWarning(null);
    setSelectedSeriesKeys((currentKeys) => {
      const currentAvailableKeys = currentKeys.filter((key) =>
        availableSeriesKeys.includes(key)
      );
      const baseKeys = currentAvailableKeys.length > 0
        ? currentAvailableKeys
        : availableSeriesKeys;

      if (isSelected) {
        return baseKeys.filter((key) => key !== serieKey);
      }

      return Array.from(new Set([...baseKeys, serieKey]));
    });
  };

  return (
    <section className="dashboard-section">
      <div className="dashboard-header">
        <div>
          <h2>Evolución temporal del periodo</h2>
          <p className="section-subtitle">
            Esta gráfica muestra la secuencia cronológica de los snapshots del periodo.
            Permite leer fases de normalidad, degradación, pico de incidencia y recuperación
            progresiva sin perder el orden temporal.
          </p>
        </div>

        <div className="analysis-actions">
          {periods && (
            <PeriodSelector
              periods={periods}
              selectedPeriod={selectedPeriod}
              onChange={onPeriodChange}
            />
          )}
        </div>
      </div>

      {!hasEnoughData ? (
        <AnalysisEmptyState message="Sin datos temporales suficientes para generar la evolución del periodo." />
      ) : (
        <div className="analysis-layout">
          <div className="analysis-chart-card">
            <p className="analysis-chart-explanation">
              Las líneas usan escala fija de 0 a 100 para comparar degradación técnica,
              presión operativa, impacto en usuarios y afección de plataformas.
            </p>

            <SeriesToggleControls
              availableSeries={availableSeries}
              selectedSeriesKeys={effectiveSelectedSeriesKeys}
              onToggle={handleSeriesToggle}
              warning={toggleWarning}
            />

            <TimelineChart points={points} visibleSeries={visibleSeries} />

            <div className="timeline-legend">
              {visibleSeries.map((serie) => (
                <span key={serie.key} className="timeline-legend-item">
                  <span
                    className="timeline-legend-color"
                    style={{ backgroundColor: serie.color }}
                  />
                  {serie.label}
                </span>
              ))}
            </div>

            {hasGeneratedData && (
              <p className="analysis-test-note">
                Parte de los puntos proceden de escenarios generados o de prueba.
              </p>
            )}
          </div>

          <aside className="analysis-summary">
            <h2>Lectura temporal</h2>
            <p>{reading}</p>

            <div className="timeline-report-grid">
              {stats.map((item) => (
                <div key={item.label} className="timeline-report-card">
                  <strong>{item.label}</strong>
                  <span>Inicial: {formatPercent(item.initial)}</span>
                  <span>Máximo: {formatPercent(item.max)}</span>
                  <span>Final: {formatPercent(item.final)}</span>
                  <span>Tendencia final: {item.trend}</span>
                </div>
              ))}
            </div>

            {mainStats && (
              <div className="report-block">
                <strong>Pico y recuperación</strong>
                <p>
                  El pico principal se produce el {formatDate(mainStats.peakTimestamp)}.
                  La recuperación frente al máximo es de {formatPercent(mainStats.recoveryFromMax)}.
                </p>
              </div>
            )}
          </aside>
        </div>
      )}
    </section>
  );
}

export default TemporalTrendSection;

