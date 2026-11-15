"use client";

import { useState } from "react";
import Button from "@component/common/Button";

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
      <label className="field">
        <span className="field-label">Display name</span>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. WFH Policy 2026"
          maxLength={200}
          required
        />
      </label>
      <label className="field">
        <span className="field-label">
          Policy file (.md, .txt, .pdf, .doc, .docx, .odt, .rtf, .html)
        </span>
        <input
          type="file"
          accept=".md,.markdown,.txt,.pdf,.doc,.docx,.odt,.rtf,.html,.htm,text/markdown,text/plain,text/html,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.oasis.opendocument.text,application/rtf"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          required
        />
      </label>
      <div className="policy-upload-row">
        <label className="field">
          <span className="field-label">Effective date (optional)</span>
          <input type="date" value={effectiveDate} onChange={(e) => setEffectiveDate(e.target.value)} />
        </label>
        <label className="field">
          <span className="field-label">Grade filter (optional)</span>
          <input value={grade} onChange={(e) => setGrade(e.target.value)} placeholder="G4" />
        </label>
        <label className="field">
          <span className="field-label">Location filter (optional)</span>
          <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Pune" />
        </label>
      </div>
      {localError ? <p className="error">{localError}</p> : null}
      <Button type="submit" icon="upload" loading={submitting} disabled={submitting}>
        {submitting ? "Uploading & indexing..." : "Upload & ingest"}
      </Button>
    </form>
  );
}
