"use client";

import { useEffect, useId, useRef } from "react";
import { sanitizeHtml } from "@utils/richText";

const TOOLS = [
  { cmd: "bold", label: "B", title: "Bold", className: "is-bold" },
  { cmd: "italic", label: "I", title: "Italic", className: "is-italic" },
  { cmd: "underline", label: "U", title: "Underline", className: "is-underline" },
  { cmd: "strikeThrough", label: "S", title: "Strikethrough", className: "is-strike" },
  { cmd: "insertUnorderedList", label: "• List", title: "Bullet list" },
  { cmd: "insertOrderedList", label: "1. List", title: "Numbered list" },
  { cmd: "formatBlock", value: "h3", label: "H", title: "Heading" },
  { cmd: "formatBlock", value: "blockquote", label: "“”", title: "Quote" },
  { cmd: "removeFormat", label: "Clear", title: "Clear formatting" },
];

/**
 * Common HTML editor: bold, italic, underline, lists, links, images, headings, quotes.
 * value / onChange use HTML strings.
 */
export default function HtmlEditor({
  value = "",
  onChange,
  placeholder = "Write something…",
  disabled = false,
  className = "",
  minHeight = 140,
  ariaLabel = "Rich text editor",
}) {
  const editorRef = useRef(null);
  const lastHtml = useRef("");
  const inputId = useId();

  useEffect(() => {
    const el = editorRef.current;
    if (!el) {
      return;
    }
    const next = value || "";
    if (next !== lastHtml.current && next !== el.innerHTML) {
      el.innerHTML = next;
      lastHtml.current = next;
    }
  }, [value]);

  function emitChange() {
    const el = editorRef.current;
    if (!el) {
      return;
    }
    const html = sanitizeHtml(el.innerHTML);
    lastHtml.current = html;
    onChange?.(html);
  }

  function focusEditor() {
    editorRef.current?.focus();
  }

  function run(command, commandValue = null) {
    if (disabled) {
      return;
    }
    focusEditor();
    document.execCommand(command, false, commandValue);
    emitChange();
  }

  function insertLink() {
    if (disabled) {
      return;
    }
    const url = window.prompt("Link URL (https://…)", "https://");
    if (!url) {
      return;
    }
    const href = url.trim();
    if (!/^(https?:|mailto:|\/|#)/i.test(href)) {
      window.alert("Only http(s), mailto, or site-relative links are allowed.");
      return;
    }
    run("createLink", href);
  }

  function insertImage() {
    if (disabled) {
      return;
    }
    const url = window.prompt("Image URL (https://… or leave blank to upload)", "");
    if (url === null) {
      return;
    }
    if (url.trim()) {
      const href = url.trim();
      if (!/^(https?:|data:image\/)/i.test(href)) {
        window.alert("Only http(s) or data:image URLs are allowed.");
        return;
      }
      run("insertImage", href);
      return;
    }

    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/png,image/jpeg,image/jpg,image/gif,image/webp";
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) {
        return;
      }
      if (file.size > 1_500_000) {
        window.alert("Please choose an image under 1.5 MB.");
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        run("insertImage", String(reader.result || ""));
      };
      reader.readAsDataURL(file);
    };
    input.click();
  }

  return (
    <div className={`html-editor ${disabled ? "is-disabled" : ""} ${className}`.trim()}>
      <div className="html-editor-toolbar" role="toolbar" aria-label="Formatting">
        {TOOLS.map((tool) => (
          <button
            key={`${tool.cmd}-${tool.value || tool.label}`}
            type="button"
            className={`html-editor-btn ${tool.className || ""}`.trim()}
            title={tool.title}
            disabled={disabled}
            onMouseDown={(event) => {
              event.preventDefault();
              if (tool.cmd === "formatBlock") {
                run("formatBlock", tool.value);
              } else {
                run(tool.cmd);
              }
            }}
          >
            {tool.label}
          </button>
        ))}
        <button
          type="button"
          className="html-editor-btn"
          title="Insert link"
          disabled={disabled}
          onMouseDown={(event) => {
            event.preventDefault();
            insertLink();
          }}
        >
          Link
        </button>
        <button
          type="button"
          className="html-editor-btn"
          title="Insert image"
          disabled={disabled}
          onMouseDown={(event) => {
            event.preventDefault();
            insertImage();
          }}
        >
          Image
        </button>
      </div>
      <div
        id={inputId}
        ref={editorRef}
        className="html-editor-surface rich-text"
        contentEditable={!disabled}
        role="textbox"
        aria-multiline="true"
        aria-label={ariaLabel}
        data-placeholder={placeholder}
        style={{ minHeight }}
        onInput={emitChange}
        onBlur={emitChange}
        suppressContentEditableWarning
      />
    </div>
  );
}
