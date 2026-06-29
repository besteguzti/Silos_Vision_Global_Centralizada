import { useEffect, useState } from "react";

import "../App.css";

import KpiCard from "../components/KpiCard";
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from "../config/api";
import { formatDataStatus, formatStatus } from "../utils/statusFormatters";

const citrixKpiInfo = {
  activeSessions: {
    description:
      "Representa el número de sesiones activas simuladas en el entorno Citrix. Aproxima la carga actual de usuarios conectados a escritorios o aplicaciones virtuales.",
    algorithm:
      "En esta fase se genera de forma simulada en CitrixService mediante un valor dinámico entre 250 y 449 sesiones. En una integración real se obtendría desde Citrix Monitor o Citrix Cloud.",
    interpretation:
      "Sirve para ver cuánta actividad hay en Citrix. No representa usuarios únicos, sino sesiones activas observadas."
  },
  activeLicenses: {
    description:
      "Indica el número de licencias activas o asignadas en el entorno Citrix simulado.",
    algorithm:
      "El valor se genera dinámicamente en CitrixService dentro de un rango controlado para simular disponibilidad de licenciamiento.",
    interpretation:
      "Permite valorar la capacidad disponible para usuarios Citrix. Un consumo elevado podría anticipar problemas de capacidad o necesidad de ampliación."
  },
  deliveryControllers: {
    description:
      "Indica cuántos Delivery Controllers están disponibles respecto al total configurado. Son componentes críticos para gestionar sesiones y publicar recursos Citrix.",
    algorithm:
      "Se calcula como availableDeliveryControllers / totalDeliveryControllers. De 67% a 100% es correcto, de 34% a 66% es advertencia y de 0% a 33% es crítico.",
    interpretation:
      "Si todos los controllers están disponibles, el estado es correcto. Si alguno no está disponible, se considera una degradación importante del servicio."
  },
  disconnectedSessions: {
    description:
      "Representa sesiones que permanecen abiertas pero sin usuario conectado activamente.",
    algorithm:
      "Se genera de forma dinámica en CitrixService dentro de un rango controlado.",
    interpretation:
      "Un número elevado puede indicar sesiones huérfanas, consumo innecesario de recursos o necesidad de revisar políticas de cierre de sesión."
  },
  averageLogonDuration: {
    description:
      "Indica el tiempo medio de inicio de sesión en Citrix, expresado en segundos.",
    algorithm:
      "Se genera dinámicamente en CitrixService. De 0 a 20 segundos es verde, más de 20 y hasta 60 segundos es amarillo y más de 60 segundos es rojo.",
    interpretation:
      "Un tiempo alto puede indicar lentitud en perfiles, scripts de inicio, carga de servidores o problemas de infraestructura."
  },
  serverLoad: {
    description:
      "Representa la carga media simulada de los servidores Citrix.",
    algorithm:
      "Se genera en CitrixService como porcentaje dinámico. Menos de 80% es verde, de 80% a 89% es amarillo y 90% o más es rojo.",
    interpretation:
      "Una carga elevada puede afectar al rendimiento de las sesiones y anticipar saturación de la plataforma."
  },
  failedLogons: {
    description:
      "Indica intentos de inicio de sesión fallidos en el entorno Citrix simulado.",
    algorithm:
      "Se genera dinámicamente en CitrixService. De 0 a 5 errores es verde, de 6 a 20 es amarillo y más de 20 es rojo.",
    interpretation:
      "Un número elevado puede indicar problemas de autenticación, disponibilidad o acceso a recursos publicados."
  },
  citrixOpenTickets: {
    description:
      "Tickets abiertos GLPI asociados a Citrix.",
    algorithm:
      "El backend lee el último snapshot GLPI y devuelve citrixOpenTickets dentro del resumen Citrix.",
    interpretation:
      "Permite conectar señales técnicas de Citrix con carga operativa clasificada como Citrix, sin afirmar causalidad."
  },
  citrixHealth: {
    description:
      "Resume el estado general del entorno Citrix mediante un semáforo: correcto, advertencia o crítico.",
    algorithm:
      "El índice suma afecciones parciales de Citrix. El estado superior se calcula con la afección total: 0-33% correcto, 34-66% advertencia y 67-100% crítico. Las tarjetas internas mantienen su propio estado.",
    interpretation:
      "Correcto indica funcionamiento normal, advertencia indica degradación moderada y crítico indica una situación que requiere revisión."
  }
};

function CitrixPage() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadCitrixDashboard = () => {
    // Citrix se muestra desde el último snapshot persistido; el frontend no
    // recalcula el índice de salud.
    fetch(`${API_BASE_URL}/citrix/summary`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("No se pudo cargar el resumen Citrix");
        }

        return response.json();
      })
      .then((data) => {
        setSummary(data);
        setError(null);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error cargando Citrix:", error);
        setError("No se pudo conectar con el backend de Citrix.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadCitrixDashboard();

    const interval = setInterval(() => {
      loadCitrixDashboard();
    }, FRONTEND_REFRESH_INTERVAL_MS);

    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <main className="dashboard">
        <h1>Citrix</h1>
        <p className="loading">Cargando datos Citrix...</p>
      </main>
    );
  }

  const citrixHealthDetails = summary?.citrixHealthDetails;
  const citrixHealth = citrixHealthDetails?.color ?? "UNKNOWN";
  const citrixReasons = citrixHealthDetails?.reasons ?? [];
  const indicatorStatus = (name) =>
    findIndicatorStatus(citrixHealthDetails?.indicators, name);

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Monitorización Citrix</p>
          <h1>Citrix</h1>
        </div>

        {summary && (
          <div className="freshness">
          <p className="updated">
            Última actualización: {formatSnapshotDate(summary.lastUpdated)}
          </p>
          <p className="updated">
            Estado de datos:{" "}
            <span className={`freshness-status freshness-status-${(summary.dataStatus ?? "NO_DATA").toLowerCase()}`}>
              {formatDataStatus(summary.dataStatus)}
            </span>
          </p>
          </div>
        )}
      </header>

      {error && (
        <section className="alert" role="alert">
          {error}
        </section>
      )}

      {summary ? (
        <>
        <section className={`status status-${citrixHealth.toLowerCase()}`}>
          <div className="status-main">
            <span>Índice de salud Citrix</span>
            <strong>Afección: {citrixHealthDetails?.percentage ?? 0} %</strong>
          <p>Estado: {formatStatus(citrixHealth)}</p>
          </div>

          <div className="status-reasons">
            <span>Motivos</span>
            {citrixReasons.length > 0 ? (
              <ul>
                {citrixReasons.slice(0, 4).map((reason) => (
                  <li key={reason}>{reason}</li>
                ))}
              </ul>
            ) : (
              <p>Sin motivos activos</p>
            )}
          </div>
        </section>

        <section className="dashboard-section">
          <div className="kpi-grid">
            <KpiCard
              title="Sesiones activas"
              value={summary.activeSessions}
              status={indicatorStatus("Sesiones activas")}
              info={citrixKpiInfo.activeSessions}
            />

            <KpiCard
              title="Licencias activas"
              value={summary.activeLicenses}
              status={indicatorStatus("Licencias activas")}
              info={citrixKpiInfo.activeLicenses}
            />

            <KpiCard
              title="Delivery Controllers disponibles"
              value={`${summary.availableDeliveryControllers}/${summary.totalDeliveryControllers}`}
              status={indicatorStatus("Delivery Controllers disponibles")}
              info={citrixKpiInfo.deliveryControllers}
            />

            <KpiCard
              title="Sesiones desconectadas"
              value={summary.disconnectedSessions}
              status={indicatorStatus("Sesiones desconectadas")}
              info={citrixKpiInfo.disconnectedSessions}
            />

            <KpiCard
              title="Average Logon Duration"
              value={`${summary.averageLogonDurationSeconds}s`}
              status={indicatorStatus("Average Logon Duration")}
              info={citrixKpiInfo.averageLogonDuration}
            />

            <KpiCard
              title="Carga de servidores"
              value={`${summary.serverLoadPercent}%`}
              status={indicatorStatus("Carga de servidores")}
              info={citrixKpiInfo.serverLoad}
            />

            <KpiCard
              title="Errores de inicio"
              value={summary.failedLogons}
              status={indicatorStatus("Errores de inicio")}
              info={citrixKpiInfo.failedLogons}
            />

            <KpiCard
              title="Tickets abiertos Citrix"
              value={summary.citrixOpenTickets}
              status={indicatorStatus("Tickets abiertos Citrix")}
              info={citrixKpiInfo.citrixOpenTickets}
            />

          </div>
        </section>
        </>
      ) : (
        <p className="loading">No se han podido cargar los datos Citrix.</p>
      )}
    </main>
  );
}

function formatSnapshotDate(value) {
  if (!value) {
    return "Sin datos";
  }

  return new Date(value).toLocaleString();
}

function findIndicatorStatus(indicators, name) {
  return indicators?.find((indicator) => indicator.name === name)?.color
    ?? "neutral";
}

export default CitrixPage;




