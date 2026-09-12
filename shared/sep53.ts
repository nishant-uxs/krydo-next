/**
 * SEP-53 — Sign and Verify Messages
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0053.md
 *
 * Freighter's `signMessage` follows SEP-53: it signs SHA-256("Stellar Signed Message:\n" + msg),
 * not the raw UTF-8 bytes. Server-side SIWS verification MUST use the same payload.
 */
import { sha256 } from "@noble/hashes/sha2.js";
import { Keypair } from "@stellar/stellar-sdk";

export const SEP53_PREFIX = "Stellar Signed Message:\n";

/** Canonical SEP-53 payload bytes for a UTF-8 string message. */
export function sep53Payload(message: string): Buffer {
  return Buffer.concat([
    Buffer.from(SEP53_PREFIX, "utf8"),
    Buffer.from(message, "utf8"),
  ]);
}

/** SHA-256 of the SEP-53 payload — this is what wallets sign with ed25519. */
export function sep53MessageHash(message: string): Buffer {
  return Buffer.from(sha256(sep53Payload(message)));
}

/**
 * Verify a Freighter / SEP-53 message signature.
 * `signature` may be base64, hex (128 chars), or raw 64-byte buffer.
 */
export function normalizeEd25519Signature(
  signature: string | Buffer | Uint8Array,
): Buffer | null {
  try {
    if (typeof signature !== "string") {
      const buf = Buffer.from(signature);
      return buf.length === 64 ? buf : null;
    }
    const trimmed = signature.trim();
    if (/^[0-9a-fA-F]{128}$/.test(trimmed)) {
      return Buffer.from(trimmed, "hex");
    }
    const b64 = Buffer.from(trimmed, "base64");
    if (b64.length === 64) return b64;
    return null;
  } catch {
    return null;
  }
}

export function verifySep53Message(
  address: string,
  message: string,
  signature: string | Buffer | Uint8Array,
): boolean {
  try {
    const sigBuf = normalizeEd25519Signature(signature);
    if (!sigBuf) return false;
    const kp = Keypair.fromPublicKey(address);
    return kp.verify(sep53MessageHash(message), sigBuf);
  } catch {
    return false;
  }
}
