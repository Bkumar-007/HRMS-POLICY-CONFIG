"use client";

import { useMemo } from "react";
import { toDisplayHtml } from "@utils/richText";

/**
 * Read-only rich text. Accepts HTML or Markdown (AI answers) and renders sanitized HTML.
 */
export default function HtmlViewer({
  value = "",
  className = "",
  as: Tag = "div",
  emptyFallback = null,
}) {
  const html = useMemo(() => toDisplayHtml(value), [value]);

  if (!html) {
    return emptyFallback;
  }

  return (
    <Tag
      className={`html-viewer rich-text ${className}`.trim()}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
