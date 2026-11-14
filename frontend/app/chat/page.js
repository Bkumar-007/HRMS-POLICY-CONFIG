"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "../../src/actions/authActions";
import { sendChatAction } from "../../src/actions/chatActions";
import AppHeader from "../../src/components/layout/AppHeader";
import ChatInput from "../../src/components/chat/ChatInput";
import ChatMessageList from "../../src/components/chat/ChatMessageList";

export default function ChatPage() {
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
      setError(err.message || "Chat failed");
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
          <ChatMessageList messages={messages} />
          {error ? <p className="error">{error}</p> : null}
        </section>
        <ChatInput onSend={handleSend} disabled={sending} />
      </div>
    </div>
  );
}