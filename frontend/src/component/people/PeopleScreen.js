"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  listPeopleAction,
  updateManagerAction,
  updateRoleAction,
} from "@store/people/peopleActions";
import AppShell from "@component/layout/AppShell";
import Button from "@component/common/Button";
import StatusBadge from "@component/common/StatusBadge";
import useRoleGuard from "@component/common/useRoleGuard";

export default function PeopleScreen() {
  const router = useRouter();
  const session = useRoleGuard("/people");
  const [people, setPeople] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [busyId, setBusyId] = useState("");

  const load = useCallback(async (token) => {
    setLoading(true);
    setError("");
    try {
      const data = await listPeopleAction(token);
      setPeople(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load directory");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!session?.token) {
      return;
    }
    load(session.token);
  }, [session, load]);

  const isAdmin = session?.role === "HR_ADMIN";
  const isManager = session?.role === "MANAGER" || isAdmin;

  const managers = useMemo(
    () => people.filter((p) => p.role === "MANAGER" || p.role === "HR_ADMIN"),
    [people],
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      return people;
    }
    return people.filter((p) =>
      [p.displayName, p.username, p.department, p.location, p.role]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q)),
    );
  }, [people, query]);

  async function handleRoleChange(employeeId, role) {
    if (!session?.token) {
      return;
    }
    setBusyId(String(employeeId));
    setError("");
    try {
      await updateRoleAction(session.token, employeeId, role);
      await load(session.token);
    } catch (err) {
      setError(err.message || "Role update failed");
    } finally {
      setBusyId("");
    }
  }

  async function handleManagerChange(employeeId, managerId) {
    if (!session?.token) {
      return;
    }
    setBusyId(String(employeeId));
    setError("");
    try {
      await updateManagerAction(session.token, employeeId, managerId ? Number(managerId) : null);
      await load(session.token);
    } catch (err) {
      setError(err.message || "Manager update failed");
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
      title={isAdmin ? "People & access" : isManager ? "My team" : "My profile"}
      subtitle={
        isAdmin
          ? "Assign HR / manager roles and maintain reporting lines."
          : isManager
            ? "Review your direct reports and open policy flags."
            : "Your employee directory card."
      }
    >
      <div className="module-toolbar">
        <label className="field search-field">
          <span className="field-label">Search</span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Name, department, location…"
          />
        </label>
        {isManager ? (
          <Button variant="secondary" icon="alert" onClick={() => router.push("/breaches")}>
            Policy breaches
          </Button>
        ) : null}
      </div>

      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="muted">Loading people...</p>
      ) : (
        <div className="data-panel">
          <div className="policy-table-wrap">
            <table className="policy-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Location</th>
                  <th>Manager</th>
                  <th>Open breaches</th>
                  {isAdmin ? <th>Admin actions</th> : null}
                </tr>
              </thead>
              <tbody>
                {filtered.map((person) => (
                  <tr key={person.id}>
                    <td>
                      <strong>{person.displayName}</strong>
                      <div className="muted" style={{ fontSize: "0.8rem" }}>
                        {person.username} · {person.grade}
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={person.role} />
                    </td>
                    <td>{person.department || "—"}</td>
                    <td>{person.location}</td>
                    <td>{person.managerName || "—"}</td>
                    <td>
                      {person.openBreachCount > 0 ? (
                        <span className="breach-count">{person.openBreachCount}</span>
                      ) : (
                        "0"
                      )}
                    </td>
                    {isAdmin ? (
                      <td>
                        <div className="inline-admin-actions">
                          <select
                            value={person.role}
                            disabled={busyId === String(person.id) || person.id === session.employeeId}
                            onChange={(e) => handleRoleChange(person.id, e.target.value)}
                          >
                            <option value="EMPLOYEE">EMPLOYEE</option>
                            <option value="MANAGER">MANAGER</option>
                            <option value="HR_ADMIN">HR_ADMIN</option>
                          </select>
                          <select
                            value={person.managerId || ""}
                            disabled={busyId === String(person.id)}
                            onChange={(e) => handleManagerChange(person.id, e.target.value)}
                          >
                            <option value="">No manager</option>
                            {managers
                              .filter((m) => m.id !== person.id)
                              .map((m) => (
                                <option key={m.id} value={m.id}>
                                  {m.displayName}
                                </option>
                              ))}
                          </select>
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="policy-cards">
            {filtered.map((person) => (
              <article key={person.id} className="policy-card">
                <div className="policy-card-header">
                  <strong>{person.displayName}</strong>
                  <StatusBadge status={person.role} />
                </div>
                <div className="policy-card-meta">
                  <span>{person.username}</span>
                  <span>
                    {person.department || "—"} · {person.location}
                  </span>
                  <span>Manager: {person.managerName || "—"}</span>
                  <span>Open breaches: {person.openBreachCount}</span>
                </div>
                {isAdmin ? (
                  <div className="inline-admin-actions">
                    <select
                      value={person.role}
                      disabled={busyId === String(person.id) || person.id === session.employeeId}
                      onChange={(e) => handleRoleChange(person.id, e.target.value)}
                    >
                      <option value="EMPLOYEE">EMPLOYEE</option>
                      <option value="MANAGER">MANAGER</option>
                      <option value="HR_ADMIN">HR_ADMIN</option>
                    </select>
                    <select
                      value={person.managerId || ""}
                      disabled={busyId === String(person.id)}
                      onChange={(e) => handleManagerChange(person.id, e.target.value)}
                    >
                      <option value="">No manager</option>
                      {managers
                        .filter((m) => m.id !== person.id)
                        .map((m) => (
                          <option key={m.id} value={m.id}>
                            {m.displayName}
                          </option>
                        ))}
                    </select>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        </div>
      )}
    </AppShell>
  );
}
