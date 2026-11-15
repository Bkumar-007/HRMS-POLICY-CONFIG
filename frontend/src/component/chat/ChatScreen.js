"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "@store/auth/authActions";
import { sendChatAction } from "@store/chat/chatActions";
import { cancelAction, confirmAction } from "@store/actions/actionsActions";
import AppShell from "@component/layout/AppShell";
import ChatInput from "@component/chat/ChatInput";
import ChatMessageList from "@component/chat/ChatMessageList";

export default function ChatScreen() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [messages, setMessages] = useState([]);
  const [conversationId, setConversationId] = useState("");
  const [sending, setSending] = useState(false);
  const [actionBusyId, setActionBusyId] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const current = getSessionAction();
    if (!current?.token) {
      router.replace("/login");
      return;
    }
    setSession(current);
  }, [router]);

  async function handleSend(text) {
    if (!session?.token) {
      return;
    }

    const userMessage = {
      id: `u-${Date.now()}`,
      role: "user",
      text,
    };
    setMessages((prev) => [...prev, userMessage]);
    setSending(true);
    setError("");

    try {
      const response = await sendChatAction(session.token, text, conversationId);
      if (response.conversationId) {
        setConversationId(response.conversationId);
      }
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: "assistant",
          text: response.message || "",
          citations: response.citations || [],
          pendingActions: response.pendingActions || [],
          actionResults: {},
        },
      ]);
    } catch (err) {
      const message =
        err.message ||
        "The AI model is busy or unavailable right now. Please try again in a moment.";
      setError(message);
      setMessages((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: "assistant",
          text: message,
          error: true,
        },
      ]);
    } finally {
      setSending(false);
    }
  }

  function patchActionResult(messageId, actionId, result) {
    setMessages((prev) =>
      prev.map((msg) => {
        if (msg.id !== messageId) {
          return msg;
        }
        return {
          ...msg,
          actionResults: {
            ...(msg.actionResults || {}),
            [actionId]: result,
          },
        };
      }),
    );
  }

  async function handleConfirmAction(actionId, messageId) {
    if (!session?.token) {
      return;
    }
    setActionBusyId(actionId);
    setError("");
    try {
      const result = await confirmAction(session.token, actionId);
      patchActionResult(messageId, actionId, result);
    } catch (err) {
      setError(err.message || "Confirm failed");
    } finally {
      setActionBusyId("");
    }
  }

  async function handleCancelAction(actionId, messageId) {
    if (!session?.token) {
      return;
    }
    setActionBusyId(actionId);
    setError("");
    try {
      const result = await cancelAction(session.token, actionId);
      patchActionResult(messageId, actionId, result);
    } catch (err) {
      setError(err.message || "Cancel failed");
    } finally {
      setActionBusyId("");
    }
  }

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  return (
    <AppShell
      displayName={session.displayName}
      role={session.role}
      title="HR Assistant"
      subtitle="Ask about leave, holidays, policies, or raise a helpdesk request."
    >
      <section className="chat-panel">
        <ChatMessageList
          messages={messages}
          sending={sending}
          actionBusyId={actionBusyId}
          onConfirmAction={handleConfirmAction}
          onCancelAction={handleCancelAction}
        />
        {error ? <p className="error">{error}</p> : null}
      </section>
      <ChatInput onSend={handleSend} disabled={sending || !!actionBusyId} />
    </AppShell>
  );
}
