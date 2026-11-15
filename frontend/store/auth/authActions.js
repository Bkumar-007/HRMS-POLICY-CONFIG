import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

const SESSION_KEY = "hr_assistant_session";

export async function loginAction(username, password) {
  const data = await commonRequest(
    apiClient,
    Route(routesMapping.auth.Login),
    { username, password },
    "POST",
  )();

  const session = {
    token: data.token,
    employeeId: data.employeeId,
    username: data.username,
    displayName: data.displayName,
    role: data.role,
  };

  if (typeof window !== "undefined") {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  return session;
}

export function logoutAction() {
  if (typeof window !== "undefined") {
    sessionStorage.removeItem(SESSION_KEY);
  }
}

export function getSessionAction() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export async function fetchMeAction(token) {
  return commonRequest(
    apiClient,
    Route(routesMapping.auth.Me),
    {},
    "GET",
  )(token);
}
