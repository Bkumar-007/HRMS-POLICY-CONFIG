"use client";

import { useState } from "react";
import Icon from "./Icon";

export default function PasswordField({
  id,
  label = "Password",
  value,
  onChange,
  placeholder,
  autoComplete = "current-password",
  required = false,
  disabled = false,
  name,
}) {
  const [visible, setVisible] = useState(false);
  const inputId = id || name || "password";

  return (
    <label className="field" htmlFor={inputId}>
      <span className="field-label">{label}</span>
      <div className="password-field">
        <input
          id={inputId}
          name={name}
          type={visible ? "text" : "password"}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          autoComplete={autoComplete}
          required={required}
          disabled={disabled}
        />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Hide password" : "Show password"}
          tabIndex={-1}
          disabled={disabled}
        >
          <Icon name={visible ? "eyeOff" : "eye"} size={18} />
        </button>
      </div>
    </label>
  );
}
