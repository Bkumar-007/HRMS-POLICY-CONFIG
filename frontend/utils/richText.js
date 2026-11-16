/**
 * Shared rich-text helpers: sanitize HTML, convert markdown → HTML for AI answers.
 */

const ALLOWED_TAGS = new Set([
  "a",
  "b",
  "strong",
  "i",
  "em",
  "u",
  "s",
  "strike",
  "p",
  "br",
  "ul",
  "ol",
  "li",
  "h1",
  "h2",
  "h3",
  "h4",
  "blockquote",
  "code",
  "pre",
  "img",
  "span",
  "div",
  "hr",
]);

const ALLOWED_ATTRS = {
  a: new Set(["href", "title", "target", "rel"]),
  img: new Set(["src", "alt", "title"]),
  span: new Set(["class"]),
  div: new Set(["class"]),
  p: new Set(["class"]),
  code: new Set(["class"]),
  pre: new Set(["class"]),
};

function escapeHtml(text) {
  return String(text ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function isSafeUrl(url) {
  if (!url) {
    return false;
  }
  const value = String(url).trim();
  if (value.startsWith("#") || value.startsWith("/")) {
    return true;
  }
  return /^(https?:|mailto:|data:image\/(png|jpeg|jpg|gif|webp);base64,)/i.test(value);
}

/**
 * Browser-side HTML sanitizer (allowlist). Safe for AI / editor output.
 */
export function sanitizeHtml(dirty) {
  if (!dirty || typeof window === "undefined") {
    return "";
  }

  const template = document.createElement("template");
  template.innerHTML = String(dirty);
  const walk = (node) => {
    const children = [...node.childNodes];
    for (const child of children) {
      if (child.nodeType === Node.COMMENT_NODE) {
        child.remove();
        continue;
      }
      if (child.nodeType !== Node.ELEMENT_NODE) {
        continue;
      }
      const tag = child.tagName.toLowerCase();
      if (!ALLOWED_TAGS.has(tag)) {
        child.replaceWith(...child.childNodes);
        walk(node);
        continue;
      }

      [...child.attributes].forEach((attr) => {
        const name = attr.name.toLowerCase();
        const allowed = ALLOWED_ATTRS[tag];
        if (!allowed || !allowed.has(name)) {
          child.removeAttribute(attr.name);
          return;
        }
        if ((name === "href" || name === "src") && !isSafeUrl(attr.value)) {
          child.removeAttribute(attr.name);
        }
        if (name.startsWith("on")) {
          child.removeAttribute(attr.name);
        }
      });

      if (tag === "a") {
        child.setAttribute("rel", "noopener noreferrer");
        if (!child.getAttribute("target")) {
          child.setAttribute("target", "_blank");
        }
      }

      walk(child);
    }
  };

  walk(template.content);
  return template.innerHTML;
}

function inlineMarkdown(text) {
  let out = escapeHtml(text);
  // images ![alt](url)
  out = out.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_, alt, url) => {
    if (!isSafeUrl(url)) {
      return escapeHtml(alt || url);
    }
    return `<img src="${escapeHtml(url)}" alt="${escapeHtml(alt)}" />`;
  });
  // links [text](url)
  out = out.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, label, url) => {
    if (!isSafeUrl(url)) {
      return escapeHtml(label);
    }
    return `<a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${label}</a>`;
  });
  // bold ** ** or __ __
  out = out.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  out = out.replace(/__(.+?)__/g, "<strong>$1</strong>");
  // italic * * or _ _ (avoid already-tagged)
  out = out.replace(/(^|[^*\w])\*(?!\*)([^*]+?)\*(?!\*)/g, "$1<em>$2</em>");
  out = out.replace(/(^|[^_\w])_(?!_)([^_]+?)_(?!_)/g, "$1<em>$2</em>");
  // inline code
  out = out.replace(/`([^`]+)`/g, "<code>$1</code>");
  // strikethrough
  out = out.replace(/~~(.+?)~~/g, "<s>$1</s>");
  return out;
}

/**
 * Convert common Markdown (AI replies) into clean HTML.
 */
export function markdownToHtml(markdown) {
  const raw = String(markdown ?? "").replace(/\r\n/g, "\n").trim();
  if (!raw) {
    return "";
  }

  const lines = raw.split("\n");
  const blocks = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (!line.trim()) {
      i += 1;
      continue;
    }

    // fenced code
    if (line.trim().startsWith("```")) {
      const buf = [];
      i += 1;
      while (i < lines.length && !lines[i].trim().startsWith("```")) {
        buf.push(escapeHtml(lines[i]));
        i += 1;
      }
      i += 1;
      blocks.push(`<pre><code>${buf.join("\n")}</code></pre>`);
      continue;
    }

    const heading = /^(#{1,4})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      blocks.push(`<h${level}>${inlineMarkdown(heading[2])}</h${level}>`);
      i += 1;
      continue;
    }

    if (/^>\s?/.test(line)) {
      const quote = [];
      while (i < lines.length && /^>\s?/.test(lines[i])) {
        quote.push(lines[i].replace(/^>\s?/, ""));
        i += 1;
      }
      blocks.push(`<blockquote><p>${inlineMarkdown(quote.join(" "))}</p></blockquote>`);
      continue;
    }

    if (/^\s*([-*+])\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*([-*+])\s+/.test(lines[i])) {
        items.push(`<li>${inlineMarkdown(lines[i].replace(/^\s*([-*+])\s+/, ""))}</li>`);
        i += 1;
      }
      blocks.push(`<ul>${items.join("")}</ul>`);
      continue;
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items = [];
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(`<li>${inlineMarkdown(lines[i].replace(/^\s*\d+\.\s+/, ""))}</li>`);
        i += 1;
      }
      blocks.push(`<ol>${items.join("")}</ol>`);
      continue;
    }

    if (/^---+$/.test(line.trim())) {
      blocks.push("<hr />");
      i += 1;
      continue;
    }

    const para = [];
    while (
      i < lines.length &&
      lines[i].trim() &&
      !/^(#{1,4})\s+/.test(lines[i]) &&
      !/^>\s?/.test(lines[i]) &&
      !/^\s*([-*+])\s+/.test(lines[i]) &&
      !/^\s*\d+\.\s+/.test(lines[i]) &&
      !lines[i].trim().startsWith("```") &&
      !/^---+$/.test(lines[i].trim())
    ) {
      para.push(lines[i]);
      i += 1;
    }
    blocks.push(`<p>${inlineMarkdown(para.join(" "))}</p>`);
  }

  return blocks.join("");
}

export function looksLikeHtml(value) {
  return /<\/?[a-z][\s\S]*>/i.test(String(value ?? ""));
}

/**
 * Normalize AI / editor content into sanitized HTML for display.
 */
export function toDisplayHtml(value) {
  const text = String(value ?? "");
  if (!text.trim()) {
    return "";
  }
  const html = looksLikeHtml(text) ? text : markdownToHtml(text);
  return sanitizeHtml(html);
}

export function plainTextFromHtml(html) {
  if (!html || typeof window === "undefined") {
    return String(html ?? "").replace(/<[^>]+>/g, " ");
  }
  const div = document.createElement("div");
  div.innerHTML = sanitizeHtml(html);
  return (div.textContent || "").trim();
}
