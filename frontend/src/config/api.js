// Centraliza la URL del backend para
// que las páginas React no dependan
// de valores hardcodeados.
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// El frontend solo relee los endpoints existentes; no dispara sincronizaciones.
export const FRONTEND_REFRESH_INTERVAL_MS =
  10 * 60 * 1000;
