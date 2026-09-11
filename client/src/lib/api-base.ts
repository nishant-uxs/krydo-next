/**
 * Shared API origin for web client.
 * - Empty / unset → same-origin `/api/...` (local Express, or Vercel rewrite → Render)
 * - Set `VITE_API_BASE_URL=https://krydo.onrender.com` to call Render directly from Vercel static.
 */
const RENDER_API_ORIGIN = "https://krydo.onrender.com";

export function apiBaseUrl(): string {
  const raw = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim();
  if (!raw) return "";
  return raw.replace(/\/$/, "");
}

/** Prefix relative `/api/...` paths with the configured API origin. */
export function apiUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  const base = apiBaseUrl();
  if (!base) return path;
  return `${base}${path.startsWith("/") ? path : `/${path}`}`;
}

export function isRemoteApi(): boolean {
  return Boolean(apiBaseUrl());
}
