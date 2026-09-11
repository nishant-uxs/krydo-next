import crypto from "crypto";
import type { PresentationRequest } from "@shared/presentation";
import {
  PRESENTATION_REQUEST_VERSION,
  presentationRequestDeepLink,
} from "@shared/presentation";
import type { PresentationPolicy } from "@shared/presentation-policy";

/**
 * In-memory Presentation Request + challenge store.
 *
 * Guarantees (single process):
 *   - opaque UUID request ids
 *   - cryptographically random challenges (32 bytes hex)
 *   - TTL expiry
 *   - single-use challenge consume after successful verify
 *   - audience + verifier binding on the stored record
 *
 * Remaining limitation (same as SIWS nonces):
 *   Not safe across multi-instance serverless isolates. Swap for
 *   Firestore/Redis with the same API for horizontal scale.
 */

export interface StoredPresentationRequest extends PresentationRequest {
  /** Challenge lifecycle: open until successfully verified. */
  challengeConsumed: boolean;
  consumedAt: string | null;
}

interface IssueInput {
  verifier: string;
  audience: string;
  reason: string | null;
  requestedCredentials: PresentationRequest["requestedCredentials"];
  policy: PresentationPolicy;
  ttlSeconds: number;
  now?: Date;
}

const MAX_REQUESTS = 20_000;
const store = new Map<string, StoredPresentationRequest>();

function gc(nowMs: number) {
  if (store.size < MAX_REQUESTS) return;
  for (const [id, entry] of store) {
    if (new Date(entry.expiresAt).getTime() <= nowMs || entry.challengeConsumed) {
      store.delete(id);
    }
  }
}

/** Test helper. */
export function __resetPresentationRequestStoreForTests(): void {
  store.clear();
}

/** Test helper — force-expire a request without deleting it. */
export function __expirePresentationRequestForTests(requestId: string): void {
  const entry = store.get(requestId);
  if (entry) {
    store.set(requestId, {
      ...entry,
      expiresAt: new Date(Date.now() - 1_000).toISOString(),
    });
  }
}

export function issuePresentationRequest(input: IssueInput): StoredPresentationRequest {
  const now = input.now ?? new Date();
  gc(now.getTime());

  const id = crypto.randomUUID();
  const challenge = crypto.randomBytes(32).toString("hex");
  const expiresAt = new Date(now.getTime() + input.ttlSeconds * 1000);

  const record: StoredPresentationRequest = {
    id,
    version: PRESENTATION_REQUEST_VERSION,
    verifier: input.verifier,
    audience: input.audience,
    challenge,
    reason: input.reason,
    requestedCredentials: input.requestedCredentials,
    policy: input.policy,
    createdAt: now.toISOString(),
    expiresAt: expiresAt.toISOString(),
    deepLink: presentationRequestDeepLink(id),
    challengeConsumed: false,
    consumedAt: null,
  };

  store.set(id, record);
  return record;
}

export function getPresentationRequest(
  requestId: string,
): StoredPresentationRequest | null {
  return store.get(requestId) ?? null;
}

/**
 * Peek whether a challenge is still usable for this request (no side effects).
 */
export function isChallengeOpen(
  request: StoredPresentationRequest,
  challenge: string,
  now: Date = new Date(),
): { ok: true } | { ok: false; reason: string } {
  if (request.challengeConsumed) {
    return { ok: false, reason: "challenge already consumed (replay)" };
  }
  if (new Date(request.expiresAt).getTime() <= now.getTime()) {
    return { ok: false, reason: "presentation request expired" };
  }
  if (request.challenge !== challenge) {
    return { ok: false, reason: "challenge mismatch" };
  }
  return { ok: true };
}

/**
 * Atomic single-use consume. Delete-before-return pattern within one process:
 * we mark consumed first so a concurrent second verify cannot both succeed.
 */
export function consumePresentationChallenge(
  requestId: string,
  challenge: string,
  now: Date = new Date(),
): { ok: true; request: StoredPresentationRequest } | { ok: false; reason: string } {
  const entry = store.get(requestId);
  if (!entry) return { ok: false, reason: "presentation request not found" };

  const check = isChallengeOpen(entry, challenge, now);
  if (!check.ok) return check;

  const updated: StoredPresentationRequest = {
    ...entry,
    challengeConsumed: true,
    consumedAt: now.toISOString(),
  };
  store.set(requestId, updated);
  return { ok: true, request: updated };
}

/** Public view strips internal consume flags from the wire if desired. */
export function toPublicPresentationRequest(
  entry: StoredPresentationRequest,
): PresentationRequest {
  const {
    challengeConsumed: _c,
    consumedAt: _a,
    ...publicView
  } = entry;
  return publicView;
}
