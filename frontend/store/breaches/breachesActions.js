import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function listBreachesAction(token) {
  return commonRequest(apiClient, Route(routesMapping.breaches.List), {}, "GET")(token);
}

export async function createBreachAction(token, payload) {
  return commonRequest(apiClient, Route(routesMapping.breaches.Create), payload, "POST")(token);
}

export async function updateBreachStatusAction(token, id, status) {
  return commonRequest(
    apiClient,
    Route(routesMapping.breaches.UpdateStatus(id)),
    { status },
    "PUT",
  )(token);
}
