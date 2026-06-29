import { useEffect, useState } from "react";

import "../App.css";

import KpiCard from "../components/KpiCard";
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from "../config/api";
import { formatDataStatus, formatStatus } from "../utils/statusFormatters";

const microsoft365KpiInfo = {
  activeUsers: {
    description:
      "Representa usuarios activos simulados en Microsoft 365.",
    algorithm:
      "Se obtiene como dato dinámico desde Microsoft365Service para simular actividad de usuarios en la plataforma.",
    interpretation:
      "Ayuda a estimar la actividad general. No implica necesariamente usuarios únicos conectados en tiempo real."
  },
  unassignedLicenses: {
    description:
      "Indica licencias disponibles que no están asignadas a usuarios.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service dentro de un rango controlado.",
    interpretation:
      "Un valor bajo puede indicar presión de licenciamiento. Un valor alto puede indicar capacidad disponible o licencias infrautilizadas."
  },
  outlookStatus: {
    description:
      "Muestra el estado simulado del servicio Outlook.",
    algorithm:
      "Microsoft365Service genera un estado dinámico: correcto, degradado o con incidencia.",
    interpretation:
      "Correcto indica funcionamiento normal, degradado indica degradación parcial e incidencia indica una situación relevante."
  },
  teamsStatus: {
    description:
      "Muestra el estado simulado del servicio Teams.",
    algorithm:
      "Microsoft365Service genera un estado dinámico: correcto, degradado o con incidencia.",
    interpretation:
      "Correcto indica funcionamiento normal, degradado indica degradación parcial e incidencia indica una situación relevante."
  },
  sharePointStatus: {
    description:
      "Muestra el estado simulado del servicio SharePoint.",
    algorithm:
      "Microsoft365Service genera un estado dinámico: correcto, degradado o con incidencia.",
    interpretation:
      "Correcto indica funcionamiento normal, degradado indica degradación parcial e incidencia indica una situación relevante."
  },
  nearlyFullMailboxes: {
    description:
      "Indica buzones próximos a quedarse sin capacidad.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service y se marca como advertencia cuando supera el umbral usado por la tarjeta.",
    interpretation:
      "Un valor alto puede anticipar incidencias de recepción o envío de correo por falta de espacio."
  },
  emailsQuarantined: {
    description:
      "Muestra correos simulados retenidos en cuarentena.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service y se considera advertencia cuando el volumen supera 100.",
    interpretation:
      "Un valor alto puede indicar campañas maliciosas, filtros más restrictivos o mayor exposición a correo sospechoso."
  },
  sharePointStoragePercent: {
    description:
      "Representa el porcentaje simulado de almacenamiento usado en SharePoint.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service. Menos del 80% es verde, de 80% a 89% es amarillo y desde 90% es rojo.",
    interpretation:
      "Valores altos indican presión de capacidad y posible necesidad de limpieza o ampliación."
  },
  riskyUsers: {
    description:
      "Cuenta usuarios simulados con señales de riesgo de identidad.",
    algorithm:
      "El backend marca 0 usuarios como verde, de 1 a 9 como amarillo y desde 10 como rojo.",
    interpretation:
      "Un valor mayor que cero requiere revisión de identidad, actividad sospechosa o controles de acceso."
  },
  failedSignIns: {
    description:
      "Indica intentos de inicio de sesión fallidos.",
    algorithm:
      "El backend marca menos de 10 como verde, de 10 a 19 como amarillo y desde 20 como rojo.",
    interpretation:
      "Un valor alto puede indicar errores de usuario, ataques de fuerza bruta o problemas de autenticación."
  },
  usersWithoutMfa: {
    description:
      "Cuenta usuarios simulados sin autenticación multifactor.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service. 0 usuarios es verde, de 1 a 4 es amarillo y desde 5 es rojo.",
    interpretation:
      "Un valor alto aumenta la exposición ante robo de credenciales y debería reducirse."
  },
  appsSecretsExpiringSoon: {
    description:
      "Indica secretos de aplicaciones próximos a caducar.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service y cualquier valor superior a cero se muestra como advertencia.",
    interpretation:
      "Un valor alto puede anticipar interrupciones en integraciones si no se renuevan los secretos."
  },
  unusedApplications: {
    description:
      "Cuenta aplicaciones empresariales simuladas sin uso relevante.",
    algorithm:
      "El backend lo marca como advertencia cuando hay al menos una aplicación sin uso.",
    interpretation:
      "Un valor alto puede indicar sobreconfiguración o aplicaciones que conviene revisar o retirar."
  },
  highPrivilegeApplications: {
    description:
      "Cuenta aplicaciones con permisos elevados.",
    algorithm:
      "El backend lo marca como advertencia cuando hay al menos una aplicación con permisos elevados.",
    interpretation:
      "Un valor alto incrementa el riesgo de seguridad y requiere revisión de permisos concedidos."
  },
  nonCompliantDevices: {
    description:
      "Indica equipos que no cumplen las políticas simuladas de Intune.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service. Hasta 30 equipos es verde, de 31 a 50 es amarillo y más de 50 es rojo.",
    interpretation:
      "Un valor alto puede implicar dispositivos con configuración insegura o fuera de estándar."
  },
  microsoft365OpenTickets: {
    description:
      "Tickets abiertos GLPI asociados a Microsoft 365.",
    algorithm:
      "El backend lee el último snapshot GLPI y devuelve microsoft365OpenTickets dentro del resumen Microsoft 365.",
    interpretation:
      "Permite relacionar señales técnicas de Microsoft 365 con trabajo operativo clasificado como Microsoft 365, sin afirmar causalidad."
  },
  outdatedWindowsDevices: {
    description:
      "Cuenta dispositivos Windows simulados con versiones desactualizadas.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service. 0 equipos es verde y cualquier valor superior a 0 se considera amarillo.",
    interpretation:
      "Un valor alto aumenta el riesgo de seguridad y mantenimiento por falta de parches o versiones antiguas."
  },
  devicesWithoutEncryption: {
    description:
      "Indica equipos sin cifrado de disco.",
    algorithm:
      "Se obtiene desde Microsoft365Service. 0 equipos es verde y cualquier valor superior a 0 es rojo.",
    interpretation:
      "Un valor mayor que cero supone riesgo de exposición de datos si se pierde o roba un equipo."
  },
  staleDevices: {
    description:
      "Cuenta dispositivos sin check-in durante más de 90 días.",
    algorithm:
      "Se genera dinámicamente en Microsoft365Service. Cualquier valor superior a 0 se considera crítico.",
    interpretation:
      "Un valor alto puede indicar inventario obsoleto, equipos fuera de uso o dispositivos que han dejado de reportar."
  },
  microsoft365Health: {
    description:
      "Resume el estado general simulado de Microsoft 365 mediante un semáforo: correcto, advertencia o crítico.",
    algorithm:
      "Agrupa los indicadores principales de Microsoft 365 en una escala común de afección.",
    interpretation:
      "Correcto indica estabilidad, advertencia indica degradación moderada y crítico una situación que requiere atención prioritaria."
  }
};

function Microsoft365Page() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadMicrosoft365Dashboard = () => {
    // La página renderiza el resumen calculado por backend; no recalcula el
    // índice de salud ni los umbrales de Microsoft 365.
    fetch(`${API_BASE_URL}/microsoft365/summary`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("No se pudo cargar el resumen Microsoft 365");
        }

        return response.json();
      })
      .then((data) => {
        setSummary(data);
        setError(null);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error cargando Microsoft 365:", error);
        setError("No se pudo conectar con el backend de Microsoft 365.");
        setSummary(null);
        setLoading(false);
      });
  };

  useEffect(() => {
    loadMicrosoft365Dashboard();

    const interval = setInterval(() => {
      loadMicrosoft365Dashboard();
    }, FRONTEND_REFRESH_INTERVAL_MS);

    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <main className="dashboard">
        <h1>Microsoft 365</h1>
        <p className="loading">Cargando datos Microsoft 365...</p>
      </main>
    );
  }

  if (error || !summary) {
    return (
      <main className="dashboard">
        <h1>Microsoft 365</h1>
        <p className="loading">{error ?? "No se han podido cargar los datos de Microsoft 365."}</p>
      </main>
    );
  }

  const microsoft365HealthDetails = summary.microsoft365HealthDetails;
  const microsoft365Health = microsoft365HealthDetails?.color ?? "UNKNOWN";
  const microsoft365Reasons = microsoft365HealthDetails?.reasons ?? [];
  const indicatorStatus = (name) =>
    findIndicatorStatus(microsoft365HealthDetails?.indicators, name);

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Monitorización Microsoft 365</p>
          <h1>Microsoft 365</h1>
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

      <section className={`status status-${microsoft365Health.toLowerCase()}`}>
        <div className="status-main">
          <span>Índice de salud Microsoft 365</span>
          <strong>Afección: {microsoft365HealthDetails?.percentage ?? 0} %</strong>
          <p>Estado: {formatStatus(microsoft365Health)}</p>
        </div>

        <div className="status-reasons">
          <span>Motivos</span>
          {microsoft365Reasons.length > 0 ? (
            <ul>
              {microsoft365Reasons.slice(0, 4).map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          ) : (
            <p>Sin motivos activos</p>
          )}
        </div>
      </section>

      <section className="dashboard-section">

      <p>
        Supervisión simulada de servicios, seguridad, aplicaciones,
        licenciamiento e Intune.
      </p>

      <h2>Uso y licenciamiento</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Usuarios activos"
          value={summary.activeUsers}
          status="neutral"
          info={microsoft365KpiInfo.activeUsers}
        />

        <KpiCard
          title="Licencias no asignadas"
          value={summary.unassignedLicenses}
          status={indicatorStatus("Licencias no asignadas")}
          info={microsoft365KpiInfo.unassignedLicenses}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Estado de servicios</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Outlook"
          value={formatStatus(summary.outlookStatus)}
          status={indicatorStatus("Outlook")}
          info={microsoft365KpiInfo.outlookStatus}
        />

        <KpiCard
          title="Teams"
          value={formatStatus(summary.teamsStatus)}
          status={indicatorStatus("Teams")}
          info={microsoft365KpiInfo.teamsStatus}
        />

        <KpiCard
          title="SharePoint"
          value={formatStatus(summary.sharePointStatus)}
          status={indicatorStatus("Servicio SharePoint")}
          info={microsoft365KpiInfo.sharePointStatus}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Exchange / SharePoint</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Buzones casi llenos"
          value={summary.nearlyFullMailboxes}
          status={indicatorStatus("Buzones casi llenos")}
          info={microsoft365KpiInfo.nearlyFullMailboxes}
        />

        <KpiCard
          title="Emails en cuarentena"
          value={summary.emailsQuarantined}
          status={indicatorStatus("Emails en cuarentena")}
          info={microsoft365KpiInfo.emailsQuarantined}
        />

        <KpiCard
          title="Almacenamiento SharePoint"
          value={`${summary.sharePointStoragePercent}%`}
          status={indicatorStatus("Almacenamiento de SharePoint")}
          info={microsoft365KpiInfo.sharePointStoragePercent}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Seguridad e identidad</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Usuarios en riesgo"
          value={summary.riskyUsers}
          status={indicatorStatus("Usuarios en riesgo")}
          info={microsoft365KpiInfo.riskyUsers}
        />

        <KpiCard
          title="Inicios fallidos"
          value={summary.failedSignIns}
          status={indicatorStatus("Inicios fallidos")}
          info={microsoft365KpiInfo.failedSignIns}
        />

        <KpiCard
          title="Usuarios sin MFA"
          value={summary.usersWithoutMfa}
          status={indicatorStatus("Usuarios sin MFA")}
          info={microsoft365KpiInfo.usersWithoutMfa}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Aplicaciones empresariales</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Secrets próximos a caducar"
          value={summary.appsSecretsExpiringSoon}
          status={indicatorStatus("Secretos proximos a caducar")}
          info={microsoft365KpiInfo.appsSecretsExpiringSoon}
        />

        <KpiCard
          title="Aplicaciones sin uso"
          value={summary.unusedApplications}
          status={indicatorStatus("Aplicaciones sin uso")}
          info={microsoft365KpiInfo.unusedApplications}
        />

        <KpiCard
          title="Apps permisos elevados"
          value={summary.highPrivilegeApplications}
          status={indicatorStatus("Apps permisos elevados")}
          info={microsoft365KpiInfo.highPrivilegeApplications}
        />
      </div>

      </section>

      <section className="dashboard-section">
      <h2>Intune / Endpoint Manager</h2>

      <div className="kpi-grid">
        <KpiCard
          title="Equipos no conformes"
          value={summary.nonCompliantDevices}
          status={indicatorStatus("Equipos no conformes")}
          info={microsoft365KpiInfo.nonCompliantDevices}
        />

        <KpiCard
          title="Tickets abiertos Microsoft 365"
          value={summary.microsoft365OpenTickets}
          status={indicatorStatus("Tickets abiertos Microsoft 365")}
          info={microsoft365KpiInfo.microsoft365OpenTickets}
        />

        <KpiCard
          title="Windows desactualizados"
          value={summary.outdatedWindowsDevices}
          status={indicatorStatus("Windows desactualizados")}
          info={microsoft365KpiInfo.outdatedWindowsDevices}
        />

        <KpiCard
          title="Equipos sin cifrado"
          value={summary.devicesWithoutEncryption}
          status={indicatorStatus("Equipos sin cifrado")}
          info={microsoft365KpiInfo.devicesWithoutEncryption}
        />

        <KpiCard
          title="Sin check-in >90 días"
          value={summary.staleDevices}
          status={indicatorStatus("Sin check-in >90 dias")}
          info={microsoft365KpiInfo.staleDevices}
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

// Reutiliza el color calculado por backend para cada indicador del índice Microsoft 365.
function findIndicatorStatus(indicators, name) {
  return indicators?.find((indicator) => indicator.name === name)?.color
    ?? "neutral";
}

export default Microsoft365Page;




