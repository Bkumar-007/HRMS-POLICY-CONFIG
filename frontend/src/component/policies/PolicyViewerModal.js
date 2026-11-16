"use client";

import Button from "@component/common/Button";

export default function PolicyViewerModal({ policy, section, loading, error, onClose }) {
  if (!policy && !loading && !error) {
    return null;
  }

  const content = policy?.content || "";
  const highlight = section ? findSectionExcerpt(content, section) : "";

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal-card policy-viewer-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Policy document"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">Policy document</p>
            <h3>{policy?.name || "Loading…"}</h3>
            {section ? <p className="muted">Focus section: {section}</p> : null}
          </div>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
        {loading ? <p className="muted">Loading policy…</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {highlight ? (
          <blockquote className="clause-quote">{highlight}</blockquote>
        ) : null}
        {content ? (
          <pre className="policy-content-view">{content}</pre>
        ) : !loading && !error ? (
          <p className="muted">No content stored for this policy.</p>
        ) : null}
      </div>
    </div>
  );
}

function findSectionExcerpt(content, section) {
  if (!content || !section) {
    return "";
  }
  const lines = content.split(/\r?\n/);
  const needle = section.trim().toLowerCase();
  let start = -1;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith("#") && line.toLowerCase().includes(needle)) {
      start = i;
      break;
    }
  }
  if (start < 0) {
    const idx = content.toLowerCase().indexOf(needle);
    if (idx < 0) {
      return "";
    }
    return content.slice(idx, Math.min(content.length, idx + 320)).trim();
  }
  const chunk = [];
  for (let i = start; i < lines.length; i++) {
    if (i > start && /^#{1,6}\s+/.test(lines[i])) {
      break;
    }
    chunk.push(lines[i]);
    if (chunk.join("\n").length > 600) {
      break;
    }
  }
  return chunk.join("\n").trim();
}
