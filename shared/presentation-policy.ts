/**
 * Presentation policy abstraction for Krydo Verifiable Presentations.
 *
 * This is intentionally NOT a string expression evaluator.
 * Policies map onto the existing ZK proof kinds (or credential-status-only).
 * Compound AND/OR evaluation is reserved for a later phase.
 */

import { z } from "zod";
import { claimTypes, proofTypes } from "./schema";

export const PRESENTATION_POLICY_VERSION = "krydo-policy-v1" as const;

/**
 * Typed policy requirements a verifier may attach to a Presentation Request.
 * Each kind is validated structurally; no arbitrary code/expressions.
 */
export const presentationPolicySchema = z.discriminatedUnion("kind", [
  z.object({
    kind: z.literal("credential_status"),
    claimType: z.enum(claimTypes),
  }),
  z.object({
    kind: z.literal("range_above"),
    claimType: z.enum(claimTypes),
    threshold: z.number().finite(),
  }),
  z.object({
    kind: z.literal("range_below"),
    claimType: z.enum(claimTypes),
    threshold: z.number().finite(),
  }),
  z.object({
    kind: z.literal("equality"),
    claimType: z.enum(claimTypes),
    targetValue: z.string().min(1).max(1024),
  }),
  z.object({
    kind: z.literal("membership"),
    claimType: z.enum(claimTypes),
    memberSet: z.array(z.string().max(256)).min(1).max(1024),
  }),
  z.object({
    kind: z.literal("non_zero"),
    claimType: z.enum(claimTypes),
  }),
  z.object({
    kind: z.literal("selective_disclosure"),
    claimType: z.enum(claimTypes),
    fields: z.array(z.string().max(64)).min(1).max(64),
  }),
]);

export type PresentationPolicy = z.infer<typeof presentationPolicySchema>;

/** Whether this policy requires an accompanying Krydo ZK proof. */
export function policyRequiresZk(policy: PresentationPolicy): boolean {
  return policy.kind !== "credential_status";
}

/**
 * Map a policy kind onto the existing ZK proof type enum when applicable.
 * Returns null for credential_status-only presentations.
 */
export function policyToProofType(
  policy: PresentationPolicy,
): (typeof proofTypes)[number] | null {
  if (policy.kind === "credential_status") return null;
  return policy.kind;
}

/**
 * Future compound policies will nest under `allOf` / `anyOf`.
 * Reserved shape only — not evaluated in P1.
 */
export interface CompoundPolicySkeleton {
  version: typeof PRESENTATION_POLICY_VERSION;
  /** Example future shape: age >= 18 AND country == IN */
  allOf?: PresentationPolicy[];
  anyOf?: PresentationPolicy[];
}
