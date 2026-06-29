function AnalysisHeader({ children }) {
  return (
    <header className="dashboard-header">
      <div>
        <p className="eyebrow">Modulo de análisis</p>
        <h1>Análisis exploratorio de KPIs transversales</h1>
      </div>

      {children}
    </header>
  );
}

export default AnalysisHeader;

