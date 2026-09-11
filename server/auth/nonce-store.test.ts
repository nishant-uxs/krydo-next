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

  it("issues a cryptographically random hex nonce", () => {
    const a = issueNonce(ADDR);
    const b = issueNonce(ADDR);
    expect(a.nonce).toMatch(/^[a-f0-9]{32}$/);
    expect(b.nonce).toMatch(/^[a-f0-9]{32}$/);
    expect(a.nonce).not.toBe(b.nonce);
    expect(a.expiresAt).toBeGreaterThan(Date.now());
  });

  it("consumes a valid nonce once", () => {
    const { nonce } = issueNonce(ADDR);
    expect(consumeNonce(nonce, ADDR)).toBe(true);
    expect(consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects replay of the same nonce", () => {
    const { nonce } = issueNonce(ADDR);
    expect(consumeNonce(nonce, ADDR)).toBe(true);
    expect(consumeNonce(nonce, ADDR)).toBe(false);
    expect(consumeNonce(nonce, OTHER)).toBe(false);
  });

  it("rejects address substitution (wallet cannot be swapped)", () => {
    const { nonce } = issueNonce(ADDR);
    expect(consumeNonce(nonce, OTHER)).toBe(false);
    // Original address also fails because consume deleted the entry first.
    expect(consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects expired nonces", () => {
    const { nonce } = issueNonce(ADDR);
    __expireNonceForTests(nonce);
    expect(consumeNonce(nonce, ADDR)).toBe(false);
  });

  it("rejects unknown nonces", () => {
    expect(consumeNonce("deadbeefdeadbeefdeadbeefdeadbeef", ADDR)).toBe(false);
  });
});
