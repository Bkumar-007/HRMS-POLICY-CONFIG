"use client";

/**
 * @deprecated Prefer AppShell for authenticated pages.
 * Kept as a thin wrapper so older imports keep working.
 */
import AppShell from "./AppShell";

export default function AppHeader({ displayName, role, title = "Recoil HR", subtitle, actions, children }) {
  if (children) {
    return (
      <AppShell
        displayName={displayName}
        role={role}
        title={title}
        subtitle={subtitle}
        actions={actions}
      >
        {children}
      </AppShell>
    );
  }

  return null;
}
