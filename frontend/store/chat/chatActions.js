import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function sendChatAction(token, message, conversationId) {
  return commonRequest(
    apiClient,
    Route(routesMapping.chat.Send),
    {
      message,
      conversationId: conversationId || undefined,
    },
    "POST",
  )(token);
}
