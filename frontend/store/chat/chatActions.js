import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function sendChatAction(token, message, conversationId, preferredLanguage = "AUTO") {
  return commonRequest(
    apiClient,
    Route(routesMapping.chat.Send),
    {
      message,
      conversationId: conversationId || undefined,
      preferredLanguage: preferredLanguage || "AUTO",
    },
    "POST",
  )(token);
}
