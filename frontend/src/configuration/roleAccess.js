/**
 * Role-based navigation and route access.
 *
 * EMPLOYEE  — self-service (chat, leave, policies browse, profile)
 * MANAGER   — team ops (people, leave approvals, breaches, helpdesk)
 * HR_ADMIN  — full portal including policy admin + role assignment
 */

export const ROLES = {
  EMPLOYEE: "EMPLOYEE",
  MANAGER: "MANAGER",
  HR_ADMIN: "HR_ADMIN",
};

/** null roles = every authenticated user */
export const NAV_ITEMS = [
  {
    href: "/chat",
    label: "Assistant",
    icon: "chat",
    description: "Ask HR questions",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/leave",
    label: "Leave",
    icon: "calendar",
    description: "Requests & approvals",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/policies",
    label: "Policies",
    icon: "policies",
    description: "Policy library",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/people",
    label: "People",
    icon: "users",
    description: "Directory & roles",
    roles: [ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/breaches",
    label: "Breaches",
    icon: "alert",
    description: "Policy violations",
    roles: [ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/helpdesk",
    label: "Helpdesk",
    icon: "ticket",
    description: "Ticket inbox",
    roles: [ROLES.MANAGER, ROLES.HR_ADMIN],
  },
  {
    href: "/profile",
    label: "My profile",
    icon: "user",
    description: "Contact details",
    roles: [ROLES.EMPLOYEE, ROLES.MANAGER, ROLES.HR_ADMIN],
  },
];

export function canAccessPath(role, href) {
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
