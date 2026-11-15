"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { logoutAction } from "@store/auth/authActions";
import { navItemsForRole } from "@configuration/roleAccess";
import Button from "@component/common/Button";
import Icon from "@component/common/Icon";

export default function AppShell({ displayName, role, title, subtitle, actions, children }) {
  const pathname = usePathname();
  const router = useRouter();
  const [navOpen, setNavOpen] = useState(false);

  const navItems = useMemo(() => navItemsForRole(role), [role]);

  function handleLogout() {
    logoutAction();
    router.replace("/login");
  }

  const initials = (displayName || "U")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || "")
    .join("");

  return (
    <div className="portal">
      <aside className={`portal-sidebar ${navOpen ? "is-open" : ""}`}>
        <div className="portal-brand">
          <span className="portal-brand-mark" aria-hidden="true">
            <Icon name="spark" size={18} />
          </span>
          <div>
            <p className="portal-brand-name">Recoil HR</p>
            <p className="portal-brand-sub">Employee portal</p>
          </div>
          <button
            type="button"
            className="portal-sidebar-close"
            onClick={() => setNavOpen(false)}
            aria-label="Close navigation"
          >
            <Icon name="close" size={18} />
          </button>
        </div>

        <nav className="portal-nav" aria-label="Primary">
          {navItems.map((item) => {
            const active = pathname === item.href || pathname?.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`portal-nav-item ${active ? "is-active" : ""}`}
                onClick={() => setNavOpen(false)}
              >
                <Icon name={item.icon} size={18} />
                <span>
                  <strong>{item.label}</strong>
                  <small>{item.description}</small>
                </span>
              </Link>
            );
          })}
        </nav>

        <div className="portal-sidebar-footer">
          <div className="portal-user-chip">
            <span className="portal-avatar" aria-hidden="true">
              {initials}
            </span>
            <div>
              <strong>{displayName}</strong>
              <small>{formatRole(role)}</small>
            </div>
          </div>
          <Button variant="ghost" icon="logout" onClick={handleLogout} className="portal-logout">
            Log out
          </Button>
        </div>
      </aside>

      {navOpen ? (
        <button
          type="button"
          className="portal-backdrop"
          aria-label="Close navigation"
          onClick={() => setNavOpen(false)}
        />
      ) : null}

      <div className="portal-main">
        <header className="portal-topbar">
          <div className="portal-topbar-left">
            <button
              type="button"
              className="portal-menu-btn"
              onClick={() => setNavOpen(true)}
              aria-label="Open navigation"
            >
              <Icon name="menu" size={20} />
            </button>
            <div>
              <h1>{title}</h1>
              {subtitle ? <p className="muted">{subtitle}</p> : null}
            </div>
          </div>
          <div className="portal-topbar-actions">
            {actions}
            <div className="portal-topbar-user">
              <span className="portal-avatar portal-avatar-sm" aria-hidden="true">
                {initials}
              </span>
              <div className="portal-topbar-user-text">
                <strong>{displayName}</strong>
                <small>{formatRole(role)}</small>
              </div>
            </div>
          </div>
        </header>

        <main className="portal-content">{children}</main>
      </div>
    </div>
  );
}

function formatRole(role) {
  if (!role) {
    return "Employee";
  }
  return String(role)
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
