"use client";

import { useCallback, useEffect, useState } from "react";
import {
  createBreachAction,
  listBreachesAction,
  updateBreachStatusAction,
} from "@store/breaches/breachesActions";
import { listPeopleAction } from "@store/people/peopleActions";
import AppShell from "@component/layout/AppShell";
import Button from "@component/common/Button";
import StatusBadge from "@component/common/StatusBadge";
import useRoleGuard from "@component/common/useRoleGuard";

export default function BreachesScreen() {
  const session = useRoleGuard("/breaches");
  const [breaches, setBreaches] = useState([]);
  const [people, setPeople] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState("");

  const [employeeId, setEmployeeId] = useState("");
  const [policyName, setPolicyName] = useState("");
  const [notes, setNotes] = useState("");
  const [severity, setSeverity] = useState("MEDIUM");
  const [submitting, setSubmitting] = useState(false);

  const canManage = session?.role === "HR_ADMIN" || session?.role === "MANAGER";

  const load = useCallback(async (token, role) => {
    setLoading(true);
    setError("");
    try {
      const [breachData, peopleData] = await Promise.all([
        listBreachesAction(token),
        role === "HR_ADMIN" || role === "MANAGER" ? listPeopleAction(token) : Promise.resolve([]),
      ]);
      setBreaches(Array.isArray(breachData) ? breachData : []);
      setPeople(Array.isArray(peopleData) ? peopleData : []);
    } catch (err) {
      setError(err.message || "Failed to load breaches");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!session?.token) {
      return;
    }
    load(session.token, session.role);
  }, [session, load]);

  async function handleCreate(event) {
    event.preventDefault();
    if (!session?.token || !canManage) {
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await createBreachAction(session.token, {
        employeeId: Number(employeeId),
        policyName: policyName.trim(),
        notes: notes.trim(),
        severity,
      });
      setEmployeeId("");
      setPolicyName("");
      setNotes("");
      setSeverity("MEDIUM");
      await load(session.token, session.role);
    } catch (err) {
      setError(err.message || "Could not record breach");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatus(id, status) {
    if (!session?.token) {
      return;
    }
    setBusyId(String(id));
    setError("");
    try {
      await updateBreachStatusAction(session.token, id, status);
      await load(session.token, session.role);
    } catch (err) {
      setError(err.message || "Status update failed");
    } finally {
      setBusyId("");
    }
  }

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  return (
    <AppShell
      displayName={session.displayName}
      role={session.role}
      title="Policy breaches"
      subtitle={
        canManage
          ? "Flag and track policy violations for your team."
          : "Breaches recorded against your account."
      }
    >
      {canManage ? (
        <form className="policy-upload" onSubmit={handleCreate}>
          <h2>Mark policy breach</h2>
          <div className="policy-upload-row">
            <label className="field">
              <span className="field-label">Employee</span>
              <select
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                required
              >
                <option value="">Select employee</option>
                {people
                  .filter((p) => p.id !== session.employeeId)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.displayName} ({p.username})
                    </option>
                  ))}
              </select>
            </label>
            <label className="field">
              <span className="field-label">Severity</span>
              <select value={severity} onChange={(e) => setSeverity(e.target.value)}>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </label>
            <label className="field">
              <span className="field-label">Policy name</span>
              <input
                value={policyName}
                onChange={(e) => setPolicyName(e.target.value)}
                placeholder="e.g. Dress Code Policy"
                required
              />
            </label>
          </div>
          <label className="field">
            <span className="field-label">Notes</span>
            <input
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="What happened?"
              required
            />
          </label>
          <Button type="submit" icon="alert" loading={submitting}>
            Record breach
          </Button>
        </form>
      ) : null}

      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="muted">Loading breaches...</p>
      ) : breaches.length === 0 ? (
        <div className="data-panel" style={{ padding: 16 }}>
          <p className="muted" style={{ margin: 0 }}>
            No policy breaches recorded yet.
          </p>
        </div>
      ) : (
        <div className="data-panel">
          <div className="policy-table-wrap">
            <table className="policy-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Policy</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Reported by</th>
                  <th>When</th>
                  {canManage ? <th>Actions</th> : null}
                </tr>
              </thead>
              <tbody>
                {breaches.map((breach) => (
                  <tr key={breach.id}>
                    <td>
                      <strong>{breach.employeeName}</strong>
                      <p className="muted" style={{ margin: "4px 0 0", fontSize: "0.8rem" }}>
                        {breach.notes}
                      </p>
                    </td>
                    <td>{breach.policyName}</td>
                    <td>
                      <StatusBadge status={breach.severity} />
                    </td>
                    <td>
                      <StatusBadge status={breach.status} />
                    </td>
                    <td>{breach.reportedByName}</td>
                    <td>{breach.createdAt ? new Date(breach.createdAt).toLocaleString() : "—"}</td>
                    {canManage ? (
                      <td>
                        <select
                          value={breach.status}
                          disabled={busyId === String(breach.id)}
                          onChange={(e) => handleStatus(breach.id, e.target.value)}
                        >
                          <option value="OPEN">OPEN</option>
                          <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
                          <option value="RESOLVED">RESOLVED</option>
                          <option value="DISMISSED">DISMISSED</option>
                        </select>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="policy-cards">
            {breaches.map((breach) => (
              <article key={breach.id} className="policy-card">
                <div className="policy-card-header">
                  <strong>{breach.employeeName}</strong>
                  <StatusBadge status={breach.status} />
                </div>
                <div className="policy-card-meta">
                  <span>{breach.policyName}</span>
                  <span>Severity: {breach.severity}</span>
                  <span>By {breach.reportedByName}</span>
                  <span>{breach.notes}</span>
                </div>
                {canManage ? (
                  <select
                    value={breach.status}
                    disabled={busyId === String(breach.id)}
                    onChange={(e) => handleStatus(breach.id, e.target.value)}
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="DISMISSED">DISMISSED</option>
                  </select>
                ) : null}
              </article>
            ))}
          </div>
        </div>
      )}
    </AppShell>
  );
}
