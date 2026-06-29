export function formatValue(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "Sin datos";
  }

  return Number(value).toLocaleString(undefined, {
    maximumFractionDigits: 2
  });
}

export function formatOptionalPercent(value) {
  if (value === null || value === undefined) {
    return "sin datos";
  }

  return `${value}%`;
}

export function formatOptionalSigned(value) {
  if (value === null || value === undefined) {
    return "sin datos";
  }

  return `${value >= 0 ? "+" : ""}${value} puntos`;
}

export function cssTone(status) {
  // El tono visual viene del estado backend; NO_DATA/STALE no se tratan como OK.
  const normalized =
    status === null || status === undefined
      ? "NO_DATA"
      : String(status).trim().toUpperCase();

  if (normalized === "") {
    return "warning";
  }

  if (
    normalized === "RED"
    || normalized === "DANGER"
    || normalized === "NO_DATA"
  ) {
    return "danger";
  }

  if (
    normalized === "YELLOW"
    || normalized === "WARNING"
    || normalized === "UNKNOWN"
    || normalized === "STALE"
  ) {
    return "warning";
  }

  return "ok";
}
