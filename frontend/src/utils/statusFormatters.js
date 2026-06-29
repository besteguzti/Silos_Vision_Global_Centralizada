const STATUS_LABELS = {
  GREEN: "Correcto",
  YELLOW: "Advertencia",
  RED: "Crítico",
  NO_DATA: "Sin datos",
  STALE: "Datos obsoletos",
  HEALTHY: "Correcto",
  DEGRADED: "Advertencia",
  INCIDENT: "Crítico",
  WARNING: "Aviso",
  CRITICAL: "Crítico",
  UNKNOWN: "Sin datos",
  NEUTRAL: "Informativo",
  neutral: "Informativo"
};

const PRIORITY_LABELS = {
  HIGH: "Alta",
  MEDIUM: "Media",
  LOW: "Baja"
};

const IMPACT_LABELS = {
  HIGH: "Alto",
  MODERATE: "Moderado",
  MEDIUM: "Medio",
  LOW: "Bajo"
};

const TREND_LABELS = {
  WORSENING: "Empeorando",
  IMPROVING: "Mejorando",
  STABLE: "Estable",
  UNKNOWN: "Sin datos"
};

export function formatStatus(status) {
  return formatValue(status, STATUS_LABELS);
}

export function formatDataStatus(status) {
  return formatValue(status, STATUS_LABELS);
}

export function formatPriority(priority) {
  return formatValue(priority, PRIORITY_LABELS);
}

export function formatImpactLevel(impactLevel) {
  return formatValue(impactLevel, IMPACT_LABELS);
}

export function formatTrend(trend) {
  return formatValue(trend, TREND_LABELS);
}

function formatValue(value, labels) {
  if (value === null || value === undefined || value === "") {
    return "Sin datos";
  }

  const normalized = String(value).trim();
  return labels[normalized] ?? labels[normalized.toUpperCase()] ?? normalized;
}

