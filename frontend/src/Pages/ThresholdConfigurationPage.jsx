import { useEffect, useMemo, useState } from "react";

import "../App.css";

import { API_BASE_URL } from "../config/api";

function ThresholdConfigurationPage() {
  const [thresholds, setThresholds] = useState(null);
  const [weights, setWeights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncControl, setSyncControl] = useState(null);
  const [syncControlUpdating, setSyncControlUpdating] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const weightTotal = useMemo(() => {
    if (!weights) {
      return 0;
    }

    return ["aruba", "citrix", "microsoft365", "glpi"]
      .map((key) => Number(weights[key] || 0))
      .reduce((total, value) => total + value, 0);
  }, [weights]);

  const loadConfiguration = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const data = await fetchConfiguration();
      setThresholds(data.thresholds);
      setWeights(data.weights);
      setSyncControl(data.syncControl);
    } catch {
      setError("No se pudo cargar la configuración de umbrales.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;

    fetchConfiguration()
      .then((data) => {
        if (!cancelled) {
          setThresholds(data.thresholds);
          setWeights(data.weights);
          setSyncControl(data.syncControl);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError("No se pudo cargar la configuración de umbrales.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const updateThresholdValue = (sectionKey, valueKey, nextValue) => {
    setThresholds((current) => ({
      ...current,
      sections: current.sections.map((section) => {
        if (section.key !== sectionKey) {
          return section;
        }

        return {
          ...section,
          values: section.values.map((value) => {
            if (value.key !== valueKey) {
              return value;
            }

            return {
              ...value,
              value: normalizeNumberInput(nextValue)
            };
          })
        };
      })
    }));
  };

  const updateWeight = (key, nextValue) => {
    setWeights((current) => ({
      ...current,
      [key]: normalizeNumberInput(nextValue)
    }));
  };

  const saveThresholds = async () => {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/config/thresholds`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(prepareThresholdPayload(thresholds))
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      setThresholds(await response.json());
      setSuccess("Umbrales guardados correctamente.");
    } catch (err) {
      setError(err.message || "No se pudieron guardar los umbrales.");
    } finally {
      setSaving(false);
    }
  };

  const saveWeights = async () => {
    if (weightTotal !== 100) {
      setError("Los pesos globales deben sumar 100.");
      setSuccess(null);
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/config/platform-weights`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(prepareWeightsPayload(weights))
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      setWeights(await response.json());
      setSuccess("Pesos globales guardados correctamente.");
    } catch (err) {
      setError(err.message || "No se pudieron guardar los pesos globales.");
    } finally {
      setSaving(false);
    }
  };

  const resetConfiguration = async () => {
    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/config/thresholds/reset`, {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      await loadConfiguration();
      setSuccess("Configuración restaurada a valores por defecto.");
    } catch (err) {
      setError(err.message || "No se pudo restaurar la configuración.");
    } finally {
      setSaving(false);
    }
  };

  const synchronizePlatforms = async () => {
    setSyncing(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/metrics/sync`, {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = await response.json();
      const platformDetails = formatPlatformResults(data.platforms);

      if (data.status === "OK") {
        setSuccess(
          `${data.message || "Sincronización completada correctamente."} Vuelve al panel principal para ver los datos actualizados.${platformDetails}`
        );
        return;
      }

      if (data.status === "IN_PROGRESS") {
        setError(data.message || "Ya hay una sincronización en curso.");
        return;
      }

      setError(
        `${data.message || "Sincronización completada con errores parciales."}${platformDetails}`
      );
    } catch (err) {
      setError(err.message || "No se pudo lanzar la sincronización manual.");
    } finally {
      setSyncing(false);
    }
  };

  const toggleAutomaticSync = async () => {
    if (!syncControl) {
      return;
    }

    setSyncControlUpdating(true);
    setError(null);
    setSuccess(null);

    const endpoint = syncControl.automaticSyncEnabled
      ? "/api/metrics/sync-control/pause"
      : "/api/metrics/sync-control/resume";

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: "POST"
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = await response.json();
      setSyncControl(data);
      setSuccess(
        data.automaticSyncEnabled
          ? "Sincronizacion automatica activada."
          : "Sincronizacion automatica pausada."
      );
    } catch (err) {
      setError(err.message || "No se pudo cambiar la sincronizacion automatica.");
    } finally {
      setSyncControlUpdating(false);
    }
  };

  if (loading) {
    return (
      <main className="dashboard">
        <p className="loading">Cargando configuración...</p>
      </main>
    );
  }

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Modelo de KPIs</p>
          <h1>Configuración de umbrales</h1>
        </div>
        <div className="header-actions">
          <button
            className="secondary-action"
            type="button"
            onClick={toggleAutomaticSync}
            disabled={syncControlUpdating || !syncControl}
          >
            {syncControlUpdating
              ? "Actualizando..."
              : syncControl?.automaticSyncEnabled
              ? "Pausar sincronizacion automatica"
              : "Iniciar sincronizacion automatica"}
          </button>
          <button
            className="secondary-action"
            type="button"
            onClick={synchronizePlatforms}
            disabled={saving || syncing}
          >
            {syncing ? "Sincronizando..." : "Sincronizar plataformas"}
          </button>
          <button
            className="secondary-action"
            type="button"
            onClick={resetConfiguration}
            disabled={saving || syncing}
          >
            Restaurar valores por defecto
          </button>
        </div>
      </header>

      {syncControl && (
        <section className="alert config-note">
          Sincronizacion automatica:{" "}
          {syncControl.automaticSyncEnabled ? "Activa" : "Pausada"}
        </section>
      )}

      <section className="alert config-note">
        Esta pantalla modifica solo la configuración del modelo de scoring. No cambia datos reales,
        datos simulados ni snapshots historicos.
      </section>

      {error && <section className="alert">{error}</section>}
      {success && <section className="success-message">{success}</section>}

      {weights && (
        <section className="dashboard-section config-section">
          <div className="config-section-header">
            <div>
              <h2>Pesos globales</h2>
              <p>
                Pesos usados para calcular el KPI Estado global. La suma debe ser 100.
              </p>
            </div>
            <button type="button" onClick={saveWeights} disabled={saving || weightTotal !== 100}>
              Guardar pesos
            </button>
          </div>

          <div className="config-grid">
            <NumericField label="Aruba" unit="%" value={weights.aruba} onChange={(value) => updateWeight("aruba", value)} />
            <NumericField label="Citrix" unit="%" value={weights.citrix} onChange={(value) => updateWeight("citrix", value)} />
            <NumericField label="Microsoft 365" unit="%" value={weights.microsoft365} onChange={(value) => updateWeight("microsoft365", value)} />
            <NumericField label="GLPI" unit="%" value={weights.glpi} onChange={(value) => updateWeight("glpi", value)} />
          </div>

          <p className={weightTotal === 100 ? "config-total ok" : "config-total warning"}>
            Total pesos: {weightTotal} %
          </p>
        </section>
      )}

      {thresholds?.sections?.map((section) => (
        <section className="dashboard-section config-section" key={section.key}>
          <div className="config-section-header">
            <div>
              <h2>{section.title}</h2>
              <p>{section.description}</p>
            </div>
            <button type="button" onClick={saveThresholds} disabled={saving}>
              Guardar umbrales
            </button>
          </div>

          <div className="config-grid">
            {section.values.map((value) => (
              <NumericField
                key={value.key}
                valueKey={value.key}
                label={value.label}
                unit={value.unit}
                value={value.value}
                description={value.description}
                defaultValue={value.defaultValue}
                onChange={(nextValue) => updateThresholdValue(section.key, value.key, nextValue)}
              />
            ))}
          </div>
        </section>
      ))}
    </main>
  );
}

function NumericField({ valueKey, label, value, unit, description, defaultValue, onChange }) {
  const limits = numericFieldLimits(valueKey);

  return (
    <label className="config-field">
      <span>{label}</span>
      <div className="config-input-row">
        <input
          type="number"
          min={limits.min}
          max={limits.max}
          step="1"
          value={value ?? ""}
          onChange={(event) => onChange(event.target.value)}
        />
        {unit && <strong>{unit}</strong>}
      </div>
      {description && <small>{description}</small>}
      {defaultValue !== undefined && defaultValue !== null && (
        <small>Por defecto: {defaultValue}{unit ? ` ${unit}` : ""}</small>
      )}
    </label>
  );
}

function numericFieldLimits(valueKey) {
  if (valueKey === "aruba.inactiveApDaysThreshold") {
    return { min: 1, max: 365 };
  }

  return { min: 0, max: undefined };
}

function normalizeNumberInput(value) {
  if (value === "") {
    return "";
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? "" : parsed;
}

function prepareThresholdPayload(thresholds) {
  return {
    sections: thresholds.sections.map((section) => ({
      ...section,
      values: section.values.map((value) => ({
        ...value,
        value: value.value === "" ? null : Number(value.value)
      }))
    }))
  };
}

function prepareWeightsPayload(weights) {
  return {
    aruba: Number(weights.aruba),
    citrix: Number(weights.citrix),
    microsoft365: Number(weights.microsoft365),
    glpi: Number(weights.glpi)
  };
}

async function readError(response) {
  try {
    const data = await response.json();
    return data.message || data.error || "La configuración no es valida.";
  } catch {
    return "La configuración no es valida.";
  }
}

function formatPlatformResults(platforms = []) {
  if (!platforms.length) {
    return "";
  }

  const details = platforms
    .map((platform) => {
      const message = platform.message ? ` (${platform.message})` : "";
      return `${platform.name}: ${platform.status}${message}`;
    })
    .join(" | ");

  return ` Detalle: ${details}`;
}

async function fetchConfiguration() {
  const [thresholdResponse, weightResponse, syncControlResponse] = await Promise.all([
    fetch(`${API_BASE_URL}/api/config/thresholds`),
    fetch(`${API_BASE_URL}/api/config/platform-weights`),
    fetch(`${API_BASE_URL}/api/metrics/sync-control`)
  ]);

  if (!thresholdResponse.ok || !weightResponse.ok || !syncControlResponse.ok) {
    throw new Error("No se pudo cargar la configuración.");
  }

  return {
    thresholds: await thresholdResponse.json(),
    weights: await weightResponse.json(),
    syncControl: await syncControlResponse.json()
  };
}

export default ThresholdConfigurationPage;

