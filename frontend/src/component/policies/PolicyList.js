"use client";

import Button from "@component/common/Button";
import StatusBadge from "@component/common/StatusBadge";

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
    return (
      <div className="policy-list">
        <p className="muted" style={{ padding: 16 }}>
          No policies indexed yet.
        </p>
      </div>
    );
  }

  return (
    <div className="policy-list">
      <div className="policy-table-wrap">
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
            {policies.map((policy) => (
              <tr key={policy.id} className={policy.status === "DISABLED" ? "policy-row-disabled" : undefined}>
                <td>
                  <strong>{policy.name}</strong>
                  {policy.lastError ? <p className="error policy-error">{policy.lastError}</p> : null}
                </td>
                <td>
                  <StatusBadge status={policy.status} />
                </td>
                <td>{policy.sourceFileName}</td>
                <td>{[policy.grade, policy.location].filter(Boolean).join(" · ") || "—"}</td>
                <td>{policy.createdAt ? new Date(policy.createdAt).toLocaleString() : "—"}</td>
                {isAdmin ? (
                  <td>
                    <PolicyActions
                      policy={policy}
                      busy={busyId === policy.id}
                      onDelete={onDelete}
                      onIngest={onIngest}
                      onDisable={onDisable}
                      onEnable={onEnable}
                    />
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="policy-cards">
        {policies.map((policy) => {
          const disabled = policy.status === "DISABLED";
          return (
            <article
              key={policy.id}
              className={`policy-card ${disabled ? "policy-row-disabled" : ""}`}
            >
              <div className="policy-card-header">
                <strong>{policy.name}</strong>
                <StatusBadge status={policy.status} />
              </div>
              <div className="policy-card-meta">
                <span>{policy.sourceFileName}</span>
                <span>{[policy.grade, policy.location].filter(Boolean).join(" · ") || "No filters"}</span>
                <span>{policy.createdAt ? new Date(policy.createdAt).toLocaleString() : "—"}</span>
              </div>
              {policy.lastError ? <p className="error policy-error">{policy.lastError}</p> : null}
              {isAdmin ? (
                <PolicyActions
                  policy={policy}
                  busy={busyId === policy.id}
                  onDelete={onDelete}
                  onIngest={onIngest}
                  onDisable={onDisable}
                  onEnable={onEnable}
                  className="policy-card-actions"
                />
              ) : null}
            </article>
          );
        })}
      </div>
    </div>
  );
}

function PolicyActions({
  policy,
  busy,
  onDelete,
  onIngest,
  onDisable,
  onEnable,
  className = "policy-actions",
}) {
  const disabled = policy.status === "DISABLED";

  return (
    <div className={className}>
      {disabled ? (
        <Button variant="secondary" icon="check" disabled={busy} onClick={() => onEnable(policy.id)}>
          Enable
        </Button>
      ) : (
        <Button variant="secondary" icon="ban" disabled={busy} onClick={() => onDisable(policy.id)}>
          Disable
        </Button>
      )}
      <Button variant="secondary" icon="refresh" disabled={busy} onClick={() => onIngest(policy.id)}>
        Re-ingest
      </Button>
      <Button
        variant="danger"
        icon="trash"
        disabled={busy}
        onClick={() => {
          if (window.confirm(`Delete policy "${policy.name}" permanently?`)) {
            onDelete(policy.id);
          }
        }}
      >
        Delete
      </Button>
    </div>
  );
}
