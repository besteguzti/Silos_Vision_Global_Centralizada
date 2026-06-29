function AnalysisErrorState({ message }) {
  if (!message) {
    return null;
  }

  return (
    <section className="alert" role="alert">
      {message}
    </section>
  );
}

export default AnalysisErrorState;
