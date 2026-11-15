import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function listPeopleAction(token) {
  return commonRequest(apiClient, Route(routesMapping.people.Directory), {}, "GET")(token);
}

export async function listTeamAction(token) {
  return commonRequest(apiClient, Route(routesMapping.people.Team), {}, "GET")(token);
}

export async function updateRoleAction(token, employeeId, role) {
  return commonRequest(
    apiClient,
    Route(routesMapping.people.UpdateRole(employeeId)),
    { role },
    "PUT",
  )(token);
}

export async function updateManagerAction(token, employeeId, managerId) {
  return commonRequest(
    apiClient,
    Route(routesMapping.people.UpdateManager(employeeId)),
    { managerId: managerId || null },
    "PUT",
  )(token);
}
