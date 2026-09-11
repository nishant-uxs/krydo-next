/**
 * Shared multi-chain wallet model for Krydo.
 * Stellar remains first-class; EVM accounts are auth/identity layer (not Soroban subjects).
 */

export type ChainType = "STELLAR" | "EVM";

/** CAIP-2 style chain id, e.g. "stellar:pubnet", "eip155:1" */
export type Caip2ChainId = string;

/** CAIP-10 account id, e.g. "eip155:1:0xabc…", "stellar:testnet:G…" */
export type Caip10AccountId = string;

export interface WalletAccount {
  chainType: ChainType;
  /** CAIP-2 */
  chainId: Caip2ChainId;
  address: string;
  walletProvider?: string;
  capabilities?: string[];
  label?: string | null;
}

export const STELLAR_CHAIN_TESTNET = "stellar:testnet";
export const STELLAR_CHAIN_PUBNET = "stellar:pubnet";

export const EVM_CHAINS = {
  ethereum: { chainId: "eip155:1", name: "Ethereum", numericId: 1 },
  polygon: { chainId: "eip155:137", name: "Polygon", numericId: 137 },
  base: { chainId: "eip155:8453", name: "Base", numericId: 8453 },
  arbitrum: { chainId: "eip155:42161", name: "Arbitrum", numericId: 42161 },
  optimism: { chainId: "eip155:10", name: "Optimism", numericId: 10 },
  bnb: { chainId: "eip155:56", name: "BNB Chain", numericId: 56 },
  avalanche: { chainId: "eip155:43114", name: "Avalanche", numericId: 43114 },
} as const;

export type SupportedEvmKey = keyof typeof EVM_CHAINS;

export const SUPPORTED_EVM_NUMERIC_IDS: ReadonlySet<number> = new Set(
  Object.values(EVM_CHAINS).map((c) => c.numericId),
);

export function stellarCaip2(network: "testnet" | "mainnet" | "pubnet" | "futurenet"): Caip2ChainId {
  if (network === "mainnet" || network === "pubnet") return STELLAR_CHAIN_PUBNET;
  return STELLAR_CHAIN_TESTNET;
}

export function evmCaip2(numericChainId: number): Caip2ChainId {
  return `eip155:${numericChainId}`;
}

export function toCaip10(chainId: Caip2ChainId, address: string): Caip10AccountId {
  return `${chainId}:${address}`;
}

export function parseCaip10(accountId: Caip10AccountId): {
  chainId: Caip2ChainId;
  address: string;
} | null {
  const parts = accountId.split(":");
  if (parts.length < 3) return null;
  if (parts[0] === "eip155") {
    return { chainId: `${parts[0]}:${parts[1]}`, address: parts.slice(2).join(":") };
  }
  if (parts[0] === "stellar") {
    return { chainId: `${parts[0]}:${parts[1]}`, address: parts.slice(2).join(":") };
  }
  return null;
}

export function isEvmAddress(value: string): boolean {
  return /^0x[a-fA-F0-9]{40}$/.test(value.trim());
}

export function normalizeEvmAddress(value: string): string {
  return value.trim().toLowerCase();
}

export function chainTypeFromCaip2(chainId: Caip2ChainId): ChainType {
  if (chainId.startsWith("eip155:")) return "EVM";
  return "STELLAR";
}
