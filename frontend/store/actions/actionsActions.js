import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function confirmAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.actions.Confirm}/${id}/confirm`),
    {},
    "POST",
  )(token);
}

export async function cancelAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.actions.Cancel}/${id}/cancel`),
    {},
    "POST",
  )(token);
}
