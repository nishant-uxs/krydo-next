import { describe, it, expect, beforeEach } from "vitest";
import {
  issueNonce,
  consumeNonce,
  __resetNonceStoreForTests,
  __expireNonceForTests,
} from "./nonce-store";

const ADDR = "GBXFXNDLV4LSWA4VB7YIL5GBD7BVNR22SGBTDKMO2SBZZHDXSKZYCP7L";
const OTHER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";

describe("SIWS nonce store", () => {
  beforeEach(() => {
    __resetNonceStoreForTests();
  });

  it("issues a cryptographically random hex nonce", async () => {
    const a = await issueNonce(ADDR);
    const b = await issueNonce(ADDR);
    expect(a.nonce).toMatch(/^[a-f0-9]{32}$/);
    expect(b.nonce).toMatch(/^[a-f0-9]{32}$/);
    expect(a.nonce).not.toBe(b.nonce);
    expect(a.expiresAt).toBeGreaterThan(Date.now());
  });

  it("consumes a valid nonce once", async () => {
    const { nonce } = await issueNonce(ADDR);
    expect(await consumeNonce(nonce, ADDR)).toBe(true);
    expect(await consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects replay of the same nonce", async () => {
    const { nonce } = await issueNonce(ADDR);
    expect(await consumeNonce(nonce, ADDR)).toBe(true);
    expect(await consumeNonce(nonce, ADDR)).toBe(false);
    expect(await consumeNonce(nonce, OTHER)).toBe(false);
  });

  it("rejects address substitution (wallet cannot be swapped)", async () => {
    const { nonce } = await issueNonce(ADDR);
    expect(await consumeNonce(nonce, OTHER)).toBe(false);
    // Original address also fails because consume deleted the entry first.
    expect(await consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects expired nonces", async () => {
    const { nonce } = await issueNonce(ADDR);
    await __expireNonceForTests(nonce);
    expect(await consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects unknown nonces", async () => {
    expect(await consumeNonce("deadbeefdeadbeefdeadbeefdeadbeef", ADDR)).toBe(false);
  });
});
