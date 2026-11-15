export default function StatusBadge({ status }) {
  const value = String(status || "UNKNOWN");
  return (
    <span className={`status-pill status-${value.toLowerCase()}`}>
      <span className="status-dot" aria-hidden="true" />
      {value}
    </span>
  );
}
