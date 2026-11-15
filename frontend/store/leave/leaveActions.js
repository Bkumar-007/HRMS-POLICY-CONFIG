import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function listLeaveRequestsAction(token, pendingOnly = false) {
  return commonRequest(
    apiClient,
    Route(routesMapping.leave.Requests),
    {},
    "GET",
    { pendingOnly: pendingOnly ? "true" : "false" },
  )(token);
}

export async function approveLeaveAction(token, id, note) {
  return commonRequest(
    apiClient,
    Route(routesMapping.leave.Approve(id)),
    { note: note || null },
    "POST",
  )(token);
}

export async function rejectLeaveAction(token, id, note) {
  return commonRequest(
    apiClient,
    Route(routesMapping.leave.Reject(id)),
    { note: note || null },
    "POST",
  )(token);
}

export async function cancelLeaveAction(token, id) {
  return commonRequest(apiClient, Route(routesMapping.leave.Cancel(id)), {}, "POST")(token);
}
