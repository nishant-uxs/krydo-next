/**
 * Parse Krydo presentation request URIs from QR / deep links.
 * Never blindly navigate to arbitrary URLs.
 */

export type QrParseResult =
  | { ok: true; requestId: string; source: "krydo" | "https" }
  | { ok: false; reason: string };

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
/** Allow opaque demo ids like req_abc123 during foundation testing. */
const OPAQUE_ID_RE = /^[A-Za-z0-9_-]{6,128}$/;

function isValidRequestId(id: string): boolean {
  return UUID_RE.test(id) || OPAQUE_ID_RE.test(id);
}

export function parseKrydoPresentationUri(raw: string): QrParseResult {
  const trimmed = raw.trim();
  if (!trimmed) return { ok: false, reason: "Empty URI" };

  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    return { ok: false, reason: "Invalid URI" };
  }

  if (url.protocol === "krydo:") {
    // krydo://present?request=<id>
    const host = url.hostname || url.host;
    if (host !== "present") {
      return { ok: false, reason: "Unsupported krydo host" };
    }
    const requestId = url.searchParams.get("request")?.trim() ?? "";
    if (!requestId) return { ok: false, reason: "Missing request parameter" };
    if (!isValidRequestId(requestId)) {
      return { ok: false, reason: "Invalid request id" };
    }
    return { ok: true, requestId, source: "krydo" };
  }

  if (url.protocol === "https:" || url.protocol === "http:") {
    // https://krydo.dev/present?request=<id>  OR  .../present/<id>
    const path = url.pathname.replace(/\/+$/, "");
    if (!path.endsWith("/present") && !/\/present\//.test(path)) {
      return { ok: false, reason: "HTTPS path is not a Krydo present link" };
    }
    let requestId = url.searchParams.get("request")?.trim() ?? "";
    if (!requestId) {
      const parts = path.split("/");
      requestId = parts[parts.length - 1] === "present" ? "" : parts[parts.length - 1];
    }
    if (!requestId) return { ok: false, reason: "Missing request parameter" };
    if (!isValidRequestId(requestId)) {
      return { ok: false, reason: "Invalid request id" };
    }
    return { ok: true, requestId, source: "https" };
  }

  return { ok: false, reason: "Unsupported URI scheme" };
}

export class QRRequestParser {
  parse(raw: string): QrParseResult {
    return parseKrydoPresentationUri(raw);
  }
}
