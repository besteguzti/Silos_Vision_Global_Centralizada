function AnalysisEmptyState({
  message = "No hay datos suficientes para generar la comparacion seleccionada."
}) {
  return <p className="loading">{message}</p>;
}

export default AnalysisEmptyState;
