"use client";

export default function ChatMessageList({ messages, sending = false }) {
  if (!messages.length && !sending) {
    return (
      <div className="chat-empty">
        Ask about leave balance, holidays, or say hello. Policy Q&amp;A arrives in
        Phase 3.
      </div>
    );
  }

  return (
    <div className="chat-list">
      {messages.map((item) => (
        <div
          key={item.id}
          className={`bubble bubble-${item.role}${item.error ? " bubble-error" : ""}`}
        >
          <p>{item.text}</p>
        </div>
      ))}
      {sending ? (
        <div className="bubble bubble-assistant bubble-pending">
          <p>Generating response...</p>
        </div>
      ) : null}
    </div>
  );
}
