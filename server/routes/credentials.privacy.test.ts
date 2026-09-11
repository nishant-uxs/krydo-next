/**
 * Regression tests for public `/api/verify` privacy.
 *
 * Exercises the route registration with mocked storage / blockchain so we
 * prove claimData never appears on the public verification response.
 */
import { describe, it, expect, vi, beforeEach } from "vitest";
import express from "express";
import request from "supertest";

vi.mock("../config", () => ({
  config: {
    JWT_SECRET: "test-secret-that-is-at-least-32-chars-long-for-vitest",
    NODE_ENV: "test",
    isProd: false,
  },
}));

vi.mock("../storage", () => ({
  storage: {
    getCredentialByHash: vi.fn(),
    getCredentialById: vi.fn(),
    getIssuerByAddress: vi.fn(),
    getWallet: vi.fn(),
    listAllCredentialsPaged: vi.fn(),
    listCredentialsForHolderPaged: vi.fn(),
  },
}));

vi.mock("../blockchain", () => ({
  verifyCredentialOnChain: vi.fn(async () => ({ valid: true })),
  isBlockchainReady: vi.fn(() => true),
  anchorCredentialRenewalOnChain: vi.fn(),
  waitForClientTx: vi.fn(),
}));

vi.mock("@shared/contracts", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@shared/contracts")>();
  return {
    ...actual,
    CREDENTIALS_ID: "",
    AUDIT_ID: "",
  };
});

vi.mock("../middleware/security", () => ({
  sensitiveLimiter: (_req: unknown, _res: unknown, next: () => void) => next(),
}));

import { assertNoClaimLeak } from "../privacy/public-credential";
import { storage } from "../storage";
import { registerCredentialRoutes } from "./credentials";

const HOLDER = "GBXFXNDLV4LSWA4VB7YIL5GBD7BVNR22SGBTDKMO2SBZZHDXSKZYCP7L";
const ISSUER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";
const HASH = "b".repeat(64);

const credentialDoc = {
  id: "22222222-2222-2222-2222-222222222222",
  credentialHash: HASH,
  issuerAddress: ISSUER,
  holderAddress: HOLDER,
  claimType: "income",
  claimSummary: "Annual income ₹18L — PAN ABCDE1234F",
  claimData: { value: "1800000", fields: { pan: "ABCDE1234F" } },
  status: "active",
  issuedAt: new Date("2026-01-01T00:00:00Z"),
  expiresAt: new Date("2027-01-01T00:00:00Z"),
  revokedAt: null,
};

describe("POST /api/verify — public privacy", () => {
  let app: express.Express;

  beforeEach(() => {
    vi.clearAllMocks();
    app = express();
    app.use(express.json());
    registerCredentialRoutes(app);

    vi.mocked(storage.getCredentialByHash).mockResolvedValue(credentialDoc as never);
    vi.mocked(storage.getCredentialById).mockResolvedValue(undefined as never);
    vi.mocked(storage.getIssuerByAddress).mockResolvedValue({
      name: "Employer Co",
      active: true,
    } as never);
  });

  it("returns a minimal verification result without claimData", async () => {
    const res = await request(app)
      .post("/api/verify")
      .send({ credentialHash: HASH })
      .expect(200);

    expect(res.body.valid).toBe(true);
    expect(res.body.credential).toBeTruthy();
    expect(res.body.credential.credentialHash).toBe(HASH);
    expect(res.body.credential.status).toBe("active");
    expect(res.body.verification).toEqual({
      onChainAnchor: true,
      issuerTrusted: true,
    });
    expect(res.body.credential).not.toHaveProperty("claimData");
    expect(res.body.credential).not.toHaveProperty("claimSummary");
    expect(JSON.stringify(res.body)).not.toContain("1800000");
    expect(JSON.stringify(res.body)).not.toContain("ABCDE1234F");
    expect(JSON.stringify(res.body)).not.toContain("PAN");
    expect(() => assertNoClaimLeak(res.body)).not.toThrow();
  });

  it("still works when credential is missing (no leak)", async () => {
    vi.mocked(storage.getCredentialByHash).mockResolvedValue(undefined as never);
    vi.mocked(storage.getCredentialById).mockResolvedValue(undefined as never);

    const res = await request(app)
      .post("/api/verify")
      .send({ credentialHash: HASH })
      .expect(200);

    expect(res.body.valid).toBe(false);
    expect(res.body.credential).toBeNull();
    expect(() => assertNoClaimLeak(res.body)).not.toThrow();
  });
});

describe("GET /api/credentials/:address — authZ", () => {
  let app: express.Express;

  beforeEach(() => {
    vi.clearAllMocks();
    app = express();
    app.use(express.json());
    app.use((req, _res, next) => {
      const header = req.headers.authorization;
      if (header?.startsWith("Bearer ")) {
        try {
          const raw = Buffer.from(header.slice(7), "base64url").toString("utf8");
          req.auth = JSON.parse(raw);
        } catch {
          /* ignore */
        }
      }
      next();
    });
    registerCredentialRoutes(app);
  });

  it("rejects unauthenticated list access with 401", async () => {
    await request(app).get(`/api/credentials/${HOLDER}`).expect(401);
  });

  it("rejects non-owner list access with 403", async () => {
    const fakeAuth = Buffer.from(
      JSON.stringify({ sub: ISSUER, role: "issuer" }),
    ).toString("base64url");
    await request(app)
      .get(`/api/credentials/${HOLDER}`)
      .set("Authorization", `Bearer ${fakeAuth}`)
      .expect(403);
  });

  it("allows owner list access", async () => {
    vi.mocked(storage.getWallet).mockResolvedValue({
      address: HOLDER,
      role: "user",
    } as never);
    vi.mocked(storage.listCredentialsForHolderPaged).mockResolvedValue({
      items: [credentialDoc],
      nextCursor: null,
    } as never);

    const fakeAuth = Buffer.from(
      JSON.stringify({ sub: HOLDER, role: "user" }),
    ).toString("base64url");
    const res = await request(app)
      .get(`/api/credentials/${HOLDER}`)
      .set("Authorization", `Bearer ${fakeAuth}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(1);
    expect(res.body[0].claimData).toEqual(credentialDoc.claimData);
  });
});
