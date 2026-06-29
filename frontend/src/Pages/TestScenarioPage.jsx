import { useState } from "react";

import "../App.css";

import KpiCard from "../components/KpiCard";
import OperationalSummaryPanel from "../components/OperationalSummaryPanel";
import { API_BASE_URL } from "../config/api";
import { formatStatus } from "../utils/statusFormatters";

const initialForm = {
    aruba: {
        totalAps: 0,
        downAps: 0,
        inactiveAps: 0,
        firmwareOutdated: 0,
        totalSwitches: 0,
        downSwitches: 0,
        switchesFirmwareUpgradeRequired: 0,
        totalWifiClients: 0,
        mutualiaWifiClients: 0,
        mutualiaApsClients: 0,
        arubaOpenTickets: 0
    },
    citrix: {
        activeSessions: 0,
        disconnectedSessions: 0,
        totalDeliveryControllers: 0,
        availableDeliveryControllers: 0,
        averageLogonDurationSeconds: 0,
        serverLoadPercent: 0,
        failedLogons: 0,
        citrixOpenTickets: 0
    },
    microsoft365: {
        sharePointStoragePercent: 0,
        activeUsers: 0,
        usersWithoutMfa: 0,
        appsSecretsExpiringSoon: 0,
        nonCompliantDevices: 0,
        outdatedWindowsDevices: 0,
        devicesWithoutEncryption: 0,
        microsoft365OpenTickets: 0
    },
    glpi: {
        criticalOpenTickets: 0,
        dailyClosurePercent: 100,
        weeklyClosurePercent: 100
    }
};

const normalizeNumberInput = (rawValue) => {
    if (rawValue === "") {
        return 0;
    }

    const numericValue = Number(rawValue);

    return Number.isFinite(numericValue) ? numericValue : 0;
};

const formatNumberInputValue = (value) => {
    const numericValue = normalizeNumberInput(value);

    return String(numericValue);
};

function TestScenarioPage() {
    const [formValues, setFormValues] = useState(initialForm);
    const [errors, setErrors] = useState({});
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [submitError, setSubmitError] = useState(null);

    const handleChange = (section, field, value) => {
        setFormValues((current) => {
            const nextValues = {
                ...current,
                [section]: {
                    ...current[section],
                    [field]: value
                }
            };

            return nextValues;
        });
    };

    const calculatedOpenTickets =
        formValues.aruba.arubaOpenTickets
        + formValues.citrix.citrixOpenTickets
        + formValues.microsoft365.microsoft365OpenTickets;

    const validate = () => {
        const nextErrors = {};

        const addError = (path, message) => {
            nextErrors[path] = message;
        };

        const { aruba, citrix, microsoft365, glpi } = formValues;

        if (aruba.downAps > aruba.totalAps) {
            addError("aruba.downAps", "APs caídos no puede ser mayor que total APs.");
        }

        if (aruba.downSwitches > aruba.totalSwitches) {
            addError("aruba.downSwitches", "Switches caídos no puede ser mayor que total switches.");
        }

        if (citrix.availableDeliveryControllers > citrix.totalDeliveryControllers) {
            addError("citrix.availableDeliveryControllers", "Delivery Controllers disponibles no puede ser mayor que total Delivery Controllers.");
        }

        if (citrix.activeSessions + citrix.disconnectedSessions > aruba.totalWifiClients) {
            addError("citrix.disconnectedSessions", "Las sesiones Citrix activas y desconectadas no pueden superar los clientes WiFi.");
        }

        if (microsoft365.usersWithoutMfa > microsoft365.activeUsers) {
            addError("microsoft365.usersWithoutMfa", "Usuarios sin MFA no puede ser mayor que usuarios activos Microsoft 365.");
        }

        const percentageFields = [
            ["microsoft365.sharePointStoragePercent", microsoft365.sharePointStoragePercent],
            ["glpi.dailyClosurePercent", glpi.dailyClosurePercent],
            ["glpi.weeklyClosurePercent", glpi.weeklyClosurePercent],
            ["citrix.serverLoadPercent", citrix.serverLoadPercent]
        ];

        percentageFields.forEach(([path, value]) => {
            if (value < 0 || value > 100) {
                addError(path, "El valor debe estar entre 0 y 100.");
            }
        });

        Object.entries({ aruba, citrix, microsoft365, glpi }).forEach(([section, data]) => {
            Object.entries(data).forEach(([field, value]) => {
                if (value < 0) {
                    addError(`${section}.${field}`, "No se permiten valores negativos.");
                }
            });
        });

        setErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!validate()) {
            return;
        }

        setLoading(true);
        setSubmitError(null);

        try {
            const response = await fetch(`${API_BASE_URL}/api/test-scenarios/evaluate`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(formValues)
            });

            if (!response.ok) {
                const errorBody = await response.text();
                throw new Error(errorBody || "Error al evaluar el escenario.");
            }

            const data = await response.json();
            setResult(data);
        } catch {
            setSubmitError(
                "No se pudo calcular el escenario. Verifica los valores y vuelve a intentarlo."
            );
            setResult(null);
        } finally {
            setLoading(false);
        }
    };

    const renderInput = (
        section,
        field,
        label,
        step = 1,
        max,
        min = 0,
        disabled = false
    ) => {
        const value = formValues[section][field];
        const path = `${section}.${field}`;

        const handleNumericChange = (event) => {
            const normalizedValue = normalizeNumberInput(event.target.value);

            event.target.value = formatNumberInputValue(normalizedValue);
            handleChange(section, field, normalizedValue);
        };

        return (
            <label className="form-field">
                <span>{label}</span>
                <input
                    type="number"
                    value={formatNumberInputValue(value)}
                    min={min}
                    max={max}
                    step={step}
                    disabled={disabled}
                    onChange={handleNumericChange}
                />
                {errors[path] && (
                    <span className="validation-error">{errors[path]}</span>
                )}
            </label>
        );
    };

    const renderCalculatedInput = (label, value) => (
        <label className="form-field">
            <span>{label}</span>
            <input
                type="number"
                value={formatNumberInputValue(value)}
                disabled
            />
        </label>
    );

    const cards = result
        ? [
            {
                title: "Estado global",
                value: `${result.summary.globalHealthPercentage}%`,
                status: result.summary.globalHealthStatus
            },
            {
                title: "Criticidad global",
                value: `${result.summary.globalCriticality}%`,
                status: result.summary.globalCriticalityStatus
            },
            {
                title: "Disponibilidad global",
                value: `${result.summary.globalAvailability}%`,
                status: result.summary.globalAvailabilityStatus
            },
            {
                title: "Presión operativa",
                value: `${result.summary.operationalPressure}%`,
                status: result.summary.operationalPressureStatus
            },
            {
                title: "Degradación técnica",
                value: `${result.summary.technicalDegradation}%`,
                status: result.summary.technicalDegradationStatus
            },
            {
                title: "Riesgo SLA",
                value: `${result.summary.slaRisk}%`,
                status: result.summary.slaRiskStatus
            },
            {
                title: "Backlog operativo",
                value: `${result.summary.operationalBacklog}%`,
                status: result.summary.operationalBacklogStatus
            },
            {
                title: "Impacto en usuarios",
                value: `${result.summary.userImpact}%`,
                status: result.summary.userImpactStatus
            },
            {
                title: "Servicios afectados",
                value: `${result.summary.affectedServicesPercent}%`,
                status: result.summary.affectedServicesStatus
            }
        ]
        : [];

    const operationalSummary = result?.operationalSummary;

    return (
        <main className="dashboard test-scenario">
            <header className="dashboard-header">
                <div>
                    <p className="eyebrow">Banco de pruebas</p>
                    <h1>Banco de pruebas</h1>
                </div>
            </header>

            <section className="alert test-note">
                Los datos de esta pantalla son datos de prueba. No se guardan ni afectan al dashboard real.
            </section>

            <form className="test-form" onSubmit={handleSubmit} noValidate>
                <div className="form-group">
                    <h2>Aruba</h2>
                    <div className="form-grid">
                        {renderInput("aruba", "totalAps", "Total APs")}
                        {renderInput("aruba", "downAps", "APs caídos")}
                        {renderInput("aruba", "inactiveAps", "APs inactivos")}
                        {renderInput("aruba", "firmwareOutdated", "Firmware pendiente APs")}
                        {renderInput("aruba", "totalSwitches", "Total switches")}
                        {renderInput("aruba", "downSwitches", "Switches caídos")}
                        {renderInput("aruba", "switchesFirmwareUpgradeRequired", "Switches con upgrade pendiente")}
                        {renderInput("aruba", "totalWifiClients", "Clientes WiFi")}
                        {renderInput("aruba", "mutualiaWifiClients", "Clientes Mutualia-WIFI")}
                        {renderInput("aruba", "mutualiaApsClients", "Clientes Mutualia-APS")}
                        {renderInput("aruba", "arubaOpenTickets", "Tickets abiertos Aruba")}
                    </div>
                </div>

                <div className="form-group">
                    <h2>Citrix</h2>
                    <div className="form-grid">
                        {renderInput("citrix", "activeSessions", "Sesiones activas")}
                        {renderInput("citrix", "disconnectedSessions", "Sesiones desconectadas")}
                        {renderInput("citrix", "totalDeliveryControllers", "Delivery Controllers totales")}
                        {renderInput("citrix", "availableDeliveryControllers", "Delivery Controllers disponibles")}
                        {renderInput("citrix", "averageLogonDurationSeconds", "Duración media de logon (s)")}
                        {renderInput("citrix", "serverLoadPercent", "Carga servidores (%)", 1, 100)}
                        {renderInput("citrix", "failedLogons", "Errores de inicio")}
                        {renderInput("citrix", "citrixOpenTickets", "Tickets abiertos Citrix")}
                    </div>
                </div>

                <div className="form-group">
                    <h2>Microsoft 365</h2>
                    <div className="form-grid">
                        {renderInput("microsoft365", "sharePointStoragePercent", "SharePoint usado (%)", 1, 100)}
                        {renderInput("microsoft365", "activeUsers", "Usuarios activos Microsoft 365")}
                        {renderInput("microsoft365", "usersWithoutMfa", "Usuarios sin MFA")}
                        {renderInput("microsoft365", "appsSecretsExpiringSoon", "Secretos próximos a caducar")}
                        {renderInput("microsoft365", "nonCompliantDevices", "Equipos no conformes")}
                        {renderInput("microsoft365", "outdatedWindowsDevices", "Windows desactualizados")}
                        {renderInput("microsoft365", "devicesWithoutEncryption", "Equipos sin cifrado")}
                        {renderInput("microsoft365", "microsoft365OpenTickets", "Tickets abiertos Microsoft 365")}
                    </div>
                </div>

                <div className="form-group">
                    <h2>GLPI</h2>
                    <div className="form-grid">
                        {renderCalculatedInput("Tickets abiertos totales (calculado)", calculatedOpenTickets)}
                        {renderInput("glpi", "criticalOpenTickets", "Tickets críticos")}
                        {renderInput("glpi", "dailyClosurePercent", "Porcentaje cierre diario (%)", 1, 100)}
                        {renderInput("glpi", "weeklyClosurePercent", "Porcentaje cierre semanal (%)", 1, 100)}
                    </div>
                </div>

                <div className="form-actions">
                    <button type="submit" disabled={loading}>
                        {loading ? "Calculando escenario…" : "Calcular escenario"}
                    </button>
                </div>

                {submitError && (
                    <section className="alert" role="alert">
                        {submitError}
                    </section>
                )}
            </form>

            {result && (
                <section className="dashboard-section result-section">
                    <h2>Resultados del escenario</h2>

                    <div className="kpi-grid">
                        {cards.map((card) => (
                            <KpiCard
                                key={card.title}
                                title={card.title}
                                value={card.value}
                                status={card.status}
                            />
                        ))}
                    </div>

                    <div className="platform-status-grid">
                        <div className="platform-status-card">
                            <strong>Estado Aruba</strong>
                            <p>{formatStatus(result.platformStatus.aruba)}</p>
                        </div>
                        <div className="platform-status-card">
                            <strong>Estado Citrix</strong>
                            <p>{formatStatus(result.platformStatus.citrix)}</p>
                        </div>
                        <div className="platform-status-card">
                            <strong>Estado Microsoft 365</strong>
                            <p>{formatStatus(result.platformStatus.microsoft365)}</p>
                        </div>
                        <div className="platform-status-card">
                            <strong>Estado GLPI</strong>
                            <p>{formatStatus(result.platformStatus.glpi)}</p>
                        </div>
                    </div>

                    {operationalSummary && (
                        <OperationalSummaryPanel
                            summary={operationalSummary}
                            contextualNotice="Este resumen se calcula únicamente con los valores introducidos en el banco de pruebas. No modifica los datos reales del dashboard."
                        />
                    )}

                </section>
            )}
        </main>
    );
}

export default TestScenarioPage;

