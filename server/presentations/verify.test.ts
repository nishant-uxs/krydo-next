/**
 * End-to-end presentation verify tests with mocked credential/issuer storage.
 */
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("../config", () => ({
  config: {
    JWT_SECRET: "test-secret-that-is-at-least-32-chars-long-for-vitest",
    NODE_ENV: "test",
    isProd: false,
  },
}));

vi.mock("../storage", () => ({
  storage: {
    getCredentialById: vi.fn(),
    getCredentialByHash: vi.fn(),
    getIssuerByAddress: vi.fn(),
    getZkProof: vi.fn(),
  },
}));

import { createPresentation } from "@shared/presentation";
import { assertNoClaimLeak } from "../privacy/public-credential";
import { storage } from "../storage";
import { verifyPresentation } from "./verify";
import {
  issuePresentationRequest,
  __resetPresentationRequestStoreForTests,
  __expirePresentationRequestForTests,
  toPublicPresentationRequest,
} from "./request-store";

const HOLDER = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H";
const ISSUER = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";
const VERIFIER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";
const CRED_ID = "11111111-1111-4111-8111-111111111111";
const HASH = "cd".repeat(32);

const credentialDoc = {
  id: CRED_ID,
  credentialHash: HASH,
  issuerAddress: ISSUER,
  holderAddress: HOLDER,
  claimType: "credit_score",
  claimSummary: "SECRET SUMMARY 820",
  claimData: { value: "820", pan: "ABCDE1234F" },
  status: "active",
  issuedAt: new Date("2026-01-01T00:00:00Z"),
  expiresAt: new Date("2099-01-01T00:00:00Z"),
  revokedAt: null,
};

async function issueStatusRequest(audience = "https://lender.example") {
  return issuePresentationRequest({
    verifier: VERIFIER,
    audience,
    reason: "kyc check",
    requestedCredentials: [{ claimType: "credit_score" }],
    policy: { kind: "credential_status", claimType: "credit_score" },
    ttlSeconds: 600,
  });
}

function buildVp(request: Awaited<ReturnType<typeof issueStatusRequest>>) {
  return createPresentation({
    request: toPublicPresentationRequest(request),
    holderAddress: HOLDER,
    credential: {
      id: CRED_ID,
      credentialHash: HASH,
      claimType: "credit_score",
      issuerAddress: ISSUER,
      holderAddress: HOLDER,
    },
  });
}

describe("verifyPresentation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    __resetPresentationRequestStoreForTests();
    vi.mocked(storage.getCredentialById).mockResolvedValue(credentialDoc as never);
    vi.mocked(storage.getCredentialByHash).mockResolvedValue(credentialDoc as never);
    vi.mocked(storage.getIssuerByAddress).mockResolvedValue({
      name: "CIBIL",
      active: true,
    } as never);
  });

  it("accepts a valid status presentation and leaks no claims", async () => {
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    const result = await verifyPresentation(vp);

    expect(result.valid).toBe(true);
    expect(result.checks.challenge).toBe(true);
    expect(result.checks.audience).toBe(true);
    expect(result.checks.credentialStatus).toBe(true);
    expect(result.checks.issuerTrusted).toBe(true);
    expect(result.checks.proof).toBe(true);
    expect(JSON.stringify(result)).not.toContain("820");
    expect(JSON.stringify(result)).not.toContain("ABCDE1234F");
    expect(JSON.stringify(result)).not.toContain("SECRET");
    expect(() => assertNoClaimLeak(result)).not.toThrow();
  });

  it("rejects replay of the same VP", async () => {
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    const first = await verifyPresentation(vp);
    expect(first.valid).toBe(true);

    const second = await verifyPresentation(vp);
    expect(second.valid).toBe(false);
    expect(second.message).toMatch(/replay|consumed/i);
  });

  it("rejects wrong challenge", async () => {
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    vp.challenge = "b".repeat(64);
    vp.proof.challenge = "b".repeat(64);
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/challenge/i);
  });

  it("rejects wrong audience", async () => {
    const request = await issueStatusRequest("https://lender-a.example");
    const vp = buildVp(request);
    vp.domain = "https://lender-b.example";
    vp.proof.domain = "https://lender-b.example";
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/audience|domain/i);
  });

  it("rejects expired request", async () => {
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    await __expirePresentationRequestForTests(request.id);
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/expired/i);
  });

  it("rejects malformed VP", async () => {
    const result = await verifyPresentation({ type: ["Nope"] });
    expect(result.valid).toBe(false);
    expect(result.checks.structure).toBe(false);
  });

  it("rejects missing holder DID binding", async () => {
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    vp.holder = "did:example:attacker";
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/holder/i);
  });

  it("rejects revoked credential", async () => {
    vi.mocked(storage.getCredentialById).mockResolvedValue({
      ...credentialDoc,
      status: "revoked",
    } as never);
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.checks.credentialStatus).toBe(false);
  });

  it("rejects expired credential", async () => {
    vi.mocked(storage.getCredentialById).mockResolvedValue({
      ...credentialDoc,
      expiresAt: new Date("2020-01-01T00:00:00Z"),
    } as never);
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/expired/i);
  });

  it("rejects untrusted issuer", async () => {
    vi.mocked(storage.getIssuerByAddress).mockResolvedValue({
      name: "Revoked Bureau",
      active: false,
    } as never);
    const request = await issueStatusRequest();
    const vp = buildVp(request);
    const result = await verifyPresentation(vp);
    expect(result.valid).toBe(false);
    expect(result.checks.issuerTrusted).toBe(false);
  });
});
