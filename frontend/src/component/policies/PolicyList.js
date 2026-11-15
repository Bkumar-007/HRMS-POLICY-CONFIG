"use client";

export default function PolicyList({
  policies,
  isAdmin,
  busyId,
  onDelete,
  onIngest,
  onDisable,
  onEnable,
}) {
  if (!policies.length) {
    return <p className="muted">No policies indexed yet.</p>;
  }

  return (
    <div className="policy-list">
      <table className="policy-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Status</th>
            <th>File</th>
            <th>Filters</th>
            <th>Created</th>
            {isAdmin ? <th>Actions</th> : null}
          </tr>
        </thead>
        <tbody>
          {policies.map((policy) => {
            const busy = busyId === policy.id;
            const disabled = policy.status === "DISABLED";
            return (
              <tr key={policy.id} className={disabled ? "policy-row-disabled" : undefined}>
                <td>
                  <strong>{policy.name}</strong>
                  {policy.lastError ? <p className="error policy-error">{policy.lastError}</p> : null}
                </td>
                <td>
                  <span className={`status-pill status-${String(policy.status || "").toLowerCase()}`}>
                    {policy.status}
                  </span>
                </td>
                <td>{policy.sourceFileName}</td>
                <td>
                  {[policy.grade, policy.location].filter(Boolean).join(" · ") || "—"}
                </td>
                <td>{policy.createdAt ? new Date(policy.createdAt).toLocaleString() : "—"}</td>
                {isAdmin ? (
                  <td className="policy-actions">
                    {disabled ? (
                      <button
                        type="button"
                        className="btn-secondary"
                        disabled={busy}
                        onClick={() => onEnable(policy.id)}
                      >
                        Enable
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn-secondary"
                        disabled={busy}
                        onClick={() => onDisable(policy.id)}
                      >
                        Disable
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn-secondary"
                      disabled={busy}
                      onClick={() => onIngest(policy.id)}
                    >
                      Re-ingest
                    </button>
                    <button
                      type="button"
                      className="btn-danger"
                      disabled={busy}
                      onClick={() => {
                        if (window.confirm(`Delete policy "${policy.name}" permanently?`)) {
                          onDelete(policy.id);
                        }
                      }}
                    >
                      Delete
                    </button>
                  </td>
                ) : null}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
