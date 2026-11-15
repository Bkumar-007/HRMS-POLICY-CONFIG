"use client";

import { useState } from "react";

export default function ChatInput({ onSend, disabled }) {
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
    <form className="chat-input" onSubmit={handleSubmit}>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Type a message..."
        disabled={disabled}
      />
      <button type="submit" disabled={disabled || !text.trim()}>
        Send
      </button>
    </form>
  );
}
