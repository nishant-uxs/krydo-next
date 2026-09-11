/**
 * Mobile runtime config.
 * Never commit secrets. Use EXPO_PUBLIC_* only for non-secret values.
 */

export interface AppConfig {
  apiBaseUrl: string;
  useMockData: boolean;
  appName: string;
  scheme: string;
}

function trimSlash(url: string): string {
  return url.replace(/\/$/, "");
}

export function getConfig(): AppConfig {
  const raw = process.env.EXPO_PUBLIC_API_BASE_URL ?? "http://127.0.0.1:5000";
  const useMock =
    (process.env.EXPO_PUBLIC_USE_MOCK_DATA ?? "true").toLowerCase() !== "false";

  return {
    apiBaseUrl: trimSlash(raw),
    useMockData: useMock,
    appName: "Krydo",
    scheme: "krydo",
  };
}
