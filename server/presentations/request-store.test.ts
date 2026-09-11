import { describe, it, expect, beforeEach } from "vitest";
import {
  issuePresentationRequest,
  getPresentationRequest,
  consumePresentationChallenge,
  isChallengeOpen,
  __resetPresentationRequestStoreForTests,
  __expirePresentationRequestForTests,
} from "./request-store";

const VERIFIER = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";

describe("presentation request store", () => {
  beforeEach(() => {
    __resetPresentationRequestStoreForTests();
  });

  it("issues opaque id + random challenge", () => {
    const a = issuePresentationRequest({
      verifier: VERIFIER,
      audience: "https://a.example",
      reason: null,
      requestedCredentials: [{ claimType: "credit_score" }],
      policy: { kind: "credential_status", claimType: "credit_score" },
      ttlSeconds: 600,
    });
    const b = issuePresentationRequest({
      verifier: VERIFIER,
      audience: "https://a.example",
      reason: null,
      requestedCredentials: [{ claimType: "credit_score" }],
      policy: { kind: "credential_status", claimType: "credit_score" },
      ttlSeconds: 600,
    });

    expect(a.id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
    expect(a.challenge).toMatch(/^[a-f0-9]{64}$/);
    expect(a.challenge).not.toBe(b.challenge);
    expect(a.deepLink).toContain(a.id);
    expect(getPresentationRequest(a.id)?.verifier).toBe(VERIFIER);
  });

  it("rejects expired challenges", () => {
    const req = issuePresentationRequest({
      verifier: VERIFIER,
      audience: VERIFIER,
      reason: null,
      requestedCredentials: [{ claimType: "income_verification" }],
      policy: { kind: "credential_status", claimType: "income_verification" },
      ttlSeconds: 600,
    });
    __expirePresentationRequestForTests(req.id);
    expect(isChallengeOpen(getPresentationRequest(req.id)!, req.challenge).ok).toBe(false);
    expect(consumePresentationChallenge(req.id, req.challenge).ok).toBe(false);
  });

  it("consumes challenge once (replay protection)", () => {
    const req = issuePresentationRequest({
      verifier: VERIFIER,
      audience: "https://lender.example",
      reason: null,
      requestedCredentials: [{ claimType: "credit_score" }],
      policy: { kind: "credential_status", claimType: "credit_score" },
      ttlSeconds: 600,
    });
    expect(consumePresentationChallenge(req.id, req.challenge).ok).toBe(true);
    expect(consumePresentationChallenge(req.id, req.challenge).ok).toBe(false);
    expect(consumePresentationChallenge(req.id, req.challenge).ok).toBe(false);
  });

  it("rejects wrong challenge", () => {
    const req = issuePresentationRequest({
      verifier: VERIFIER,
      audience: "https://lender.example",
      reason: null,
      requestedCredentials: [{ claimType: "credit_score" }],
      policy: { kind: "credential_status", claimType: "credit_score" },
      ttlSeconds: 600,
    });
    expect(consumePresentationChallenge(req.id, "b".repeat(64)).ok).toBe(false);
    // Original still usable after wrong attempt.
    expect(consumePresentationChallenge(req.id, req.challenge).ok).toBe(true);
  });
});
