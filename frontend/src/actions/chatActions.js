import { httpRequest } from "../api/httpClient";

export async function sendChatAction(token, message, conversationId) {
  return httpRequest("/api/chat", {
    method: "POST",
    token,
    body: {
      message,
      conversationId: conversationId || undefined,
    },
  });
}