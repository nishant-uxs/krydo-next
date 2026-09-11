import { describe, it, expect, beforeEach } from "vitest";
import {
  MockVerificationClient,
  ApiError,
  HttpVerificationClient,
  __setVerificationClientForTests,
} from "../src/api/VerificationClient";
import { MockProofProver } from "../src/prover/ProofProver";
import { buildHolderPresentation } from "../src/api/buildPresentation";
import { DEMO_CREDENTIALS } from "../src/data/demo";

describe("VerificationClient mock", () => {
  beforeEach(() => {
    __setVerificationClientForTests(null);
  });

  it("loads a demo request", async () => {
    const client = new MockVerificationClient();
    const req = await client.getPresentationRequest("req_demo001");
    expect(req.id).toBe("req_demo001");
    expect(req.challenge).toHaveLength(64);
  });

  it("marks mock verify clearly", async () => {
    const client = new MockVerificationClient();
    const result = await client.verifyPresentation({});
    expect(result.message).toMatch(/DEMO|mock/i);
  });
});

describe("HttpVerificationClient errors", () => {
  it("surfaces network failure", async () => {
    const client = new HttpVerificationClient("http://127.0.0.1:9");
    // Force fetch fail by stubbing
    const original = globalThis.fetch;
    globalThis.fetch = async () => {
      throw new Error("offline");
    };
    try {
      await expect(client.getPresentationRequest("x")).rejects.toBeInstanceOf(ApiError);
    } finally {
      globalThis.fetch = original;
    }
  });

  it("surfaces HTTP errors", async () => {
    const client = new HttpVerificationClient("http://example.test");
    const original = globalThis.fetch;
    globalThis.fetch = async () =>
      new Response(JSON.stringify({ message: "gone" }), { status: 410 });
    try {
      await expect(client.getPresentationRequest("x")).rejects.toMatchObject({
        status: 410,
        message: "gone",
      });
    } finally {
      globalThis.fetch = original;
    }
  });

  it("rejects malformed JSON", async () => {
    const client = new HttpVerificationClient("http://example.test");
    const original = globalThis.fetch;
    globalThis.fetch = async () => new Response("not-json", { status: 200 });
    try {
      await expect(client.getPresentationRequest("x")).rejects.toBeInstanceOf(ApiError);
    } finally {
      globalThis.fetch = original;
    }
  });
});

describe("presentation flow + mock prover", () => {
  it("builds a clearly marked demo presentation using shared-compatible fields", async () => {
    const client = new MockVerificationClient();
    const request = await client.getPresentationRequest("req_flow");
    // Align claim type with demo credential
    request.requestedCredentials = [{ claimType: "identity_verification" }];
    request.policy = { kind: "credential_status", claimType: "identity_verification" };

    const proof = await new MockProofProver().prove({
      requestId: request.id,
      credentialId: DEMO_CREDENTIALS[0].id,
      claimType: DEMO_CREDENTIALS[0].claimType,
      policyKind: request.policy.kind,
      challenge: request.challenge,
      audience: request.audience,
    });

    expect(proof.isMock).toBe(true);
    expect(proof.label).toBe("DEMO / MOCK PROOF");

    const built = buildHolderPresentation({
      request,
      credential: DEMO_CREDENTIALS[0],
      proof,
    });

    expect(built.isMock).toBe(true);
    expect(built.kind).toBe("demo");
    expect(JSON.stringify(built.presentation)).toContain("DEMO / MOCK PROOF");
    expect(JSON.stringify(built.presentation)).not.toContain("claimData");
  });
});
