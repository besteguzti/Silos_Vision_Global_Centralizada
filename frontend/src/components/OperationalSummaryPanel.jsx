import {
    formatImpactLevel,
    formatPriority,
    formatStatus,
    formatTrend
} from "../utils/statusFormatters";

function OperationalSummaryPanel({ summary, contextualNotice }) {
    if (!summary) {
        return null;
    }

    return (
        <section className={`executive-summary executive-summary-${(summary.priority ?? "LOW").toLowerCase()}`}>
            <div className="executive-summary-main">
                <p className="eyebrow">Diagnóstico operativo</p>
                <h2>Resumen operativo</h2>
                {contextualNotice && (
                    <p>{contextualNotice}</p>
                )}
                <p>{summary.summaryText}</p>
            </div>

            <div className="executive-summary-grid">
                <ExecutiveField
                    label="Servicios afectados"
                    value={
                        summary.affectedServices?.length > 0
                            ? summary.affectedServices.join(", ")
                            : "Sin servicios afectados"
                    }
                />
                <ExecutiveField
                    label="Plataforma principal"
                    value={summary.mainAffectedPlatform}
                />
                <ExecutiveField
                    label="Origen probable"
                    value={summary.probableOrigin}
                />
                <ExecutiveField
                    label="Impacto"
                    value={formatImpactLevel(summary.impactLevel)}
                />
                <ExecutiveField
                    label="Usuarios potencialmente afectados"
                    value={summary.estimatedAffectedUsers}
                />
                <ExecutiveField
                    label="Prioridad"
                    value={formatPriority(summary.priority)}
                />
                <ExecutiveField
                    label="Tendencia"
                    value={formatTrend(summary.trend)}
                />
                <ExecutiveField
                    label="Primera acción"
                    value={summary.firstAction}
                />
            </div>

            <PlatformFindings findings={summary.platformFindings} />
        </section>
    );
}

function ExecutiveField({ label, value }) {
    return (
        <div className="executive-summary-field">
            <span>{label}</span>
            <strong>{value ?? "Sin datos"}</strong>
        </div>
    );
}

function PlatformFindings({ findings }) {
    const visibleFindings = Array.isArray(findings)
        ? findings.filter((item) => item?.platform && Array.isArray(item.findings) && item.findings.length > 0)
        : [];

    return (
        <div className="platform-findings">
            <h3>Incidencias detectadas por plataforma</h3>
            {visibleFindings.length > 0 ? (
                <div className="platform-findings-grid">
                    {visibleFindings.map((item) => (
                        <article
                            className={`platform-finding-card platform-finding-${(item.status ?? "UNKNOWN").toLowerCase()}`}
                            key={item.platform}
                        >
                            <div className="platform-finding-header">
                                <strong>{item.platform}</strong>
                                <span>{formatStatus(item.status)}</span>
                            </div>
                            <ul>
                                {item.findings.map((finding, index) => (
                                    <li key={`${item.platform}-${index}`}>{finding}</li>
                                ))}
                            </ul>
                        </article>
                    ))}
                </div>
            ) : (
                <p className="platform-findings-empty">
                    No se han detectado incidencias relevantes en los indicadores evaluados.
                </p>
            )}
        </div>
    );
}

export default OperationalSummaryPanel;

