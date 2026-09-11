/**
 * Public / verifier-facing credential views.
 *
 * Trust boundary: plaintext `claimData` and human-readable `claimSummary`
 * must never leave the server on unauthenticated verification paths.
 * Holders and issuers still receive full documents on authenticated,
 * authorized routes.
 */

export interface CredentialLike {
  id: string;
  credentialHash: string;
  issuerAddress: string;
  holderAddress: string;
  claimType: string;
  claimSummary?: string;
  claimData?: unknown;
  status: string;
  issuedAt: Date | string;
  expiresAt: Date | string | null;
  revokedAt?: Date | string | null;
  onChainTxHash?: string | null;
}

/** Minimal credential fields safe for a public verifier. */
export interface PublicCredentialView {
  id: string;
  credentialHash: string;
  issuerAddress: string;
  holderAddress: string;
  claimType: string;
  status: string;
  issuedAt: Date | string;
  expiresAt: Date | string | null;
  revokedAt: Date | string | null;
}

/**
 * Strip plaintext claims and other sensitive metadata from a credential
 * document before returning it on a public verification endpoint.
 */
export function toPublicCredentialView(credential: CredentialLike): PublicCredentialView {
  return {
    id: credential.id,
    credentialHash: credential.credentialHash,
    issuerAddress: credential.issuerAddress,
    holderAddress: credential.holderAddress,
    claimType: credential.claimType,
    status: credential.status,
    issuedAt: credential.issuedAt,
    expiresAt: credential.expiresAt ?? null,
    revokedAt: credential.revokedAt ?? null,
  };
}

/** Runtime guard used in regression tests and defensive response checks. */
export function assertNoClaimLeak(payload: unknown): void {
  if (payload === null || typeof payload !== "object") return;
  const walk = (value: unknown, path: string): void => {
    if (value === null || typeof value !== "object") return;
    if (Array.isArray(value)) {
      value.forEach((item, i) => walk(item, `${path}[${i}]`));
      return;
    }
    for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
      const next = path ? `${path}.${key}` : key;
      if (key === "claimData" || key === "claimSummary") {
        throw new Error(`Sensitive field leaked at ${next}`);
      }
      walk(child, next);
    }
  };
  walk(payload, "");
}
