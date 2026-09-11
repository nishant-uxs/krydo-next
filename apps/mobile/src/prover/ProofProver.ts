/**
 * ProofProver boundary.
 *
 * Future:
 *   ProofProver
 *     ├── MockProofProver   (this phase — DEMO ONLY)
 *     └── KrydoMobileZkProver (later — real device/native proving)
 *
 * Do NOT claim mock proofs are cryptographically valid.
 */

export interface ProofInput {
  requestId: string;
  credentialId: string;
  claimType: string;
  policyKind: string;
  challenge: string;
  audience: string;
}

export interface ProofResult {
  /** Always true for MockProofProver — marks non-real crypto. */
  isMock: boolean;
  label: "DEMO / MOCK PROOF";
  proofId: string | null;
  commitment: string | null;
  createdAt: string;
  note: string;
}

export interface ProofProver {
  prove(input: ProofInput): Promise<ProofResult>;
}

export class MockProofProver implements ProofProver {
  async prove(input: ProofInput): Promise<ProofResult> {
    // Simulate brief work without inventing crypto validity.
    await new Promise((r) => setTimeout(r, 250));
    return {
      isMock: true,
      label: "DEMO / MOCK PROOF",
      proofId: null,
      commitment: null,
      createdAt: new Date().toISOString(),
      note: `Mock proof for request ${input.requestId} / credential ${input.credentialId}. Not cryptographically valid.`,
    };
  }
}

let prover: ProofProver | null = null;

export function getProofProver(): ProofProver {
  if (!prover) prover = new MockProofProver();
  return prover;
}

export function __setProofProverForTests(p: ProofProver | null): void {
  prover = p;
}
