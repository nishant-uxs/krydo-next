import crypto from "crypto";
import { collections } from "../db";

/**
 * Short-lived nonce storage for SIWS / SIWE challenges.
 *
 * Production: Firestore (`authNonces`) so issue/consume works across
 * Render instances and restarts.
 * Tests (`NODE_ENV=test`): in-memory Map (no Firebase required).
 */
interface NonceEntry {
  nonce: string;
  address: string;
  chainId?: string;
  issuedAt: number;
  expiresAt: number;
}

const NONCE_TTL_MS = 5 * 60 * 1000;
const MAX_MEMORY_NONCES = 10_000;
const memoryStore = new Map<string, NonceEntry>();

function useMemory(): boolean {
  return process.env.NODE_ENV === "test" || process.env.KRYDO_MEMORY_AUTH_STORE === "1";
}

function gcMemory(now: number) {
  if (memoryStore.size < MAX_MEMORY_NONCES) return;
  memoryStore.forEach((v, k) => {
    if (v.expiresAt <= now) memoryStore.delete(k);
  });
}

/** Test helper — clears the in-memory store. */
export function __resetNonceStoreForTests(): void {
  memoryStore.clear();
}

/** Test helper — force-expire an existing nonce without removing it. */
export async function __expireNonceForTests(nonce: string): Promise<void> {
  if (useMemory()) {
    const entry = memoryStore.get(nonce);
    if (entry) memoryStore.set(nonce, { ...entry, expiresAt: Date.now() - 1 });
    return;
  }
  const ref = collections.authNonces.doc(nonce);
  const snap = await ref.get();
  if (snap.exists) {
    await ref.update({ expiresAt: Date.now() - 1 });
  }
}

export async function issueNonce(
  address: string,
  chainId?: string,
): Promise<{ nonce: string; expiresAt: number }> {
  const addr = address.trim();
  const nonce = crypto.randomBytes(16).toString("hex");
  const now = Date.now();
  const expiresAt = now + NONCE_TTL_MS;
  const entry: NonceEntry = {
    nonce,
    address: addr,
    chainId: chainId?.trim(),
    issuedAt: now,
    expiresAt,
  };

  if (useMemory()) {
    gcMemory(now);
    memoryStore.set(nonce, entry);
    return { nonce, expiresAt };
  }

  await collections.authNonces.doc(nonce).set(entry);
  return { nonce, expiresAt };
}

/**
 * Single-use consume: returns true iff the nonce exists, matches the address,
 * (and chainId when the entry was chain-bound), hasn't expired, and removes it.
 */
export async function consumeNonce(
  nonce: string,
  address: string,
  chainId?: string,
): Promise<boolean> {
  const addr = address.trim();
  const chain = chainId?.trim();

  if (useMemory()) {
    const entry = memoryStore.get(nonce);
    if (!entry) return false;
    memoryStore.delete(nonce);
    if (entry.expiresAt < Date.now()) return false;
    if (entry.address !== addr) return false;
    if (entry.chainId !== undefined) {
      if (!chain || entry.chainId !== chain) return false;
    }
    return true;
  }

  const db = collections.authNonces.firestore;
  const ref = collections.authNonces.doc(nonce);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return false;
    const entry = snap.data() as NonceEntry;
    tx.delete(ref);
    if (!entry || entry.expiresAt < Date.now()) return false;
    if (entry.address !== addr) return false;
    if (entry.chainId !== undefined) {
      if (!chain || entry.chainId !== chain) return false;
    }
    return true;
  });
}
