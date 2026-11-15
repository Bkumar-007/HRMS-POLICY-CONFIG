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
import AppShell from "@component/layout/AppShell";
import Button from "@component/common/Button";
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
    <AppShell
      displayName={session.displayName}
      role={session.role}
      title="Policy library"
      subtitle="Manage documents used by the HR assistant for grounded answers."
      actions={
        <>
          {isAdmin ? (
            <Button
              variant="secondary"
              icon="refresh"
              loading={bulkBusy}
              disabled={loading}
              onClick={handleIngestAll}
            >
              {bulkBusy ? "Re-indexing…" : "Re-ingest all"}
            </Button>
          ) : null}
          <Button variant="secondary" icon="chat" onClick={() => router.push("/chat")}>
            Open chat
          </Button>
        </>
      }
    >
      <section className="policies-panel">
        {isAdmin ? (
          <PolicyUploadForm onUpload={handleUpload} />
        ) : (
          <div className="card" style={{ padding: 16 }}>
            <p className="muted" style={{ margin: 0 }}>
              You are signed in as an employee — browse only. Log in as{" "}
              <strong>admin.hr</strong> to upload, disable, re-ingest, or delete policies.
            </p>
          </div>
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
    </AppShell>
  );
}
