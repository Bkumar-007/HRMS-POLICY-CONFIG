"use client";

export default function ChatMessageList({ messages }) {
  if (!messages.length) {
    return (
      <div className="chat-empty">
        Ask about leave policy, travel rules, or say hello. Phase 1 is chat-only;
        tools come in Phase 2.
      </div>
    );
  }

  return (
    <div className="chat-list">
      {messages.map((item) => (
        <div key={item.id} className={`bubble bubble-${item.role}`}>
          <p>{item.text}</p>
        </div>
      ))}
    </div>
  );
}