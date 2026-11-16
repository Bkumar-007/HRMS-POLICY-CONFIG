/**
 * Focused Policy Assistant — not a full HRMS.
 *
 * Core: chat (RAG + tools), policy library, profile.
 * Light ops only for managers: leave approvals + helpdesk escalations from chat.
 * People / breaches screens stay in the codebase but are out of the product nav.
 */

export const ROLES = {
  EMPLOYEE: "EMPLOYEE",
  MANAGER: "MANAGER",
  HR_ADMIN: "HR_ADMIN",
};

/** Primary product navigation (slim). */
export const NAV_ITEMS = [
  {
    href: "/chat",
    label: "Assistant",
    icon: "chat",
    description: "Policy Q&A + actions",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/policies",
    label: "Policies",
    icon: "policies",
    description: "Indexed library",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/leave",
    label: "Leave",
    icon: "calendar",
    description: "Approvals queue",
    roles: [ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/helpdesk",
    label: "Helpdesk",
    icon: "ticket",
    description: "Chat escalations",
    roles: [ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/profile",
    label: "Profile",
    icon: "user",
    description: "Your account",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
];

/** Routes that still exist but are not marketed as a full HR suite. */
export const SECONDARY_ROUTES = ["/people", "/breaches", "/leave"];

export function canAccessPath(role, href) {
  if (href === "/people" || href === "/breaches") {
    return role === ROLES.MANAGER || role === ROLES.HR_ADMIN;
  }
  if (href === "/leave") {
    // Employees apply leave from chat; managers/admins use the approvals screen.
    return role === ROLES.MANAGER || role === ROLES.HR_ADMIN || role === ROLES.EMPLOYEE;
  }
  const item = NAV_ITEMS.find((nav) => nav.href === href);
  if (!item) {
    return true;
  }
  return item.roles.includes(role);
}

export function navItemsForRole(role) {
  return NAV_ITEMS.filter((item) => item.roles.includes(role));
}

export function homePathForRole(role) {
  return "/chat";
}

export function isManagerOrHr(role) {
  return role === ROLES.MANAGER || role === ROLES.HR_ADMIN;
}

export function isHrAdmin(role) {
  return role === ROLES.HR_ADMIN;
}
