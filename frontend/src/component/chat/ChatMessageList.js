"use client";

export default function ChatMessageList({
  messages,
  sending = false,
  onConfirmAction,
  onCancelAction,
  actionBusyId = "",
}) {
  if (!messages.length && !sending) {
    return (
      <div className="chat-empty">
        Ask about leave balance, holidays, or HR policies (WFH, travel, expenses…).
        Policy answers include citations from the indexed library. Leave apply and
        helpdesk tickets need Confirm / Cancel.
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
                  {citation.snippet ? (
                    <span className="citation-snippet"> — {citation.snippet}</span>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : null}
          {item.pendingActions?.length ? (
            <div className="pending-actions">
              {item.pendingActions.map((action) => {
                const resolved = item.actionResults?.[action.id];
                const busy = actionBusyId === action.id;
                return (
                  <div key={action.id} className="pending-action-card">
                    <p className="pending-action-summary">
                      <strong>{formatActionType(action.type)}</strong>
                      {action.summary ? ` — ${action.summary}` : ""}
                    </p>
                    {resolved ? (
                      <p className={`pending-action-result status-${resolved.status?.toLowerCase()}`}>
                        {resolved.message || resolved.status}
                      </p>
                    ) : (
                      <div className="pending-action-buttons">
                        <button
                          type="button"
                          className="btn"
                          disabled={busy || sending}
                          onClick={() => onConfirmAction?.(action.id, item.id)}
                        >
                          {busy ? "Working…" : "Confirm"}
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          disabled={busy || sending}
                          onClick={() => onCancelAction?.(action.id, item.id)}
                        >
                          Cancel
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
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

function formatActionType(type) {
  if (!type) {
    return "Pending action";
  }
  return String(type)
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
