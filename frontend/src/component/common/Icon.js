const PATHS = {
  chat: "M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9l-4 3v-3H6a2 2 0 0 1-2-2V6z",
  policies:
    "M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm7 1.5V8h3.5L14 4.5zM8 12h8M8 16h8M8 8h4",
  logout: "M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M14 16l4-4-4-4M18 12H10",
  send: "M4 12l16-7-7 16-2-7-7-2z",
  eye: "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
  eyeOff:
    "M3 3l18 18M10.6 10.6A3 3 0 0 0 13.4 13.4M9.9 5.1A10.5 10.5 0 0 1 12 5c6.5 0 10 7 10 7a18.4 18.4 0 0 1-4.2 4.8M6.1 6.1A18.2 18.2 0 0 0 2 12s3.5 7 10 7c1.4 0 2.7-.3 3.9-.8",
  upload: "M12 16V5m0 0l-4 4m4-4l4 4M5 19h14",
  refresh:
    "M20 12a8 8 0 1 1-2.3-5.6M20 4v5h-5",
  ban: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM7.5 7.5l9 9",
  check: "M5 13l4 4L19 7",
  trash: "M5 7h14M10 11v6M14 11v6M8 7l1-2h6l1 2M7 7l1 12a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2l1-12",
  menu: "M4 7h16M4 12h16M4 17h16",
  close: "M6 6l12 12M18 6L6 18",
  user: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-7 9a7 7 0 0 1 14 0",
  lock: "M8 11V8a4 4 0 1 1 8 0v3M7 11h10v9H7v-9z",
  mail: "M4 6h16v12H4V6zm0 0l8 7 8-7",
  phone: "M7 3h3l1.5 4-2 1.5a12 12 0 0 0 5 5L16.5 12 20.5 13.5v3A15 15 0 0 1 7 3z",
  search: "M11 18a7 7 0 1 1 0-14 7 7 0 0 1 0 14zm6-1 4 4",
  home: "M4 11l8-7 8 7v9a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9z",
  spark: "M12 3l1.5 5.5L19 10l-5.5 1.5L12 17l-1.5-5.5L5 10l5.5-1.5L12 3z",
  users:
    "M16 11a4 4 0 1 0-3.2-6.4A4 4 0 0 0 16 11zM8 12a3.5 3.5 0 1 0-2.8-5.6A3.5 3.5 0 0 0 8 12zM3.5 20a4.5 4.5 0 0 1 9 0M12.5 20a5.5 5.5 0 0 1 8.5 0",
  alert:
    "M12 3l9 16H3L12 3zm0 6v4m0 3h.01",
  ticket:
    "M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8z",
  shield:
    "M12 3l8 3v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-3z",
  calendar:
    "M7 4V2m10 2V2M4 8h16M5 5h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zm3 7h.01M12 12h.01M15 12h.01M8 16h.01M12 16h.01M16 16h.01",
  panelLeft:
    "M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zm5 0v14",
  chevronLeft: "M15 6l-6 6 6 6",
  chevronRight: "M9 6l6 6-6 6",
};

export default function Icon({ name, size = 18, className = "", strokeWidth = 1.75 }) {
  const d = PATHS[name];
  if (!d) {
    return null;
  }

  return (
    <svg
      className={`ui-icon ${className}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={d} />
    </svg>
  );
}
