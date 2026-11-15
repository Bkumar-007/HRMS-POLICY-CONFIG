"use client";

export default function ChatMessageList({ messages, sending = false }) {
  if (!messages.length && !sending) {
    return (
      <div className="chat-empty">
        Ask about leave balance, holidays, or HR policies (WFH, travel, expenses…).
        Policy answers include citations from the indexed library.
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
          {item.citations?.length ? (
            <ul className="citations">
              {item.citations.map((citation, index) => (
                <li key={`${item.id}-c-${index}`}>
                  <strong>{citation.source}</strong>
                  {citation.section ? ` · ${citation.section}` : ""}
                  {citation.snippet ? <span className="citation-snippet"> — {citation.snippet}</span> : null}
                </li>
              ))}
            </ul>
          ) : null}
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
