const ApiServerURL =
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_BASE ||
  "http://localhost:8080";

export const AppSetting = {
  ApiServerURL: ApiServerURL.endsWith("/")
    ? ApiServerURL.slice(0, -1)
    : ApiServerURL,
};
