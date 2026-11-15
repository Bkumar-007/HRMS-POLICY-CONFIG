import { AppSetting } from "@configuration/AppSetting";

async function parseError(response) {
  try {
    const body = await response.json();
    return body.message || response.statusText;
  } catch {
    return response.statusText || "Request failed";
  }
}

/**
 * Authenticated HTTP client (Bearer JWT when token is provided).
 * Returns the raw JSON body (including ApiResponse envelope when present).
 */
export async function apiClient(pathOrUrl, options = {}) {
  const { method = "GET", body, token, headers = {}, params } = options;

  let url = pathOrUrl.startsWith("http")
    ? pathOrUrl
    : `${AppSetting.ApiServerURL}${pathOrUrl.startsWith("/") ? "" : "/"}${pathOrUrl}`;

  if (params && Object.keys(params).length > 0) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        search.append(key, String(value));
      }
    });
    url += (url.includes("?") ? "&" : "?") + search.toString();
  }

  const finalHeaders = { ...headers };
  if (body !== undefined && !(body instanceof FormData) && !finalHeaders["Content-Type"]) {
    finalHeaders["Content-Type"] = "application/json";
  }
  if (token) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method,
    headers: finalHeaders,
    body:
      body === undefined
        ? undefined
        : typeof body === "string" || body instanceof FormData
          ? body
          : JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
