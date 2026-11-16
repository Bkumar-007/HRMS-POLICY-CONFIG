"use client";

import Button from "@component/common/Button";
import HtmlViewer from "@component/common/HtmlViewer";

export default function ClauseModal({ citation, onClose, onOpenPolicy }) {
  if (!citation) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal-card clause-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Policy clause"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">Policy clause</p>
            <h3>{citation.source || "Policy"}</h3>
            {citation.section ? <p className="muted">{citation.section}</p> : null}
          </div>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
        <blockquote className="clause-quote">
          <HtmlViewer
            value={citation.snippet || "No snippet available for this citation."}
            className="clause-rich"
          />
        </blockquote>
        <div className="modal-actions">
          {citation.policyId ? (
            <Button
              onClick={() =>
                onOpenPolicy?.(citation.policyId, citation.section || "")
              }
            >
              Open full policy
            </Button>
          ) : null}
          <Button variant="secondary" onClick={onClose}>
            Done
          </Button>
        </div>
      </div>
    </div>
  );
}
