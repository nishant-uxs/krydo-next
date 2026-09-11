/**
 * Presentation verification — challenge, audience, expiry, credential status,
 * issuer trust, and optional existing ZK proof verification.
 *
 * Privacy: never returns claimData / claimSummary.
 */

import type { VerifiablePresentation } from "@shared/presentation";
import {
  addressFromStellarDid,
  verifiablePresentationSchema,
} from "@shared/presentation";
import { policyRequiresZk, policyToProofType } from "@shared/presentation-policy";
import { storage } from "../storage";
import {
  consumePresentationChallenge,
  getPresentationRequest,
  isChallengeOpen,
  type StoredPresentationRequest,
} from "./request-store";
import { childLogger } from "../logger";

const log = childLogger("presentations/verify");

export interface PresentationVerificationChecks {
  structure: boolean;
  holderBinding: boolean;
  challenge: boolean;
  audience: boolean;
  expiration: boolean;
  credentialStatus: boolean;
  issuerTrusted: boolean;
  proof: boolean;
  replay: boolean;
}

export interface PresentationVerificationResult {
  valid: boolean;
  message: string;
  checks: PresentationVerificationChecks;
  requestId: string | null;
  credential: {
    credentialHash: string;
    claimType: string;
    status: string;
    issuerAddress: string;
    holderAddress: string;
    expiresAt: string | null;
  } | null;
  issuerName: string | null;
  proof: {
    type: string;
    proofId?: string;
    cryptographicallyValid?: boolean;
    reason?: string;
  } | null;
}

function fail(
  partial: Partial<PresentationVerificationResult> & {
    message: string;
    checks: PresentationVerificationChecks;
  },
): PresentationVerificationResult {
  return {
    valid: false,
    requestId: null,
    credential: null,
    issuerName: null,
    proof: null,
    ...partial,
  };
}

function emptyChecks(over: Partial<PresentationVerificationChecks> = {}): PresentationVerificationChecks {
  return {
    structure: false,
    holderBinding: false,
    challenge: false,
    audience: false,
    expiration: false,
    credentialStatus: false,
    issuerTrusted: false,
    proof: false,
    replay: false,
    ...over,
  };
}

/**
 * Verify a Krydo Verifiable Presentation against its Presentation Request.
 *
 * Challenge is consumed only after all other checks pass, so a failed
 * status/issuer/proof check does not burn the challenge — but a successful
 * verify permanently consumes it (replay protection).
 */
export async function verifyPresentation(
  rawVp: unknown,
  opts: { now?: Date } = {},
): Promise<PresentationVerificationResult> {
  const now = opts.now ?? new Date();

  const parsed = verifiablePresentationSchema.safeParse(rawVp);
  if (!parsed.success) {
    return fail({
      message: `Malformed presentation: ${parsed.error.issues[0]?.message ?? "invalid"}`,
      checks: emptyChecks(),
    });
  }
  const vp: VerifiablePresentation = parsed.data;
  const checks = emptyChecks({ structure: true });

  const request = getPresentationRequest(vp.requestId);
  if (!request) {
    return fail({
      message: "Presentation request not found or expired",
      requestId: vp.requestId,
      checks,
    });
  }

  // Expiration (request + VP)
  if (new Date(request.expiresAt).getTime() <= now.getTime()) {
    return fail({
      message: "Presentation request has expired",
      requestId: request.id,
      checks: { ...checks, expiration: false },
    });
  }
  if (new Date(vp.expiresAt).getTime() <= now.getTime()) {
    return fail({
      message: "Presentation has expired",
      requestId: request.id,
      checks: { ...checks, expiration: false },
    });
  }
  checks.expiration = true;

  // Audience / domain binding
  if (vp.domain !== request.audience || vp.proof.domain !== request.audience) {
    return fail({
      message: "Audience / domain mismatch",
      requestId: request.id,
      checks: { ...checks, audience: false },
    });
  }
  checks.audience = true;

  // Challenge binding (peek — consume after success)
  if (
    vp.challenge !== request.challenge ||
    vp.proof.challenge !== request.challenge
  ) {
    return fail({
      message: "Challenge mismatch",
      requestId: request.id,
      checks: { ...checks, challenge: false },
    });
  }
  const open = isChallengeOpen(request, vp.challenge, now);
  if (!open.ok) {
    return fail({
      message: open.reason,
      requestId: request.id,
      checks: {
        ...checks,
        challenge: false,
        replay: open.reason.includes("replay") || open.reason.includes("consumed"),
      },
    });
  }
  checks.challenge = true;

  // Holder binding
  const holderFromDid = addressFromStellarDid(vp.holder);
  if (!holderFromDid) {
    return fail({
      message: "Invalid holder DID",
      requestId: request.id,
      checks: { ...checks, holderBinding: false },
    });
  }
  if (holderFromDid !== vp.verifiableCredential.holderAddress) {
    return fail({
      message: "Holder DID does not match credential subject",
      requestId: request.id,
      checks: { ...checks, holderBinding: false },
    });
  }

  const credId = vp.verifiableCredential.id.replace(/^urn:uuid:/, "");
  const credential =
    (await storage.getCredentialById(credId)) ??
    (await storage.getCredentialByHash(vp.verifiableCredential.credentialHash));

  if (!credential) {
    return fail({
      message: "Referenced credential not found",
      requestId: request.id,
      checks: { ...checks, holderBinding: false, credentialStatus: false },
    });
  }

  if (credential.holderAddress !== holderFromDid) {
    return fail({
      message: "Credential holder substitution rejected",
      requestId: request.id,
      checks: { ...checks, holderBinding: false },
    });
  }
  if (
    credential.credentialHash.replace(/^0x/i, "").toLowerCase() !==
    vp.verifiableCredential.credentialHash.replace(/^0x/i, "").toLowerCase()
  ) {
    return fail({
      message: "Credential hash mismatch",
      requestId: request.id,
      checks: { ...checks, holderBinding: false },
    });
  }
  if (credential.claimType !== request.policy.claimType) {
    return fail({
      message: "Credential claimType does not satisfy request policy",
      requestId: request.id,
      checks: { ...checks, holderBinding: false },
    });
  }
  checks.holderBinding = true;

  // Credential status
  const credExpired =
    !!credential.expiresAt && new Date(credential.expiresAt).getTime() <= now.getTime();
  if (credential.status !== "active" || credExpired) {
    return fail({
      message: credExpired
        ? "Underlying credential has expired"
        : `Underlying credential is ${credential.status}`,
      requestId: request.id,
      checks: { ...checks, credentialStatus: false },
      credential: publicCred(credential),
    });
  }
  checks.credentialStatus = true;

  // Issuer trust
  const issuer = await storage.getIssuerByAddress(credential.issuerAddress);
  const issuerTrusted = !!issuer?.active;
  if (!issuerTrusted) {
    return fail({
      message: "Issuer is not trusted / not active",
      requestId: request.id,
      checks: { ...checks, issuerTrusted: false },
      credential: publicCred(credential),
      issuerName: issuer?.name ?? null,
    });
  }
  checks.issuerTrusted = true;

  // Proof verification
  const proofResult = await verifyPresentationProof(vp, request, credential.id, now);
  if (!proofResult.valid) {
    return fail({
      message: proofResult.reason,
      requestId: request.id,
      checks: { ...checks, proof: false },
      credential: publicCred(credential),
      issuerName: issuer?.name ?? null,
      proof: proofResult.meta,
    });
  }
  checks.proof = true;

  // Consume challenge — replay protection
  const consumed = consumePresentationChallenge(request.id, vp.challenge, now);
  if (!consumed.ok) {
    return fail({
      message: consumed.reason,
      requestId: request.id,
      checks: { ...checks, challenge: false, replay: true },
      credential: publicCred(credential),
      issuerName: issuer?.name ?? null,
      proof: proofResult.meta,
    });
  }

  log.info(
    { requestId: request.id, claimType: credential.claimType },
    "presentation verified",
  );

  return {
    valid: true,
    message: "Presentation verified",
    checks: { ...checks, replay: false },
    requestId: request.id,
    credential: publicCred(credential),
    issuerName: issuer?.name ?? null,
    proof: proofResult.meta,
  };
}

function publicCred(credential: {
  credentialHash: string;
  claimType: string;
  status: string;
  issuerAddress: string;
  holderAddress: string;
  expiresAt: Date | null;
}) {
  return {
    credentialHash: credential.credentialHash,
    claimType: credential.claimType,
    status: credential.status,
    issuerAddress: credential.issuerAddress,
    holderAddress: credential.holderAddress,
    expiresAt: credential.expiresAt
      ? new Date(credential.expiresAt).toISOString()
      : null,
  };
}

async function verifyPresentationProof(
  vp: VerifiablePresentation,
  request: StoredPresentationRequest,
  credentialId: string,
  now: Date,
): Promise<{
  valid: boolean;
  reason: string;
  meta: PresentationVerificationResult["proof"];
}> {
  const requiresZk = policyRequiresZk(request.policy);

  if (vp.proof.type === "KrydoCredentialReference2025") {
    if (requiresZk) {
      return {
        valid: false,
        reason: "Policy requires a ZK proof presentation",
        meta: { type: vp.proof.type },
      };
    }
    if (
      vp.proof.credentialHash.replace(/^0x/i, "").toLowerCase() !==
      vp.verifiableCredential.credentialHash.replace(/^0x/i, "").toLowerCase()
    ) {
      return {
        valid: false,
        reason: "Status proof credentialHash mismatch",
        meta: { type: vp.proof.type },
      };
    }
    return {
      valid: true,
      reason: "credential reference proof accepted",
      meta: { type: vp.proof.type },
    };
  }

  // KrydoZkProof2025 — reuse existing engine
  if (!requiresZk) {
    // Status-only policy with ZK attached is still acceptable.
  } else {
    const expected = policyToProofType(request.policy);
    if (expected && vp.proof.proofType !== expected) {
      return {
        valid: false,
        reason: `Proof type ${vp.proof.proofType} does not match policy ${expected}`,
        meta: { type: vp.proof.type, proofId: vp.proof.proofId },
      };
    }
  }

  const proof = await storage.getZkProof(vp.proof.proofId);
  if (!proof) {
    return {
      valid: false,
      reason: "ZK proof not found",
      meta: { type: vp.proof.type, proofId: vp.proof.proofId },
    };
  }
  if (proof.credentialId !== credentialId) {
    return {
      valid: false,
      reason: "ZK proof is not bound to the presented credential",
      meta: { type: vp.proof.type, proofId: vp.proof.proofId },
    };
  }
  if (proof.proverAddress !== vp.verifiableCredential.holderAddress) {
    return {
      valid: false,
      reason: "ZK proof prover does not match presentation holder",
      meta: { type: vp.proof.type, proofId: vp.proof.proofId },
    };
  }
  if (proof.expiresAt && proof.expiresAt.getTime() <= now.getTime()) {
    return {
      valid: false,
      reason: "ZK proof has expired",
      meta: { type: vp.proof.type, proofId: vp.proof.proofId },
    };
  }

  // Policy parameter binding (threshold / target / membership)
  const publicInputs = proof.publicInputs as {
    threshold?: number;
    targetValue?: string;
    memberSet?: string[];
  };
  if (request.policy.kind === "range_above" || request.policy.kind === "range_below") {
    if (publicInputs.threshold !== request.policy.threshold) {
      return {
        valid: false,
        reason: "ZK proof threshold does not match request policy",
        meta: { type: vp.proof.type, proofId: vp.proof.proofId },
      };
    }
  }
  if (request.policy.kind === "equality") {
    if (publicInputs.targetValue !== request.policy.targetValue) {
      return {
        valid: false,
        reason: "ZK proof targetValue does not match request policy",
        meta: { type: vp.proof.type, proofId: vp.proof.proofId },
      };
    }
  }

  const { verifyZkProof } = await import("../zk-engine");
  const cryptoResult = verifyZkProof(
    proof.proofData as Parameters<typeof verifyZkProof>[0],
    proof.publicInputs as Parameters<typeof verifyZkProof>[1],
  );

  if (!cryptoResult.valid) {
    return {
      valid: false,
      reason: cryptoResult.reason || "ZK proof cryptographically invalid",
      meta: {
        type: vp.proof.type,
        proofId: vp.proof.proofId,
        cryptographicallyValid: false,
        reason: cryptoResult.reason,
      },
    };
  }

  return {
    valid: true,
    reason: cryptoResult.reason || "ZK proof valid",
    meta: {
      type: vp.proof.type,
      proofId: vp.proof.proofId,
      cryptographicallyValid: true,
      reason: cryptoResult.reason,
    },
  };
}
