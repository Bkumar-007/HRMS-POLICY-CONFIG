"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMeAction, getSessionAction, updateProfileAction } from "@store/auth/authActions";
import AppShell from "@component/layout/AppShell";
import Button from "@component/common/Button";
import StatusBadge from "@component/common/StatusBadge";

export default function ProfileScreen() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [profile, setProfile] = useState(null);

  const load = useCallback(async (token) => {
    setLoading(true);
    setError("");
    try {
      const data = await fetchMeAction(token);
      setProfile(data);
      setDisplayName(data.displayName || "");
      setEmail(data.email || "");
      setPhone(data.phone || "");
    } catch (err) {
      setError(err.message || "Failed to load profile");
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

  async function handleSave(event) {
    event.preventDefault();
    if (!session?.token) {
      return;
    }
    setSaving(true);
    setError("");
    setInfo("");
    try {
      const updated = await updateProfileAction(session.token, {
        displayName: displayName.trim(),
        email: email.trim(),
        phone: phone.trim(),
      });
      setProfile(updated);
      setSession((prev) => (prev ? { ...prev, displayName: updated.displayName } : prev));
      setInfo("Profile updated. These details are used for password-reset OTP.");
    } catch (err) {
      setError(err.message || "Could not update profile");
    } finally {
      setSaving(false);
    }
  }

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  return (
    <AppShell
      displayName={session.displayName}
      role={session.role}
      title="My profile"
      subtitle="Update your contact details used for OTP and directory."
    >
      {loading ? (
        <p className="muted">Loading profile...</p>
      ) : (
        <div className="profile-grid">
          <form className="policy-upload" onSubmit={handleSave}>
            <h2>Editable details</h2>
            <label className="field">
              <span className="field-label">Display name</span>
              <input
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={120}
                required
              />
            </label>
            <label className="field">
              <span className="field-label">Email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                required
              />
            </label>
            <label className="field">
              <span className="field-label">Mobile number</span>
              <input
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="10-digit mobile"
                inputMode="tel"
                required
              />
            </label>
            {error ? <p className="error">{error}</p> : null}
            {info ? <p className="success">{info}</p> : null}
            <Button type="submit" icon="user" loading={saving}>
              {saving ? "Saving..." : "Save profile"}
            </Button>
          </form>

          <aside className="policy-upload profile-readonly">
            <h2>Work details</h2>
            <p className="muted">Managed by HR — contact admin to change these.</p>
            <dl className="profile-meta">
              <div>
                <dt>Username</dt>
                <dd>{profile?.username || "—"}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>{profile?.role ? <StatusBadge status={profile.role} /> : "—"}</dd>
              </div>
              <div>
                <dt>Grade</dt>
                <dd>{profile?.grade || "—"}</dd>
              </div>
              <div>
                <dt>Department</dt>
                <dd>{profile?.department || "—"}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{profile?.location || "—"}</dd>
              </div>
              <div>
                <dt>Manager</dt>
                <dd>{profile?.managerName || "—"}</dd>
              </div>
            </dl>
          </aside>
        </div>
      )}
    </AppShell>
  );
}
