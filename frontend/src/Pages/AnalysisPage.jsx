import { useEffect, useState } from "react";

import "../App.css";

import AnalysisErrorState from "../components/analysis/AnalysisErrorState";
import AnalysisHeader from "../components/analysis/AnalysisHeader";
import AnalysisLoadingState from "../components/analysis/AnalysisLoadingState";
import SpecificKpiRelationsSection from "../components/analysis/SpecificKpiRelationsSection";
import TechnicalRelationTable from "../components/analysis/TechnicalRelationTable";
import TemporalTrendSection from "../components/analysis/TemporalTrendSection";
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from "../config/api";

const periods = [
  { label: "7 días", value: "7d" },
  { label: "30 días", value: "30d" },
  { label: "90 días", value: "90d" }
];

function AnalysisPage() {
  const [period, setPeriod] = useState("30d");
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // El panel se alimenta del endpoint agregado del backend: React no genera
  // datos de prueba ni calcula relaciones, solo renderiza la respuesta persistida.
  useEffect(() => {
    const loadAnalysis = () => fetch(
      `${API_BASE_URL}/api/analysis/glpi-platform-relation?period=${period}`
    )
      .then((response) => {
        if (!response.ok) {
          throw new Error("No se pudo cargar el análisis");
        }

        return response.json();
      })
      .then((data) => {
        setAnalysis(data);
        setError(null);
        setLoading(false);
      })
      .catch(() => {
        setError("No se han podido cargar los datos del análisis.");
        setLoading(false);
      });

    loadAnalysis();

    const interval = setInterval(() => {
      loadAnalysis();
    }, FRONTEND_REFRESH_INTERVAL_MS);

    return () => clearInterval(interval);
  }, [period]);

  const handlePeriodChange = (event) => {
    setLoading(true);
    setPeriod(event.target.value);
  };

  return (
    <main className="dashboard">
      <AnalysisHeader />

      <AnalysisErrorState message={error} />

      {loading && <AnalysisLoadingState />}

      {analysis && !loading && (
        <>
          <TechnicalRelationTable relations={analysis.technicalRelations ?? []} />

          <TemporalTrendSection
            timeline={analysis.technicalTimeline ?? []}
            periods={periods}
            selectedPeriod={period}
            onPeriodChange={handlePeriodChange}
          />

          <SpecificKpiRelationsSection
            relations={analysis.specificKpiRelations ?? []}
            periods={periods}
            selectedPeriod={period}
            onPeriodChange={handlePeriodChange}
          />
        </>
      )}
    </main>
  );
}

export default AnalysisPage;

