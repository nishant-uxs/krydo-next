/**
 * Safe logging — never dump tokens, keys, claimData, or full presentations.
 */

const SENSITIVE = /token|password|secret|claimData|claimSummary|authorization|private/i;

function isDev(): boolean {
  return typeof globalThis !== "undefined" &&
    (globalThis as { __DEV__?: boolean }).__DEV__ === true;
}

export function safeLog(message: string, meta?: Record<string, unknown>): void {
  if (!isDev()) return;
  if (!meta) {
    console.info(`[krydo] ${message}`);
    return;
  }
  const redacted: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(meta)) {
    redacted[k] = SENSITIVE.test(k) ? "[REDACTED]" : v;
  }
  console.info(`[krydo] ${message}`, redacted);
}
