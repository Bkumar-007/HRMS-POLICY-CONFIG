import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function listHelpdeskTicketsAction(token) {
  return commonRequest(apiClient, Route(routesMapping.helpdesk.Tickets), {}, "GET")(token);
}

export async function updateHelpdeskTicketStatusAction(token, id, status) {
  return commonRequest(
    apiClient,
    Route(routesMapping.helpdesk.UpdateTicketStatus(id)),
    { status },
    "PUT",
  )(token);
}
