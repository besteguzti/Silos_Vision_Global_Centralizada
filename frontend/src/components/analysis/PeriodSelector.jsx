function PeriodSelector({ periods, selectedPeriod, onChange }) {
  return (
    <label>
      Periodo
      <select value={selectedPeriod} onChange={onChange}>
        {periods.map((item) => (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export default PeriodSelector;
