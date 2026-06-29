import { useEffect, useState } from "react";

import "../App.css";

import KpiCard from "../components/KpiCard";
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from "../config/api";
import { formatDataStatus, formatStatus } from "../utils/statusFormatters";

const glpiKpiInfo = {
  openTickets: {
    description:
      "Representa el número total de tickets abiertos en el entorno GLPI simulado.",
    algorithm:
      "Se genera dinámicamente en GlpiService. De 0 a 100 tickets es verde, de 101 a 200 es amarillo y 201 o más es rojo.",
    interpretation:
      "Cuando sube, hay más trabajo pendiente y puede hacer falta refuerzo operativo."
  },
  criticalOpenTickets: {
    description:
      "Indica tickets abiertos clasificados como críticos.",
    algorithm:
      "Se genera dinámicamente en GlpiService manteniendo coherencia con el total de tickets abiertos. 0 es verde, de 1 a 10 es amarillo y más de 10 es rojo.",
    interpretation:
      "Cualquier valor mayor que cero requiere atención prioritaria por posible impacto en servicio."
  },
  slaBreachedTickets: {
    description:
      "Cuenta tickets que han superado el tiempo objetivo de resolución o atención.",
    algorithm:
      "Se genera dinámicamente en GlpiService. 0 es verde, de 1 a 10 es amarillo y más de 10 es rojo.",
    interpretation:
      "Cuando sube, conviene revisar compromisos de servicio y posibles retrasos."
  },
  averageResolutionHours: {
    description:
      "Indica el tiempo medio de resolución de tickets, expresado en horas.",
    algorithm:
      "Se genera dinámicamente en GlpiService como indicador informativo de rendimiento operativo.",
    interpretation:
      "Cuando sube, puede haber lentitud en la resolución o saturación del equipo de soporte."
  },
  operationalBacklog: {
    description:
      "Representa la carga operativa pendiente acumulada.",
    algorithm:
      "Se genera dinámicamente en GlpiService a partir del comportamiento simulado de tickets.",
    interpretation:
      "Un backlog elevado indica más trabajo acumulado y mayor presión operativa."
  },
  createdToday: {
    description:
      "Indica tickets creados durante el día actual en la simulación.",
    algorithm:
      "Se genera dinámicamente en GlpiService como actividad diaria entrante.",
    interpretation:
      "Cuando sube, el soporte ha recibido más trabajo durante el día."
  },
  closedToday: {
    description:
      "Indica tickets cerrados durante el día actual.",
    algorithm:
      "Se genera dinámicamente en GlpiService y se usa para calcular el porcentaje de cierre diario. Si el cierre es igual o superior al 50% es verde; por debajo del 50% es amarillo.",
    interpretation:
      "Si es menor que los tickets creados hoy, puede crecer el trabajo pendiente diario."
  },
  createdThisWeek: {
    description:
      "Indica tickets creados durante la semana actual.",
    algorithm:
      "Se genera dinámicamente en GlpiService como actividad semanal entrante.",
    interpretation:
      "Ayuda a detectar semanas con más entrada de tickets y posibles picos de actividad."
  },
  closedThisWeek: {
    description:
      "Indica tickets cerrados durante la semana actual.",
    algorithm:
      "Se genera dinámicamente en GlpiService y se usa para calcular el porcentaje de cierre semanal. Si el cierre es igual o superior al 50% es verde; por debajo del 50% es amarillo.",
    interpretation:
      "Si queda por debajo de los creados en la semana, el backlog puede aumentar."
  }
};

function GlpiPage() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadGlpiDashboard = () => {
    // GLPI llega como consecuencia operativa calculada en backend; React solo
    // muestra el resumen y sus estados.
    fetch(`${API_BASE_URL}/glpi/summary`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("No se pudo cargar el resumen GLPI");
        }

        return response.json();
      })
      .then((data) => {
        setSummary(data);
        setError(null);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error cargando GLPI:", error);
        setError("No se pudo conectar con el backend de GLPI.");
        setSummary(null);
        setLoading(false);
      });
  };

  useEffect(() => {
    loadGlpiDashboard();

    const interval = setInterval(() => {
      loadGlpiDashboard();
    }, FRONTEND_REFRESH_INTERVAL_MS);

    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <main className="dashboard">
        <h1>GLPI</h1>
        <p className="loading">Cargando datos GLPI...</p>
      </main>
    );
  }

  if (error || !summary) {
    return (
      <main className="dashboard">
        <h1>GLPI</h1>
        <p className="loading">{error ?? "No se han podido cargar los datos de GLPI."}</p>
      </main>
    );
  }

  const glpiHealthDetails = summary.glpiHealthDetails;
  const glpiHealth = glpiHealthDetails?.color ?? "UNKNOWN";
  const glpiReasons = glpiHealthDetails?.reasons ?? [];
  const indicatorStatus = (name) =>
    findIndicatorStatus(glpiHealthDetails?.indicators, name);

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Monitorización GLPI</p>
          <h1>GLPI</h1>
        </div>
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
      </header>

      <section className={`status status-${glpiHealth.toLowerCase()}`}>
        <div className="status-main">
          <span>Índice de salud GLPI</span>
          <strong>Afección: {glpiHealthDetails?.percentage ?? 0} %</strong>
          <p>Estado: {formatStatus(glpiHealth)}</p>
        </div>

        <div className="status-reasons">
          <span>Motivos</span>
          {glpiReasons.length > 0 ? (
            <ul>
              {glpiReasons.slice(0, 4).map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          ) : (
            <p>Sin motivos activos</p>
          )}
        </div>
      </section>

      <p>
        Supervisión simulada de tickets, actividad semanal, SLA y backlog
        operativo.
      </p>

      <section className="dashboard-section">
      <h2>Operación</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Tickets abiertos"
          value={summary.openTickets}
          status={indicatorStatus("Tickets abiertos")}
          info={glpiKpiInfo.openTickets}
        />

        <KpiCard
          title="Tickets críticos abiertos"
          value={summary.criticalOpenTickets}
          status={indicatorStatus("Tickets abiertos críticos")}
          info={glpiKpiInfo.criticalOpenTickets}
        />

        <KpiCard
          title="Tickets vencidos SLA"
          value={summary.slaBreachedTickets}
          status={indicatorStatus("Tickets vencidos SLA")}
          info={glpiKpiInfo.slaBreachedTickets}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Rendimiento</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Tiempo medio resolución"
          value={`${summary.averageResolutionHours}h`}
          status="neutral"
          info={glpiKpiInfo.averageResolutionHours}
        />

        <KpiCard
          title="Backlog operativo"
          value={summary.operationalBacklog}
          status="neutral"
          info={glpiKpiInfo.operationalBacklog}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Actividad diaria</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Tickets creados hoy"
          value={summary.createdToday}
          status="neutral"
          info={glpiKpiInfo.createdToday}
        />

        <KpiCard
          title="Tickets cerrados hoy"
          value={summary.closedToday}
          status={indicatorStatus("Porcentaje de tickets cerrados")}
          info={glpiKpiInfo.closedToday}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Actividad semanal</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Tickets creados semana"
          value={summary.createdThisWeek}
          status="neutral"
          info={glpiKpiInfo.createdThisWeek}
        />

        <KpiCard
          title="Tickets cerrados semana"
          value={summary.closedThisWeek}
          status={indicatorStatus("Porcentaje de tickets cerrados semana")}
          info={glpiKpiInfo.closedThisWeek}
        />
      </div>
      </section>
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

export default GlpiPage;




