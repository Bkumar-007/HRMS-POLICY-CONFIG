"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "@store/auth/authActions";
import { sendChatAction } from "@store/chat/chatActions";
import AppHeader from "@component/layout/AppHeader";
import ChatInput from "@component/chat/ChatInput";
import ChatMessageList from "@component/chat/ChatMessageList";

export default function ChatScreen() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [messages, setMessages] = useState([]);
  const [conversationId, setConversationId] = useState("");
  const [sending, setSending] = useState(false);
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

  if (!session) {
    return <div className="page">Loading...</div>;
  }

  return (
    <div className="page page-chat">
      <div className="chat-shell">
        <AppHeader displayName={session.displayName} role={session.role} />
        <section className="chat-panel">
          <ChatMessageList messages={messages} sending={sending} />
          {error ? <p className="error">{error}</p> : null}
        </section>
        <ChatInput onSend={handleSend} disabled={sending} />
      </div>
    </div>
  );
}
