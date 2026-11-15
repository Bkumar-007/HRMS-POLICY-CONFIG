"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "@store/auth/authActions";
import {
  approveLeaveAction,
  cancelLeaveAction,
  listLeaveRequestsAction,
  rejectLeaveAction,
} from "@store/leave/leaveActions";
import AppShell from "@component/layout/AppShell";
import Button from "@component/common/Button";
import StatusBadge from "@component/common/StatusBadge";

const PENDING = new Set(["PENDING_APPROVAL", "SUBMITTED"]);

export default function LeaveScreen() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState("");
  const [pendingOnly, setPendingOnly] = useState(true);
  const [notes, setNotes] = useState({});

  const canReview = session?.role === "HR_ADMIN" || session?.role === "MANAGER";

  const load = useCallback(async (token, onlyPending) => {
    setLoading(true);
    setError("");
    try {
      const data = await listLeaveRequestsAction(token, onlyPending);
      setRequests(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load leave requests");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const current = getSessionAction();
    if (!current?.token) {
      router.replace("/login");
      return;
    }
    setSession(current);
    // Managers default to pending queue; employees see all of theirs
    const onlyPending = current.role === "HR_ADMIN" || current.role === "MANAGER";
    setPendingOnly(onlyPending);
    load(current.token, onlyPending);
  }, [router, load]);

  const pendingCount = useMemo(
    () => requests.filter((r) => PENDING.has(r.status)).length,
    [requests],
  );

  async function handleApprove(id) {
    if (!session?.token) {
      return;
    }
    setBusyId(String(id));
    setError("");
    try {
      await approveLeaveAction(session.token, id, notes[id] || "");
      await load(session.token, pendingOnly);
    } catch (err) {
      setError(err.message || "Approve failed");
    } finally {
      setBusyId("");
    }
  }

  async function handleReject(id) {
    if (!session?.token) {
      return;
    }
    const note = notes[id]?.trim();
    if (!note) {
      setError("Add a short note before rejecting.");
      return;
    }
    setBusyId(String(id));
    setError("");
    try {
      await rejectLeaveAction(session.token, id, note);
      await load(session.token, pendingOnly);
    } catch (err) {
      setError(err.message || "Reject failed");
    } finally {
      setBusyId("");
    }
  }

  async function handleCancel(id) {
    if (!session?.token) {
      return;
    }
    if (!window.confirm("Cancel this leave request?")) {
      return;
    }
    setBusyId(String(id));
    setError("");
    try {
      await cancelLeaveAction(session.token, id);
      await load(session.token, pendingOnly);
    } catch (err) {
      setError(err.message || "Cancel failed");
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
      title={canReview ? "Leave approvals" : "My leave"}
      subtitle={
        canReview
          ? "Approve or reject leave requests from your team."
          : "Track leave you submitted through the assistant."
      }
      actions={
        canReview ? (
          <Button
            variant="secondary"
            onClick={() => {
              const next = !pendingOnly;
              setPendingOnly(next);
              load(session.token, next);
            }}
          >
            {pendingOnly ? "Show all" : "Pending only"}
          </Button>
        ) : null
      }
    >
      {canReview ? (
        <div className="module-toolbar">
          <p className="muted" style={{ margin: 0 }}>
            {pendingCount} awaiting decision
            {pendingOnly ? "" : " in the current list filter"}
          </p>
        </div>
      ) : (
        <p className="muted">
          Apply leave in <strong>Assistant</strong>, confirm the pending action, then your manager
          reviews it here.
        </p>
      )}

      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <p className="muted">Loading leave requests...</p>
      ) : requests.length === 0 ? (
        <div className="data-panel" style={{ padding: 16 }}>
          <p className="muted" style={{ margin: 0 }}>
            {pendingOnly ? "No leave requests waiting for approval." : "No leave requests yet."}
          </p>
        </div>
      ) : (
        <div className="data-panel">
          <div className="policy-table-wrap">
            <table className="policy-table">
              <thead>
                <tr>
                  {canReview ? <th>Employee</th> : null}
                  <th>Type</th>
                  <th>Dates</th>
                  <th>Days</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((item) => {
                  const awaiting = PENDING.has(item.status);
                  const mine = item.employeeId === session.employeeId;
                  return (
                    <tr key={item.id}>
                      {canReview ? (
                        <td>
                          <strong>{item.employeeName}</strong>
                        </td>
                      ) : null}
                      <td>{item.leaveType}</td>
                      <td>
                        {item.startDate} → {item.endDate}
                      </td>
                      <td>{item.days}</td>
                      <td>
                        {item.reason || "—"}
                        {item.reviewNote ? (
                          <p className="muted" style={{ margin: "4px 0 0", fontSize: "0.8rem" }}>
                            Review: {item.reviewNote}
                            {item.reviewerName ? ` · ${item.reviewerName}` : ""}
                          </p>
                        ) : null}
                      </td>
                      <td>
                        <StatusBadge status={item.status} />
                      </td>
                      <td>
                        {canReview && awaiting ? (
                          <div className="leave-actions">
                            <input
                              value={notes[item.id] || ""}
                              onChange={(e) =>
                                setNotes((prev) => ({ ...prev, [item.id]: e.target.value }))
                              }
                              placeholder="Note (required to reject)"
                            />
                            <div className="pending-action-buttons">
                              <Button
                                icon="check"
                                disabled={busyId === String(item.id)}
                                loading={busyId === String(item.id)}
                                onClick={() => handleApprove(item.id)}
                              >
                                Approve
                              </Button>
                              <Button
                                variant="danger"
                                icon="ban"
                                disabled={busyId === String(item.id)}
                                onClick={() => handleReject(item.id)}
                              >
                                Reject
                              </Button>
                            </div>
                          </div>
                        ) : mine && awaiting ? (
                          <Button
                            variant="secondary"
                            disabled={busyId === String(item.id)}
                            onClick={() => handleCancel(item.id)}
                          >
                            Cancel request
                          </Button>
                        ) : (
                          <span className="muted">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="policy-cards">
            {requests.map((item) => {
              const awaiting = PENDING.has(item.status);
              const mine = item.employeeId === session.employeeId;
              return (
                <article key={item.id} className="policy-card">
                  <div className="policy-card-header">
                    <strong>
                      {item.leaveType} · {item.days} day(s)
                    </strong>
                    <StatusBadge status={item.status} />
                  </div>
                  <div className="policy-card-meta">
                    {canReview ? <span>{item.employeeName}</span> : null}
                    <span>
                      {item.startDate} → {item.endDate}
                    </span>
                    <span>{item.reason || "No reason"}</span>
                    {item.reviewNote ? <span>Review: {item.reviewNote}</span> : null}
                  </div>
                  {canReview && awaiting ? (
                    <div className="leave-actions">
                      <input
                        value={notes[item.id] || ""}
                        onChange={(e) =>
                          setNotes((prev) => ({ ...prev, [item.id]: e.target.value }))
                        }
                        placeholder="Note (required to reject)"
                      />
                      <div className="pending-action-buttons">
                        <Button
                          icon="check"
                          disabled={busyId === String(item.id)}
                          onClick={() => handleApprove(item.id)}
                        >
                          Approve
                        </Button>
                        <Button
                          variant="danger"
                          icon="ban"
                          disabled={busyId === String(item.id)}
                          onClick={() => handleReject(item.id)}
                        >
                          Reject
                        </Button>
                      </div>
                    </div>
                  ) : null}
                  {mine && awaiting && !canReview ? (
                    <Button
                      variant="secondary"
                      disabled={busyId === String(item.id)}
                      onClick={() => handleCancel(item.id)}
                    >
                      Cancel request
                    </Button>
                  ) : null}
                </article>
              );
            })}
          </div>
        </div>
      )}
    </AppShell>
  );
}
