"use client";

import { useState } from "react";

export default function PolicyUploadForm({ onUpload }) {
  const [name, setName] = useState("");
  const [file, setFile] = useState(null);
  const [effectiveDate, setEffectiveDate] = useState("");
  const [grade, setGrade] = useState("");
  const [location, setLocation] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [localError, setLocalError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    if (!name.trim() || !file) {
      setLocalError("Name and file are required");
      return;
    }
    setSubmitting(true);
    setLocalError("");
    try {
      await onUpload({
        name: name.trim(),
        file,
        effectiveDate: effectiveDate || undefined,
        grade: grade || undefined,
        location: location || undefined,
      });
      setName("");
      setFile(null);
      setEffectiveDate("");
      setGrade("");
      setLocation("");
      event.target.reset();
    } catch (err) {
      setLocalError(err.message || "Upload failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="policy-upload" onSubmit={handleSubmit}>
      <h2>Upload policy</h2>
      <label>
        Display name
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. WFH Policy 2026"
          maxLength={200}
          required
        />
      </label>
      <label>
        Policy file (.md, .txt, .pdf, .doc, .docx, .odt, .rtf, .html)
        <input
          type="file"
          accept=".md,.markdown,.txt,.pdf,.doc,.docx,.odt,.rtf,.html,.htm,text/markdown,text/plain,text/html,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.oasis.opendocument.text,application/rtf"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          required
        />
      </label>
      <div className="policy-upload-row">
        <label>
          Effective date (optional)
          <input type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
        </label>
        <label>
          Grade filter (optional)
          <input value={grade} onChange={(e) => setGrade(e.target.value)} placeholder="G4" />
        </label>
        <label>
          Location filter (optional)
          <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Pune" />
        </label>
      </div>
      {localError ? <p className="error">{localError}</p> : null}
      <button type="submit" disabled={submitting}>
        {submitting ? "Uploading & indexing..." : "Upload & ingest"}
      </button>
    </form>
  );
}
