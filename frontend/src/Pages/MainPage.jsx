import { useEffect, useState } from "react";

import "../App.css";

import KpiCard from "../components/KpiCard";
import OperationalSummaryPanel from "../components/OperationalSummaryPanel";
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from "../config/api";
import { formatDataStatus } from "../utils/statusFormatters";

const mainKpiInfo = {
    globalHealth: {
        description:
            "Resume la afección general combinando Aruba, Citrix, Microsoft 365 y GLPI.",
        algorithm:
            "Se calcula ponderando los índices principales según la configuración definida en backend.",
        interpretation:
            "0-33 es verde, 34-66 amarillo y 67-100 rojo. Cuanto más alto, mayor afección global."
    },
    globalCriticality: {
        description:
            "Recoge cuántas señales críticas aparecen dentro de las plataformas.",
        algorithm:
            "Promedia indicadores normalizados: correcto 0, advertencia 50 y crítico 100.",
        interpretation:
            "Cuando sube, conviene revisar primero las plataformas con indicadores en rojo."
    },
    globalAvailability: {
        description:
            "Resume la disponibilidad estimada de los servicios principales.",
        algorithm:
            "Agrupa señales de disponibilidad de Aruba, Citrix, Microsoft 365 y GLPI usando los pesos configurados en backend.",
        interpretation:
            "Cuanto más alto, mayor disponibilidad estimada. Valores bajos indican mayor afección sobre la disponibilidad."
    },
    operationalPressure: {
        description:
            "Resume la carga técnica y operativa acumulada.",
        algorithm:
            "Agrupa señales de GLPI, Citrix, Microsoft 365 y Aruba usando los pesos configurados en backend.",
        interpretation:
            "Cuando sube, el área técnica tiene más trabajo o más tensión operativa."
    },
    technicalDegradation: {
        description:
            "Detecta deterioro técnico aunque no exista una caída total.",
        algorithm:
            "Agrupa indicadores técnicos de Aruba, Citrix, Microsoft 365 y GLPI según la configuración del backend.",
        interpretation:
            "Valores altos indican degradación técnica que conviene revisar."
    },
    slaRisk: {
        description:
            "Estima si hay señales que puedan comprometer niveles de servicio.",
        algorithm:
            "Cruza señales de Citrix, Aruba, GLPI y Microsoft 365 con los pesos definidos en backend.",
        interpretation:
            "Cuando sube, conviene revisar SLA, tiempos de respuesta y degradación percibida."
    },
    operationalBacklog: {
        description:
            "Resume el trabajo pendiente acumulado.",
        algorithm:
            "Relaciona trabajo pendiente de GLPI con señales de Microsoft 365, Aruba y Citrix según pesos configurados en backend.",
        interpretation:
            "Cuando sube, hay más tareas pendientes o acciones técnicas por cerrar."
    },
    userImpact: {
        description:
            "Aproxima la afección que pueden percibir los usuarios.",
        algorithm:
            "Usa señales de impacto de Citrix, Aruba, Microsoft 365 y GLPI con la configuración del backend.",
        interpretation:
            "Cuando sube, es más probable que el problema sea visible para usuarios finales."
    },
    affectedServices: {
        description:
            "Indica cuántas plataformas están afectadas a la vez.",
        algorithm:
            "Calcula la proporción de plataformas que están en amarillo o rojo entre Aruba, Citrix, Microsoft 365 y GLPI.",
        interpretation:
            "0% significa ninguna plataforma afectada. 100% significa las cuatro plataformas afectadas."
    }
};

function MainPage() {
    const [summary, setSummary] =
        useState(null);

    const [executiveSummary, setExecutiveSummary] =
        useState(null);

    const [error, setError] =
        useState(null);

    const loadDashboard = () => {
        // La lógica de KPIs y diagnóstico vive en backend; aquí solo se
        // recuperan los DTOs para renderizar tarjetas y resumen operativo.
        fetch(`${API_BASE_URL}/dashboard/summary`)
            .then((response) => {

                if (!response.ok) {

                    throw new Error(
                        "No se pudo cargar el dashboard general"
                    );
                }

                return response.json();
            })
            .then((data) => {

                setSummary(data);
                setError(null);
            })
            .catch(() => {

                setError(
                    "No se pudo conectar con el backend del dashboard."
                );
            });

        fetch(`${API_BASE_URL}/api/dashboard/executive-summary`)
            .then((response) => {

                if (!response.ok) {

                    throw new Error(
                        "No se pudo cargar el diagnóstico operativo"
                    );
                }

                return response.json();
            })
            .then((data) => {

                setExecutiveSummary(data);
            })
            .catch(() => {

                setExecutiveSummary(null);
            });
    };

    useEffect(() => {

        loadDashboard();

        const interval =
            setInterval(() => {

                loadDashboard();

            }, FRONTEND_REFRESH_INTERVAL_MS);

        return () => clearInterval(interval);

    }, []);

    if (!summary && !error) {

        return (
            <main className="dashboard">
                <h1>Dashboard General</h1>
                <p className="loading">Cargando dashboard general...</p>
            </main>
        );
    }

    const cards = summary
        ? [
            {
                title: "Estado global",
                value: `${summary.globalHealthPercentage}%`,
                status: summary.globalHealthStatus,
                info: mainKpiInfo.globalHealth
            },
            {
                title: "Criticidad global",
                value: `${summary.globalCriticality}%`,
                status: summary.globalCriticalityStatus,
                info: mainKpiInfo.globalCriticality
            },
            {
                title: "Disponibilidad global",
                value: `${summary.globalAvailability}%`,
                status: summary.globalAvailabilityStatus,
                info: mainKpiInfo.globalAvailability
            },
            {
                title: "Presión operativa",
                value: `${summary.operationalPressure}%`,
                status: summary.operationalPressureStatus,
                info: mainKpiInfo.operationalPressure
            },
            {
                title: "Degradación técnica",
                value: `${summary.technicalDegradation}%`,
                status: summary.technicalDegradationStatus,
                info: mainKpiInfo.technicalDegradation
            },
            {
                title: "Riesgo SLA",
                value: `${summary.slaRisk}%`,
                status: summary.slaRiskStatus,
                info: mainKpiInfo.slaRisk
            },
            {
                title: "Backlog operativo",
                value: `${summary.operationalBacklog}%`,
                status: summary.operationalBacklogStatus,
                info: mainKpiInfo.operationalBacklog
            },
            {
                title: "Impacto en usuarios",
                value: `${summary.userImpact}%`,
                status: summary.userImpactStatus,
                info: mainKpiInfo.userImpact
            },
            {
                title: "Servicios afectados",
                value: `${summary.affectedServicesPercent}%`,
                status: summary.affectedServicesStatus,
                info: mainKpiInfo.affectedServices
            }
        ]
        : [];

    return (
        <main className="dashboard">
            <header className="dashboard-header">
                <div>
                    <p className="eyebrow">Vista principal</p>
                    <h1>Dashboard General</h1>
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

            {executiveSummary && (
                <OperationalSummaryPanel summary={executiveSummary} />
            )}

            {summary && (
                <section className="dashboard-section">
                    <div className="kpi-grid">
                        {cards.map((card) => (
                            <KpiCard
                                key={card.title}
                                title={card.title}
                                value={card.value}
                                status={card.status}
                                info={card.info}
                            />
                        ))}
                    </div>
                </section>
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

export default MainPage;

