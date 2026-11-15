"use client";

import { useCallback, useEffect, useState } from "react";
import {
  listHelpdeskTicketsAction,
  updateHelpdeskTicketStatusAction,
} from "@store/helpdesk/helpdeskActions";
import AppShell from "@component/layout/AppShell";
import StatusBadge from "@component/common/StatusBadge";
import useRoleGuard from "@component/common/useRoleGuard";

export default function HelpdeskScreen() {
  const session = useRoleGuard("/helpdesk");
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState("");

  const load = useCallback(async (token) => {
    setLoading(true);
    setError("");
    try {
      const data = await listHelpdeskTicketsAction(token);
      setTickets(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load tickets");
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

  async function handleStatus(id, status) {
    if (!session?.token || session.role !== "HR_ADMIN") {
      return;
    }
    setBusyId(String(id));
    setError("");
    try {
      await updateHelpdeskTicketStatusAction(session.token, id, status);
      await load(session.token);
    } catch (err) {
      setError(err.message || "Could not update ticket");
    } finally {
      setBusyId("");
    }
  }

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  const isAdmin = session.role === "HR_ADMIN";

  return (
    <AppShell
      displayName={session.displayName}
      role={session.role}
      title="Helpdesk inbox"
      subtitle={
        isAdmin
          ? "Review and update tickets raised through the assistant."
          : "Tickets from your direct reports."
      }
    >
      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="muted">Loading tickets...</p>
      ) : tickets.length === 0 ? (
        <div className="data-panel" style={{ padding: 16 }}>
          <p className="muted" style={{ margin: 0 }}>
            No helpdesk tickets yet.
          </p>
        </div>
      ) : (
        <div className="data-panel">
          <div className="policy-table-wrap">
            <table className="policy-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Subject</th>
                  <th>Status</th>
                  <th>Created</th>
                  {isAdmin ? <th>Actions</th> : null}
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <strong>{ticket.employeeName}</strong>
                    </td>
                    <td>
                      {ticket.subject}
                      <p className="muted" style={{ margin: "4px 0 0", fontSize: "0.8rem" }}>
                        {ticket.description}
                      </p>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : "—"}</td>
                    {isAdmin ? (
                      <td>
                        <select
                          value={ticket.status}
                          disabled={busyId === String(ticket.id)}
                          onChange={(e) => handleStatus(ticket.id, e.target.value)}
                        >
                          <option value="OPEN">OPEN</option>
                          <option value="IN_PROGRESS">IN_PROGRESS</option>
                          <option value="RESOLVED">RESOLVED</option>
                          <option value="CLOSED">CLOSED</option>
                        </select>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="policy-cards">
            {tickets.map((ticket) => (
              <article key={ticket.id} className="policy-card">
                <div className="policy-card-header">
                  <strong>{ticket.subject}</strong>
                  <StatusBadge status={ticket.status} />
                </div>
                <div className="policy-card-meta">
                  <span>{ticket.employeeName}</span>
                  <span>{ticket.description}</span>
                  <span>{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : "—"}</span>
                </div>
                {isAdmin ? (
                  <select
                    value={ticket.status}
                    disabled={busyId === String(ticket.id)}
                    onChange={(e) => handleStatus(ticket.id, e.target.value)}
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="CLOSED">CLOSED</option>
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
