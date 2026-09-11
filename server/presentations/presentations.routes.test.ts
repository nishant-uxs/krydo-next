/**
 * HTTP-level presentation route tests.
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
    getCredentialById: vi.fn(),
    getCredentialByHash: vi.fn(),
    getIssuerByAddress: vi.fn(),
    getZkProof: vi.fn(),
  },
}));

vi.mock("../middleware/security", () => ({
  sensitiveLimiter: (_req: unknown, _res: unknown, next: () => void) => next(),
}));

import { storage } from "../storage";
import { registerPresentationRoutes } from "../routes/presentations";
import {
  __resetPresentationRequestStoreForTests,
} from "./request-store";
import { assertNoClaimLeak } from "../privacy/public-credential";

const HOLDER = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H";
const ISSUER = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP";
const VERIFIER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";
const OTHER = "GBLZQQXJ3W3XHL3N7Q7H2W5YV5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y";
const CRED_ID = "11111111-1111-4111-8111-111111111111";
const HASH = "ef".repeat(32);

function authHeader(sub: string, role = "user") {
  return `Bearer ${Buffer.from(JSON.stringify({ sub, role })).toString("base64url")}`;
}

function buildApp() {
  const app = express();
  app.use(express.json());
  app.use((req, _res, next) => {
    const header = req.headers.authorization;
    if (header?.startsWith("Bearer ")) {
      try {
        req.auth = JSON.parse(
          Buffer.from(header.slice(7), "base64url").toString("utf8"),
        );
      } catch {
        /* ignore */
      }
    }
    next();
  });
  registerPresentationRoutes(app);
  return app;
}

describe("presentation routes", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    __resetPresentationRequestStoreForTests();
    vi.mocked(storage.getCredentialById).mockResolvedValue({
      id: CRED_ID,
      credentialHash: HASH,
      issuerAddress: ISSUER,
      holderAddress: HOLDER,
      claimType: "credit_score",
      claimSummary: "secret",
      claimData: { value: "900" },
      status: "active",
      issuedAt: new Date("2026-01-01"),
      expiresAt: new Date("2099-01-01"),
      revokedAt: null,
    } as never);
    vi.mocked(storage.getIssuerByAddress).mockResolvedValue({
      name: "Bureau",
      active: true,
    } as never);
  });

  it("requires auth to create a presentation request", async () => {
    const app = buildApp();
    await request(app)
      .post("/api/presentations/request")
      .send({
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
      })
      .expect(401);
  });

  it("creates a request with server-generated challenge (ignores client challenge via strict)", async () => {
    const app = buildApp();
    const res = await request(app)
      .post("/api/presentations/request")
      .set("Authorization", authHeader(VERIFIER))
      .send({
        audience: "https://bank.example",
        reason: "loan",
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
        challenge: "client-supplied-should-fail",
      })
      .expect(400);

    expect(res.body.message).toBeTruthy();

    const ok = await request(app)
      .post("/api/presentations/request")
      .set("Authorization", authHeader(VERIFIER))
      .send({
        audience: "https://bank.example",
        reason: "loan",
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
      })
      .expect(201);

    expect(ok.body.challenge).toMatch(/^[a-f0-9]{64}$/);
    expect(ok.body.verifier).toBe(VERIFIER);
    expect(ok.body.deepLink).toMatch(/^krydo:\/\/present\?request=/);
    expect(ok.body.id).toMatch(/^[0-9a-f-]{36}$/i);
    expect(() => assertNoClaimLeak(ok.body)).not.toThrow();
  });

  it("retrieves request publicly by opaque id", async () => {
    const app = buildApp();
    const created = await request(app)
      .post("/api/presentations/request")
      .set("Authorization", authHeader(VERIFIER))
      .send({
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
      })
      .expect(201);

    const got = await request(app)
      .get(`/api/presentations/request/${created.body.id}`)
      .expect(200);

    expect(got.body.id).toBe(created.body.id);
    expect(got.body.challenge).toBe(created.body.challenge);
    expect(() => assertNoClaimLeak(got.body)).not.toThrow();
  });

  it("holder can create VP; non-holder cannot", async () => {
    const app = buildApp();
    const created = await request(app)
      .post("/api/presentations/request")
      .set("Authorization", authHeader(VERIFIER))
      .send({
        audience: "https://bank.example",
        requestedCredentials: [{ claimType: "credit_score" }],
        policy: { kind: "credential_status", claimType: "credit_score" },
      })
      .expect(201);

    await request(app)
      .post("/api/presentations/create")
      .set("Authorization", authHeader(OTHER))
      .send({ requestId: created.body.id, credentialId: CRED_ID })
      .expect(403);

    const vpRes = await request(app)
      .post("/api/presentations/create")
      .set("Authorization", authHeader(HOLDER))
      .send({ requestId: created.body.id, credentialId: CRED_ID })
      .expect(201);

    expect(vpRes.body.type).toContain("VerifiablePresentation");
    expect(() => assertNoClaimLeak(vpRes.body)).not.toThrow();

    const verify1 = await request(app)
      .post("/api/presentations/verify")
      .send({ presentation: vpRes.body })
      .expect(200);
    expect(verify1.body.valid).toBe(true);
    expect(() => assertNoClaimLeak(verify1.body)).not.toThrow();

    const verify2 = await request(app)
      .post("/api/presentations/verify")
      .send({ presentation: vpRes.body })
      .expect(400);
    expect(verify2.body.valid).toBe(false);
  });
});
