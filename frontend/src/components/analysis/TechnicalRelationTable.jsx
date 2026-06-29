import {
  cssTone,
  formatOptionalPercent,
  formatOptionalSigned
} from "./analysisUtils";

// Tabla de co-afección entre plataformas técnicas; no implica causalidad directa.
function TechnicalRelationTable({ relations }) {
  return (
    <section className="dashboard-section">
      <h2>Relacion técnica aparente entre plataformas</h2>
      <div className="analysis-table-wrapper">
        <table className="analysis-points-table analysis-relations-table">
          <thead>
            <tr>
              <th>Relacion</th>
              <th>Co-ocurrencia técnica</th>
              <th>Incremento medio</th>
              <th>Lectura</th>
            </tr>
          </thead>
          <tbody>
            {relations.map((relation) => (
              <tr key={relation.relation}>
                <td>{relation.relation}</td>
                <td>{formatOptionalPercent(relation.cooccurrencePercentage)}</td>
                <td>{formatOptionalSigned(relation.averageIncrease)}</td>
                <td>
                  <span className={`analysis-reading ${cssTone(relation.readingStatus)}`}>
                    {relation.reading}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default TechnicalRelationTable;

