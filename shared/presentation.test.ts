import { describe, it, expect } from "vitest";
import {
  createPresentation,
  createPresentationRequestBodySchema,
  presentationRequestDeepLink,
  stellarDidFromAddress,
  addressFromStellarDid,
  verifiablePresentationSchema,
  type PresentationRequest,
} from "./presentation";
import {
  policyRequiresZk,
  policyToProofType,
  presentationPolicySchema,
} from "./presentation-policy";

const VERIFIER = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";
const HOLDER = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H";
const ISSUER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";

function sampleRequest(over: Partial<PresentationRequest> = {}): PresentationRequest {
  const id = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
  return {
    id,
    version: "krydo-vp-request-v1",
    verifier: VERIFIER,
    audience: "https://lender.example",
    challenge: "a".repeat(64),
    reason: "loan eligibility",
    requestedCredentials: [{ claimType: "credit_score" }],
    policy: { kind: "credential_status", claimType: "credit_score" },
    createdAt: new Date("2026-01-01T00:00:00.000Z").toISOString(),
    expiresAt: new Date("2099-01-01T00:00:00.000Z").toISOString(),
    deepLink: presentationRequestDeepLink(id),
    ...over,
  };
}

describe("presentation policy", () => {
  it("parses typed policies and rejects unknown kinds", () => {
    expect(
      presentationPolicySchema.parse({
        kind: "range_above",
        claimType: "credit_score",
        threshold: 700,
      }),
    ).toMatchObject({ kind: "range_above", threshold: 700 });
    expect(() =>
      presentationPolicySchema.parse({ kind: "age >= 18", claimType: "credit_score" }),
    ).toThrow();
  });

  it("maps policy to ZK proof types", () => {
    expect(policyRequiresZk({ kind: "credential_status", claimType: "credit_score" })).toBe(
      false,
    );
    expect(policyToProofType({ kind: "range_above", claimType: "credit_score", threshold: 1 })).toBe(
      "range_above",
    );
  });
});

describe("createPresentationRequestBodySchema", () => {
  it("accepts a valid create body", () => {
    const parsed = createPresentationRequestBodySchema.parse({
      requestedCredentials: [{ claimType: "credit_score" }],
      policy: { kind: "credential_status", claimType: "credit_score" },
    });
    expect(parsed.ttlSeconds).toBe(600);
  });

  it("rejects client-supplied challenge (strict)", () => {
    expect(() =>
      createPresentationRequestBodySchema.parse({
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
        challenge: "evil",
      }),
    ).toThrow();
  });
});

describe("createPresentation", () => {
  it("builds a valid VP without claimData", () => {
    const vp = createPresentation({
      request: sampleRequest(),
      holderAddress: HOLDER,
      credential: {
        id: "11111111-1111-4111-8111-111111111111",
        credentialHash: "ab".repeat(32),
        claimType: "credit_score",
        issuerAddress: ISSUER,
        holderAddress: HOLDER,
      },
      now: new Date("2026-06-01T00:00:00.000Z"),
    });

    expect(verifiablePresentationSchema.parse(vp).type).toContain("VerifiablePresentation");
    expect(vp.challenge).toBe(sampleRequest().challenge);
    expect(vp.domain).toBe("https://lender.example");
    expect(vp.holder).toBe(stellarDidFromAddress(HOLDER));
    expect(JSON.stringify(vp)).not.toContain("claimData");
    expect(vp.proof.type).toBe("KrydoCredentialReference2025");
  });

  it("requires ZK proof for range policies", () => {
    expect(() =>
      createPresentation({
        request: sampleRequest({
          policy: { kind: "range_above", claimType: "credit_score", threshold: 700 },
        }),
        holderAddress: HOLDER,
        credential: {
          id: "11111111-1111-4111-8111-111111111111",
          credentialHash: "ab".repeat(32),
          claimType: "credit_score",
          issuerAddress: ISSUER,
          holderAddress: HOLDER,
        },
      }),
    ).toThrow(/requires a ZK proof/);
  });

  it("rejects holder/credential substitution", () => {
    expect(() =>
      createPresentation({
        request: sampleRequest(),
        holderAddress: HOLDER,
        credential: {
          id: "11111111-1111-4111-8111-111111111111",
          credentialHash: "ab".repeat(32),
          claimType: "credit_score",
          issuerAddress: ISSUER,
          holderAddress: VERIFIER,
        },
      }),
    ).toThrow(/does not match/);
  });
});

describe("DID helpers", () => {
  it("round-trips stellar DID", () => {
    const did = stellarDidFromAddress(HOLDER);
    expect(addressFromStellarDid(did)).toBe(HOLDER);
  });

  it("builds deep link", () => {
    expect(presentationRequestDeepLink("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee")).toBe(
      "krydo://present?request=aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
    );
  });
});
