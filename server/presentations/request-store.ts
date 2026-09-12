import crypto from "crypto";
import type { PresentationRequest } from "@shared/presentation";
import {
  PRESENTATION_REQUEST_VERSION,
  presentationRequestDeepLink,
} from "@shared/presentation";
import type { PresentationPolicy } from "@shared/presentation-policy";
import { collections } from "../db";

/**
 * Presentation Request + challenge store.
 *
 * Production: Firestore (`presentationRequests`) for multi-instance safety.
 * Tests (`NODE_ENV=test`): in-memory Map.
 */

export interface StoredPresentationRequest extends PresentationRequest {
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

const MAX_MEMORY_REQUESTS = 20_000;
const memoryStore = new Map<string, StoredPresentationRequest>();

function useMemory(): boolean {
  return process.env.NODE_ENV === "test" || process.env.KRYDO_MEMORY_AUTH_STORE === "1";
}

function gcMemory(nowMs: number) {
  if (memoryStore.size < MAX_MEMORY_REQUESTS) return;
  for (const [id, entry] of memoryStore) {
    if (new Date(entry.expiresAt).getTime() <= nowMs || entry.challengeConsumed) {
      memoryStore.delete(id);
    }
  }
}

export function __resetPresentationRequestStoreForTests(): void {
  memoryStore.clear();
}

export async function __expirePresentationRequestForTests(requestId: string): Promise<void> {
  if (useMemory()) {
    const entry = memoryStore.get(requestId);
    if (entry) {
      memoryStore.set(requestId, {
        ...entry,
        expiresAt: new Date(Date.now() - 1_000).toISOString(),
      });
    }
    return;
  }
  const ref = collections.presentationRequests.doc(requestId);
  const snap = await ref.get();
  if (snap.exists) {
    await ref.update({ expiresAt: new Date(Date.now() - 1_000).toISOString() });
  }
}

export async function issuePresentationRequest(
  input: IssueInput,
): Promise<StoredPresentationRequest> {
  const now = input.now ?? new Date();
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

  if (useMemory()) {
    gcMemory(now.getTime());
    memoryStore.set(id, record);
    return record;
  }

  await collections.presentationRequests.doc(id).set(record);
  return record;
}

export async function getPresentationRequest(
  requestId: string,
): Promise<StoredPresentationRequest | null> {
  if (useMemory()) {
    return memoryStore.get(requestId) ?? null;
  }
  const snap = await collections.presentationRequests.doc(requestId).get();
  if (!snap.exists) return null;
  return snap.data() as StoredPresentationRequest;
}

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

export async function consumePresentationChallenge(
  requestId: string,
  challenge: string,
  now: Date = new Date(),
): Promise<{ ok: true; request: StoredPresentationRequest } | { ok: false; reason: string }> {
  if (useMemory()) {
    const entry = memoryStore.get(requestId);
    if (!entry) return { ok: false, reason: "presentation request not found" };
    const check = isChallengeOpen(entry, challenge, now);
    if (!check.ok) return check;
    const updated: StoredPresentationRequest = {
      ...entry,
      challengeConsumed: true,
      consumedAt: now.toISOString(),
    };
    memoryStore.set(requestId, updated);
    return { ok: true, request: updated };
  }

  const db = collections.presentationRequests.firestore;
  const ref = collections.presentationRequests.doc(requestId);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return { ok: false as const, reason: "presentation request not found" };
    const entry = snap.data() as StoredPresentationRequest;
    const check = isChallengeOpen(entry, challenge, now);
    if (!check.ok) return check;
    const updated: StoredPresentationRequest = {
      ...entry,
      challengeConsumed: true,
      consumedAt: now.toISOString(),
    };
    tx.set(ref, updated);
    return { ok: true as const, request: updated };
  });
}

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
