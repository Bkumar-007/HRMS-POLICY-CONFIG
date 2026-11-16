"use client";

import { useState } from "react";
import Button from "@component/common/Button";

export default function ChatInput({ onSend, disabled, preferredLanguage, onLanguageChange }) {
  const [text, setText] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    const value = text.trim();
    if (!value || disabled) {
      return;
    }
    setText("");
    await onSend(value);
  }

  return (
    <form className="chat-input chat-input-with-lang" onSubmit={handleSubmit}>
      <label className="chat-lang">
        <span className="sr-only">Reply language</span>
        <select
          value={preferredLanguage || "AUTO"}
          onChange={(e) => onLanguageChange?.(e.target.value)}
          disabled={disabled}
          aria-label="Reply language"
        >
          <option value="AUTO">Auto</option>
          <option value="EN">English</option>
          <option value="HI">हिन्दी</option>
        </select>
      </label>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Ask in English or Hindi… try “show me the clause” or “test me on WFH”"
        disabled={disabled}
        aria-label="Message"
      />
      <Button type="submit" icon="send" disabled={disabled || !text.trim()}>
        Send
      </Button>
    </form>
  );
}
