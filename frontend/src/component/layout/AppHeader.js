"use client";

import { useRouter } from "next/navigation";
import { logoutAction } from "@store/auth/authActions";

export default function AppHeader({ displayName, role }) {
  const router = useRouter();

  function handleLogout() {
    logoutAction();
    router.replace("/login");
  }

  return (
    <header className="app-header">
      <div>
        <p className="brand">Acme HR Assistant</p>
        <p className="muted">
          {displayName} · {role}
        </p>
      </div>
      <button type="button" className="btn-secondary" onClick={handleLogout}>
        Log out
      </button>
    </header>
  );
}
