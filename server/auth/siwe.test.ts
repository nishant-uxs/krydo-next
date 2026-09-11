import { describe, it, expect, beforeEach } from "vitest";
import { SiweMessage } from "siwe";
import { generatePrivateKey, privateKeyToAccount } from "viem/accounts";
import { __resetNonceStoreForTests, issueNonce, consumeNonce } from "./nonce-store";
import { SUPPORTED_EVM_NUMERIC_IDS, evmCaip2, normalizeEvmAddress } from "@shared/wallet";

describe("SIWE nonce binding", () => {
  beforeEach(() => {
    __resetNonceStoreForTests();
  });

  it("issues and consumes address+chain bound nonce", () => {
    const address = normalizeEvmAddress("0x1111111111111111111111111111111111111111");
    const chain = evmCaip2(1);
    const { nonce } = issueNonce(address, chain);
    expect(consumeNonce(nonce, address, "eip155:137")).toBe(false);
    const { nonce: n2 } = issueNonce(address, chain);
    expect(consumeNonce(n2, address, chain)).toBe(true);
    expect(consumeNonce(n2, address, chain)).toBe(false);
  });

  it("supports configured EVM chain ids", () => {
    expect(SUPPORTED_EVM_NUMERIC_IDS.has(1)).toBe(true);
    expect(SUPPORTED_EVM_NUMERIC_IDS.has(8453)).toBe(true);
    expect(SUPPORTED_EVM_NUMERIC_IDS.has(999999)).toBe(false);
  });
});

describe("SIWE message crypto", () => {
  it("verifies a valid SIWE signature", async () => {
    const account = privateKeyToAccount(generatePrivateKey());
    const nonce = "abcd1234abcd1234";
    const message = new SiweMessage({
      domain: "localhost",
      address: account.address,
      statement: "Sign in to Krydo with your Ethereum wallet.",
      uri: "http://localhost:5000",
      version: "1",
      chainId: 1,
      nonce,
      issuedAt: new Date().toISOString(),
    });
    const prepared = message.prepareMessage();
    const signature = await account.signMessage({ message: prepared });
    const ok = await new SiweMessage(prepared).verify({
      signature,
      domain: "localhost",
      nonce,
    });
    expect(ok.success).toBe(true);
  });

  it("rejects wrong domain on verify", async () => {
    const account = privateKeyToAccount(generatePrivateKey());
    const nonce = "efgh5678efgh5678";
    const message = new SiweMessage({
      domain: "localhost",
      address: account.address,
      statement: "Sign in to Krydo with your Ethereum wallet.",
      uri: "http://localhost:5000",
      version: "1",
      chainId: 1,
      nonce,
      issuedAt: new Date().toISOString(),
    });
    const prepared = message.prepareMessage();
    const signature = await account.signMessage({ message: prepared });
    try {
      const bad = await new SiweMessage(prepared).verify({
        signature,
        domain: "evil.example",
        nonce,
      });
      expect(bad.success).toBe(false);
    } catch (err: unknown) {
      // siwe may throw SiweError / response object on domain mismatch
      const msg = err instanceof Error ? err.message : JSON.stringify(err);
      expect(msg.toLowerCase()).toMatch(/domain/);
    }
  });
});
