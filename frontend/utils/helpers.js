/**
 * commonRequest(apiClient, url, body, method, params) → (token?) => Promise<data>
 *
 * Expects backend envelope:
 *   { code: 1|0, data, message, count? }
 * code 1 = success, code 0 = failure.
 * Resolves to `data` on success; throws Error(message) on failure.
 */
export const commonRequest =
  (client, url, data, type = "GET", params = {}, extra = {}) =>
  async (token) => {
    const res = await client(url, {
      method: type,
      body:
        data === undefined || (data && Object.keys(data).length === 0 && type === "GET")
          ? undefined
          : data,
      params,
      token,
      ...extra,
    });

    if (res == null) {
      return null;
    }

    // Envelope: { code, data, message, count }
    if (typeof res === "object" && "code" in res) {
      if (res.code === 0) {
        throw new Error(res.message || "Request failed");
      }
      return res.data;
    }

    // Fallback for any non-enveloped response
    return res;
  };
