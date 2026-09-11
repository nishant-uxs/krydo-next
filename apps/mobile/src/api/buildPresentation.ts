/**
 * Builds a presentation payload for the holder flow.
 *
 * Prefer shared `createPresentation` when a live PresentationRequest +
 * credential are available. Falls back to an explicit DEMO marker object
 * when offline / mock mode cannot satisfy shared validators.
 */

import {
  createPresentation,
  type PresentationRequest,
  type VerifiablePresentation,
} from "../../../../shared/presentation";
import type { StoredCredential } from "../data/demo";
import type { PresentationRequestDto } from "../api/VerificationClient";
import type { ProofResult } from "../prover/ProofProver";

function dtoToRequest(dto: PresentationRequestDto): PresentationRequest {
  return {
    id: dto.id,
    version: "krydo-vp-request-v1",
    verifier: dto.verifier,
    audience: dto.audience,
    challenge: dto.challenge,
    reason: dto.reason,
    requestedCredentials: dto.requestedCredentials.map((r) => ({
      claimType: r.claimType as PresentationRequest["requestedCredentials"][number]["claimType"],
    })),
    policy: dto.policy as PresentationRequest["policy"],
    createdAt: dto.createdAt,
    expiresAt: dto.expiresAt,
    deepLink: dto.deepLink,
  };
}

export interface BuildPresentationInput {
  request: PresentationRequestDto;
  credential: StoredCredential;
  proof: ProofResult;
}

export type BuiltPresentation =
  | { kind: "shared"; presentation: VerifiablePresentation; isMock: false }
  | {
      kind: "demo";
      presentation: Record<string, unknown>;
      isMock: true;
      label: "DEMO / MOCK PROOF";
    };

export function buildHolderPresentation(
  input: BuildPresentationInput,
): BuiltPresentation {
  const { request, credential, proof } = input;

  // Mock proofs never claim cryptographic validity — return a marked demo VP.
  if (proof.isMock) {
    return {
      kind: "demo",
      isMock: true,
      label: "DEMO / MOCK PROOF",
      presentation: {
        "@context": [
          "https://www.w3.org/ns/credentials/v2",
          "https://krydo.dev/credentials/v1",
        ],
        type: ["VerifiablePresentation", "KrydoPresentation"],
        id: `urn:uuid:demo-${credential.id}`,
        holder: `did:pkh:stellar:testnet:${credential.holderAddress}`,
        requestId: request.id,
        challenge: request.challenge,
        domain: request.audience,
        created: proof.createdAt,
        expiresAt: request.expiresAt,
        verifiableCredential: {
          id: `urn:uuid:${credential.id}`,
          credentialHash: credential.credentialHash,
          claimType: credential.claimType,
          issuerAddress: credential.issuerAddress,
          holderAddress: credential.holderAddress,
        },
        proof: {
          type: "KrydoDemoMockProof2026",
          proofPurpose: "authentication",
          created: proof.createdAt,
          challenge: request.challenge,
          domain: request.audience,
          label: "DEMO / MOCK PROOF",
          note: proof.note,
        },
      },
    };
  }

  const presentation = createPresentation({
    request: dtoToRequest(request),
    holderAddress: credential.holderAddress,
    credential: {
      id: credential.id,
      credentialHash: credential.credentialHash,
      claimType: credential.claimType,
      issuerAddress: credential.issuerAddress,
      holderAddress: credential.holderAddress,
    },
  });

  return { kind: "shared", presentation, isMock: false };
}
