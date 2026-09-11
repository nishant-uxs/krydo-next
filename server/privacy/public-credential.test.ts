import { describe, it, expect } from "vitest";
import {
  toPublicCredentialView,
  assertNoClaimLeak,
} from "./public-credential";

const sample = {
  id: "11111111-1111-1111-1111-111111111111",
  credentialHash: "a".repeat(64),
  issuerAddress: "GBXFXNDLV4LSWA4VB7YIL5GBD7BVNR22SGBTDKMO2SBZZHDXSKZYCP7L",
  holderAddress: "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF",
  claimType: "credit_score",
  claimSummary: "Alice score 820 — SSN 123-45-6789",
  claimData: { value: "820", fields: { ssn: "123-45-6789" } },
  status: "active",
  issuedAt: new Date("2026-01-01T00:00:00Z"),
  expiresAt: new Date("2027-01-01T00:00:00Z"),
  revokedAt: null,
};

describe("toPublicCredentialView", () => {
  it("omits claimData and claimSummary", () => {
    const view = toPublicCredentialView(sample);
    expect(view).not.toHaveProperty("claimData");
    expect(view).not.toHaveProperty("claimSummary");
    expect(view.credentialHash).toBe(sample.credentialHash);
    expect(view.claimType).toBe("credit_score");
    expect(view.status).toBe("active");
    expect(() => assertNoClaimLeak({ valid: true, credential: view })).not.toThrow();
  });

  it("assertNoClaimLeak throws when claimData is present", () => {
    expect(() =>
      assertNoClaimLeak({ credential: { claimData: { value: "1" } } }),
    ).toThrow(/claimData/);
  });

  it("assertNoClaimLeak throws when claimSummary is present", () => {
    expect(() =>
      assertNoClaimLeak({ credential: { claimSummary: "secret" } }),
    ).toThrow(/claimSummary/);
  });
});
