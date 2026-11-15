"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "@store/auth/authActions";
import {
  deletePolicyAction,
  disablePolicyAction,
  enablePolicyAction,
  ingestAllPoliciesAction,
  ingestPolicyAction,
  listPoliciesAction,
  uploadPolicyAction,
} from "@store/policies/policiesActions";
import AppHeader from "@component/layout/AppHeader";
import PolicyUploadForm from "@component/policies/PolicyUploadForm";
import PolicyList from "@component/policies/PolicyList";

export default function PoliciesScreen() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState("");
  const [bulkBusy, setBulkBusy] = useState(false);

  const load = useCallback(async (token) => {
    setLoading(true);
    setError("");
    try {
      const data = await listPoliciesAction(token);
      setPolicies(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load policies");
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
    load(current.token);
  }, [router, load]);

  const isAdmin = session?.role === "HR_ADMIN";

  async function runAdminAction(id, action, fallbackMessage) {
    if (!session?.token || !isAdmin) {
      return;
    }
    setBusyId(id || "bulk");
    setError("");
    try {
      await action();
      await load(session.token);
    } catch (err) {
      setError(err.message || fallbackMessage);
    } finally {
      setBusyId("");
    }
  }

  async function handleUpload(payload) {
    if (!session?.token || !isAdmin) {
      return;
    }
    setError("");
    try {
      await uploadPolicyAction(session.token, payload);
      await load(session.token);
    } catch (err) {
      setError(err.message || "Upload failed");
      throw err;
    }
  }

  async function handleIngestAll() {
    if (!session?.token || !isAdmin) {
      return;
    }
    setBulkBusy(true);
    setError("");
    try {
      await ingestAllPoliciesAction(session.token);
      await load(session.token);
    } catch (err) {
      setError(err.message || "Re-ingest all failed");
    } finally {
      setBulkBusy(false);
    }
  }

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  return (
    <div className="page page-chat">
      <div className="chat-shell">
        <AppHeader displayName={session.displayName} role={session.role} />
        <section className="policies-panel card">
          <div className="policies-heading">
            <div>
              <h1>Policies</h1>
              <p className="muted">
                Configure the HR policy library. Disabled policies stay on file but are not used in chat.
              </p>
            </div>
            <div className="policies-heading-actions">
              {isAdmin ? (
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={bulkBusy || loading}
                  onClick={handleIngestAll}
                >
                  {bulkBusy ? "Re-indexing…" : "Re-ingest all"}
                </button>
              ) : null}
              <button type="button" className="btn-secondary" onClick={() => router.push("/chat")}>
                Open chat
              </button>
            </div>
          </div>

          {isAdmin ? (
            <PolicyUploadForm onUpload={handleUpload} />
          ) : (
            <p className="muted">
              You are signed in as an employee — browse only. Log in as{" "}
              <strong>admin.hr</strong> to upload, disable, re-ingest, or delete policies.
            </p>
          )}

          {error ? <p className="error">{error}</p> : null}
          {loading ? (
            <p className="muted">Loading policies...</p>
          ) : (
            <PolicyList
              policies={policies}
              isAdmin={isAdmin}
              busyId={busyId}
              onDelete={(id) =>
                runAdminAction(id, () => deletePolicyAction(session.token, id), "Delete failed")
              }
              onIngest={(id) =>
                runAdminAction(id, () => ingestPolicyAction(session.token, id), "Re-ingest failed")
              }
              onDisable={(id) =>
                runAdminAction(id, () => disablePolicyAction(session.token, id), "Disable failed")
              }
              onEnable={(id) =>
                runAdminAction(id, () => enablePolicyAction(session.token, id), "Enable failed")
              }
            />
          )}
        </section>
      </div>
    </div>
  );
}
