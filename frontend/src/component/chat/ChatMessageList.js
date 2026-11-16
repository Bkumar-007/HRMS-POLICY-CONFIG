"use client";

import { useState } from "react";
import Button from "@component/common/Button";
import HtmlViewer from "@component/common/HtmlViewer";
import ClauseModal from "@component/chat/ClauseModal";
import PolicyQuizCard from "@component/chat/PolicyQuizCard";

export default function ChatMessageList({
  messages,
  sending = false,
  onConfirmAction,
  onCancelAction,
  onOpenPolicy,
  actionBusyId = "",
}) {
  const [activeCitation, setActiveCitation] = useState(null);

  if (!messages.length && !sending) {
    return (
      <div className="chat-empty">
        Ask about HR policies (English or Hindi). Try “show me the clause”,
        a short policy quiz, or leave balance — answers stay in chat with citations.
      </div>
    );
  }

  return (
    <>
      <div className="chat-list">
        {messages.map((item) => (
          <div
            key={item.id}
            className={`bubble bubble-${item.role}${item.error ? " bubble-error" : ""}`}
          >
            {item.role === "assistant" && !item.error ? (
              <HtmlViewer value={item.text} className="chat-rich-answer" />
            ) : (
              <p className="chat-plain-text">{item.text}</p>
            )}
            {item.citations?.length ? (
              <ul className="citations">
                {item.citations.map((citation, index) => (
                  <li key={`${item.id}-c-${index}`}>
                    <button
                      type="button"
                      className="citation-link"
                      onClick={() => setActiveCitation(citation)}
                    >
                      <strong>{citation.source}</strong>
                      {citation.section ? ` · ${citation.section}` : ""}
                    </button>
                    {citation.snippet ? (
                      <span className="citation-snippet"> — {citation.snippet}</span>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : null}
            {item.quiz?.length ? <PolicyQuizCard questions={item.quiz} /> : null}
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
                          <Button
                            disabled={busy || sending}
                            loading={busy}
                            onClick={() => onConfirmAction?.(action.id, item.id)}
                          >
                            Confirm
                          </Button>
                          <Button
                            variant="secondary"
                            disabled={busy || sending}
                            onClick={() => onCancelAction?.(action.id, item.id)}
                          >
                            Cancel
                          </Button>
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
      <ClauseModal
        citation={activeCitation}
        onClose={() => setActiveCitation(null)}
        onOpenPolicy={(policyId, section) => {
          setActiveCitation(null);
          onOpenPolicy?.(policyId, section);
        }}
      />
    </>
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
