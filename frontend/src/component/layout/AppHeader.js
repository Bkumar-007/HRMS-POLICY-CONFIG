"use client";

import Link from "next/link";
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
        <p className="brand">Recoil HR Assistant</p>
        <p className="muted">
          {displayName} · {role}
        </p>
      </div>
      <nav className="app-nav">
        <Link href="/chat">Chat</Link>
        <Link href="/policies">Policies</Link>
        <button type="button" className="btn-secondary" onClick={handleLogout}>
          Log out
        </button>
      </nav>
    </header>
  );
}
