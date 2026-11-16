"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { logoutAction } from "@store/auth/authActions";
import { navItemsForRole } from "@configuration/roleAccess";
import Button from "@component/common/Button";
import Icon from "@component/common/Icon";

const SIDEBAR_PIN_KEY = "recoil.hr.sidebarPinned";

export default function AppShell({ displayName, role, title, subtitle, actions, children }) {
  const pathname = usePathname();
  const router = useRouter();
  const [navOpen, setNavOpen] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [hovered, setHovered] = useState(false);

  const navItems = useMemo(() => navItemsForRole(role), [role]);
  // Pin changes layout width; hover only overlays labels without shifting content.
  const layoutExpanded = pinned;
  const labelsExpanded = pinned || hovered || navOpen;

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(SIDEBAR_PIN_KEY);
      if (saved === "1") {
        setPinned(true);
      }
    } catch {
      // ignore storage errors
    }
  }, []);

  useEffect(() => {
    setNavOpen(false);
  }, [pathname]);

  function handleLogout() {
    logoutAction();
    router.replace("/login");
  }

  function togglePin() {
    setPinned((prev) => {
      const next = !prev;
      try {
        window.localStorage.setItem(SIDEBAR_PIN_KEY, next ? "1" : "0");
      } catch {
        // ignore
      }
      return next;
    });
  }

  const initials = (displayName || "U")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || "")
    .join("");

  return (
    <div className={`portal ${layoutExpanded ? "sidebar-expanded" : "sidebar-collapsed"}`}>
      <aside
        className={`portal-sidebar ${navOpen ? "is-open" : ""} ${labelsExpanded ? "is-expanded" : "is-collapsed"}`}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        <div className="portal-brand">
          <span className="portal-brand-mark" aria-hidden="true">
            <Icon name="spark" size={18} />
          </span>
          <div className="portal-brand-copy">
            <p className="portal-brand-name">Recoil HR</p>
            <p className="portal-brand-sub">Policy assistant</p>
          </div>
          <button
            type="button"
            className="portal-sidebar-close"
            onClick={() => setNavOpen(false)}
            aria-label="Close navigation"
          >
            <Icon name="close" size={18} />
          </button>
          <button
            type="button"
            className="portal-pin-btn"
            onClick={togglePin}
            aria-pressed={pinned}
            aria-label={pinned ? "Collapse sidebar" : "Keep sidebar expanded"}
            title={pinned ? "Unpin sidebar" : "Pin sidebar open"}
          >
            <Icon name={pinned ? "chevronLeft" : "chevronRight"} size={16} />
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
                title={`${item.label} — ${item.description}`}
                onClick={() => setNavOpen(false)}
              >
                <Icon name={item.icon} size={18} />
                <span className="portal-nav-copy">
                  <strong>{item.label}</strong>
                  <small>{item.description}</small>
                </span>
              </Link>
            );
          })}
        </nav>

        <div className="portal-sidebar-footer">
          <div className="portal-user-chip" title={`${displayName} · ${formatRole(role)}`}>
            <span className="portal-avatar" aria-hidden="true">
              {initials}
            </span>
            <div className="portal-user-copy">
              <strong>{displayName}</strong>
              <small>{formatRole(role)}</small>
            </div>
          </div>
          <Button
            variant="ghost"
            icon="logout"
            onClick={handleLogout}
            className="portal-logout"
            title="Log out"
          >
            <span className="portal-logout-label">Log out</span>
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
            <button
              type="button"
              className="portal-desktop-toggle"
              onClick={togglePin}
              aria-pressed={pinned}
              aria-label={pinned ? "Collapse sidebar" : "Expand sidebar"}
              title={pinned ? "Collapse sidebar" : "Expand sidebar"}
            >
              <Icon name="panelLeft" size={18} />
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
