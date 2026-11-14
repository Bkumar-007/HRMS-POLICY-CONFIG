/**
 * Low-level HTTP helper.
 * Pages/components must use feature actions — not this module directly.
 */

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

async function parseError(response) {
  try {
    const body = await response.json();
    return body.message || response.statusText;
  } catch {
    return response.statusText || "Request failed";
  }
}

export async function httpRequest(path, options = {}) {
  const { method = "GET", body, token, headers = {} } = options;

  const finalHeaders = {
    "Content-Type": "application/json",
    ...headers,
  };

  if (token) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: finalHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}