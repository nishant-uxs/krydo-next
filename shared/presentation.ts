/**
 * Krydo Verifiable Presentation (VP) + Presentation Request types.
 *
 * Separation of concerns:
 *   Credential (VC)  — long-lived, issuer-signed / on-chain anchored
 *   Presentation (VP)— short-lived, holder-created for one verifier request
 *
 * A VP must NOT simply be another copy of the VC. It binds:
 *   holder + challenge + audience + (optional ZK proof) + credential reference
 *
 * Trust boundary honesty:
 *   Current ZK proving still runs on the Krydo backend when generating proofs.
 *   This module defines the presentation protocol the future mobile wallet
 *   will use; it does not claim device-only proving.
 *
 * Spec alignment: compatible with W3C VC Data Model v2 presentation concepts
 * while remaining Krydo-native for challenge/audience/replay.
 */

import { z } from "zod";
import { claimTypes, stellarAddressSchema } from "./schema";
import { STELLAR_NETWORK } from "./contracts";
import {
  presentationPolicySchema,
  type PresentationPolicy,
  policyRequiresZk,
} from "./presentation-policy";

export const PRESENTATION_REQUEST_VERSION = "krydo-vp-request-v1" as const;
export const PRESENTATION_VERSION = "krydo-vp-v1" as const;

const W3C_VC_V2 = "https://www.w3.org/ns/credentials/v2";
const KRYDO_CONTEXT = "https://krydo.dev/credentials/v1";

function caip2ChainRef(network: string): string {
  if (network === "mainnet" || network === "public" || network === "pubnet") {
    return "pubnet";
  }
  if (network === "futurenet") return "futurenet";
  return "testnet";
}

const CHAIN_REF = caip2ChainRef(STELLAR_NETWORK);

/** did:pkh for a Stellar StrKey address (case-sensitive). */
export function stellarDidFromAddress(addr: string): string {
  return `did:pkh:stellar:${CHAIN_REF}:${addr}`;
}

/** Extract StrKey from a did:pkh:stellar:… DID, or return null. */
export function addressFromStellarDid(did: string): string | null {
  const prefix = `did:pkh:stellar:${CHAIN_REF}:`;
  if (!did.startsWith(prefix)) return null;
  const addr = did.slice(prefix.length);
  return /^G[A-Z2-7]{55}$/.test(addr) ? addr : null;
}

/** Deep-link / QR-compatible URI for a presentation request. */
export function presentationRequestDeepLink(requestId: string): string {
  return `krydo://present?request=${encodeURIComponent(requestId)}`;
}

/** HTTPS-equivalent scan URL (SPA can route later). */
export function presentationRequestHttpsLink(
  origin: string,
  requestId: string,
): string {
  const base = origin.replace(/\/$/, "");
  return `${base}/present?request=${encodeURIComponent(requestId)}`;
}

// ---------- Presentation Request ----------

export const createPresentationRequestBodySchema = z
  .object({
    /**
     * Audience / domain the VP must bind to. Defaults to verifier address
     * when omitted. Client-supplied challenge / verifier / expiresAt /
     * id are rejected via `.strict()`.
     */
    audience: z.string().trim().min(1).max(256).optional(),
    reason: z.string().trim().max(500).optional(),
    /** TTL in seconds (default 10 min, max 24h). */
    ttlSeconds: z.number().int().min(60).max(86_400).optional().default(600),
    requestedCredentials: z
      .array(
        z.object({
          claimType: z.enum(claimTypes),
        }),
      )
      .min(1)
      .max(8),
    policy: presentationPolicySchema,
  })
  .strict();

export type CreatePresentationRequestBody = z.infer<
  typeof createPresentationRequestBodySchema
>;

export interface PresentationRequest {
  id: string;
  version: typeof PRESENTATION_REQUEST_VERSION;
  verifier: string;
  audience: string;
  challenge: string;
  reason: string | null;
  requestedCredentials: Array<{ claimType: (typeof claimTypes)[number] }>;
  policy: PresentationPolicy;
  createdAt: string;
  expiresAt: string;
  /** Mobile-wallet-ready deep link. */
  deepLink: string;
}

/** Public retrieval view — no secrets beyond the challenge (needed by holder). */
export type PublicPresentationRequest = PresentationRequest;

// ---------- Verifiable Presentation ----------

export const presentationCredentialRefSchema = z.object({
  id: z.string().min(1).max(128),
  credentialHash: z
    .string()
    .regex(/^(0x)?[a-fA-F0-9]{64}$/),
  claimType: z.enum(claimTypes),
  issuerAddress: stellarAddressSchema,
  holderAddress: stellarAddressSchema,
});

export type PresentationCredentialRef = z.infer<
  typeof presentationCredentialRefSchema
>;

export const krydoZkPresentationProofSchema = z.object({
  type: z.literal("KrydoZkProof2025"),
  proofPurpose: z.literal("authentication"),
  created: z.string().datetime(),
  challenge: z.string().min(16).max(128),
  domain: z.string().min(1).max(256),
  proofId: z.string().uuid(),
  proofType: z.string().min(1).max(32),
  commitment: z.string().min(4).max(256).optional(),
});

export const krydoStatusPresentationProofSchema = z.object({
  type: z.literal("KrydoCredentialReference2025"),
  proofPurpose: z.literal("authentication"),
  created: z.string().datetime(),
  challenge: z.string().min(16).max(128),
  domain: z.string().min(1).max(256),
  credentialHash: z
    .string()
    .regex(/^(0x)?[a-fA-F0-9]{64}$/),
});

export const presentationProofSchema = z.union([
  krydoZkPresentationProofSchema,
  krydoStatusPresentationProofSchema,
]);

export type PresentationProof = z.infer<typeof presentationProofSchema>;

export const verifiablePresentationSchema = z.object({
  "@context": z.array(z.string()).min(1),
  type: z.array(z.string()).refine(
    (t) => t.includes("VerifiablePresentation"),
    "type must include VerifiablePresentation",
  ),
  id: z.string().min(1).max(128),
  holder: z.string().min(1).max(256),
  requestId: z.string().uuid(),
  challenge: z.string().min(16).max(128),
  domain: z.string().min(1).max(256),
  created: z.string().datetime(),
  expiresAt: z.string().datetime(),
  verifiableCredential: presentationCredentialRefSchema,
  proof: presentationProofSchema,
});

export type VerifiablePresentation = z.infer<typeof verifiablePresentationSchema>;

// ---------- Builder ----------

export interface CreatePresentationInput {
  request: PresentationRequest;
  holderAddress: string;
  credential: {
    id: string;
    credentialHash: string;
    claimType: string;
    issuerAddress: string;
    holderAddress: string;
  };
  /** Required when policyRequiresZk(request.policy). */
  zkProof?: {
    id: string;
    proofType: string;
    commitment: string;
  };
  /** Override VP expiry (must be ≤ request.expiresAt). */
  expiresAt?: Date;
  now?: Date;
}

/**
 * Build a typed Verifiable Presentation for a presentation request.
 *
 * Does NOT sign with the holder's private key — the future mobile wallet
 * owns that. This produces the protocol object + Krydo-native proof payload
 * that `/api/presentations/verify` understands.
 */
export function createPresentation(
  input: CreatePresentationInput,
): VerifiablePresentation {
  const now = input.now ?? new Date();
  const { request, holderAddress, credential } = input;

  if (credential.holderAddress !== holderAddress) {
    throw new Error("Credential holder does not match presentation holder");
  }
  if (credential.claimType !== request.policy.claimType) {
    throw new Error("Credential claimType does not match request policy");
  }
  if (
    !request.requestedCredentials.some((r) => r.claimType === credential.claimType)
  ) {
    throw new Error("Credential claimType not in requestedCredentials");
  }

  const requiresZk = policyRequiresZk(request.policy);
  if (requiresZk && !input.zkProof) {
    throw new Error("Policy requires a ZK proof reference");
  }
  if (!requiresZk && input.zkProof) {
    // Allow attaching ZK even for status-only requests, but prefer status proof.
  }

  const requestExpiry = new Date(request.expiresAt);
  if (requestExpiry.getTime() <= now.getTime()) {
    throw new Error("Presentation request has expired");
  }

  let vpExpiry = input.expiresAt ?? requestExpiry;
  if (vpExpiry.getTime() > requestExpiry.getTime()) {
    vpExpiry = requestExpiry;
  }

  const holderDid = stellarDidFromAddress(holderAddress);
  const created = now.toISOString();
  const id = `urn:uuid:${cryptoRandomUuid()}`;

  const credentialRef: PresentationCredentialRef = {
    id: `urn:uuid:${credential.id}`,
    credentialHash: credential.credentialHash.replace(/^0x/i, "").toLowerCase(),
    claimType: credential.claimType as (typeof claimTypes)[number],
    issuerAddress: credential.issuerAddress,
    holderAddress: credential.holderAddress,
  };

  let proof: PresentationProof;
  if (requiresZk && input.zkProof) {
    proof = {
      type: "KrydoZkProof2025",
      proofPurpose: "authentication",
      created,
      challenge: request.challenge,
      domain: request.audience,
      proofId: input.zkProof.id,
      proofType: input.zkProof.proofType,
      commitment: input.zkProof.commitment,
    };
  } else {
    proof = {
      type: "KrydoCredentialReference2025",
      proofPurpose: "authentication",
      created,
      challenge: request.challenge,
      domain: request.audience,
      credentialHash: credentialRef.credentialHash,
    };
  }

  return {
    "@context": [W3C_VC_V2, KRYDO_CONTEXT],
    type: ["VerifiablePresentation", "KrydoPresentation"],
    id,
    holder: holderDid,
    requestId: request.id,
    challenge: request.challenge,
    domain: request.audience,
    created,
    expiresAt: vpExpiry.toISOString(),
    verifiableCredential: credentialRef,
    proof,
  };
}

/** Prefer Web Crypto UUID — never use Math.random for identifiers. */
function cryptoRandomUuid(): string {
  if (typeof globalThis.crypto?.randomUUID === "function") {
    return globalThis.crypto.randomUUID();
  }
  throw new Error("Secure crypto.randomUUID() is required to create presentations");
}

export const PRESENTATION_CONSTANTS = {
  W3C_VC_V2,
  KRYDO_CONTEXT,
  PRESENTATION_REQUEST_VERSION,
  PRESENTATION_VERSION,
} as const;
