import { describe, it, expect } from "vitest";
import {
  evmCaip2,
  stellarCaip2,
  toCaip10,
  parseCaip10,
  chainTypeFromCaip2,
  normalizeEvmAddress,
  isEvmAddress,
  SUPPORTED_EVM_NUMERIC_IDS,
} from "./wallet";

describe("shared/wallet", () => {
  it("builds CAIP-2 / CAIP-10", () => {
    expect(evmCaip2(1)).toBe("eip155:1");
    expect(stellarCaip2("testnet")).toBe("stellar:testnet");
    expect(toCaip10("eip155:1", "0xabc")).toBe("eip155:1:0xabc");
    expect(parseCaip10("eip155:8453:0xdead")?.chainId).toBe("eip155:8453");
    expect(chainTypeFromCaip2("eip155:1")).toBe("EVM");
    expect(chainTypeFromCaip2("stellar:testnet")).toBe("STELLAR");
  });

  it("normalizes EVM addresses", () => {
    expect(isEvmAddress("0x1111111111111111111111111111111111111111")).toBe(true);
    expect(isEvmAddress("GDUKMGUGDZQK")).toBe(false);
    expect(normalizeEvmAddress("0xABCD")).toBe("0xabcd");
  });

  it("lists supported EVM ids", () => {
    for (const id of [1, 137, 8453, 42161, 10, 56, 43114]) {
      expect(SUPPORTED_EVM_NUMERIC_IDS.has(id)).toBe(true);
    }
  });
});
