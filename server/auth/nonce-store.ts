import crypto from "crypto";

/**
 * Short-lived nonce storage for Sign-in-with-Stellar challenges.
 *
 * Guarantees (single process):
 *   - cryptographically random nonces (16 bytes)
 *   - TTL expiry (5 minutes)
 *   - single-use consume (delete-before-validate)
 *   - address binding (nonce cannot authenticate a different wallet)
 *
 * Remaining limitation (P0 documented):
 *   In-memory Map is NOT safe across multiple serverless instances or
 *   horizontal replicas — a nonce issued on instance A cannot be consumed
 *   on instance B, and a replay could succeed if it lands on a different
 *   isolate that never saw the first consume. For true multi-instance
 *   safety, swap this store for Firestore/Redis with the same API.
 *   Do NOT introduce an external dependency solely for this P0 task.
 */
interface NonceEntry {
  nonce: string;
  address: string; // exact StrKey (case-sensitive)
  issuedAt: number;
  expiresAt: number;
}

const NONCE_TTL_MS = 5 * 60 * 1000; // 5 minutes
const MAX_NONCES = 10_000;

const store = new Map<string, NonceEntry>();

function gc(now: number) {
  if (store.size < MAX_NONCES) return;
  store.forEach((v, k) => {
    if (v.expiresAt <= now) store.delete(k);
  });
}

/** Test helper — clears the in-memory store. */
export function __resetNonceStoreForTests(): void {
  store.clear();
}

/** Test helper — force-expire an existing nonce without removing it. */
export function __expireNonceForTests(nonce: string): void {
  const entry = store.get(nonce);
  if (entry) {
    store.set(nonce, { ...entry, expiresAt: Date.now() - 1 });
  }
}

export function issueNonce(address: string): { nonce: string; expiresAt: number } {
  const addr = address.trim();
  const nonce = crypto.randomBytes(16).toString("hex");
  const now = Date.now();
  const expiresAt = now + NONCE_TTL_MS;
  gc(now);
  store.set(nonce, { nonce, address: addr, issuedAt: now, expiresAt });
  return { nonce, expiresAt };
}

/**
 * Single-use consume: returns true iff the nonce exists, matches the address,
 * hasn't expired, and removes it from the store in the process.
 * Delete-before-check ensures a concurrent double-consume cannot both succeed
 * within a single process.
 */
export function consumeNonce(nonce: string, address: string): boolean {
  const entry = store.get(nonce);
  if (!entry) return false;
  store.delete(nonce);
  if (entry.expiresAt < Date.now()) return false;
  return entry.address === address.trim();
}
