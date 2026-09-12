/**
 * Attach explorer-linkable issue tx hashes onto credential list items.
 * Repairs legacy rows where the Stellar hash was saved but data.onChain
 * stayed false (pre-flag updateTransactionTxHash).
 */
import type { Credential, Transaction } from "@shared/schema";
import { isOffChainTx } from "@shared/schema";
import { storage } from "../storage";
import { verifyCredentialOnChain, isChainReadable } from "../blockchain";
import { childLogger } from "../logger";

const log = childLogger("credentials/issue-tx");

function credHashFromTx(tx: Transaction): string | null {
  const d = tx.data as { credentialHash?: unknown } | null | undefined;
  return typeof d?.credentialHash === "string" ? d.credentialHash : null;
}

export async function enrichCredentialsWithIssueTx(
  items: Credential[],
  holderOrIssuerAddress: string,
): Promise<Array<Credential & { txHash: string | null }>> {
  if (items.length === 0) return [];

  const txs = await storage.getTransactions(holderOrIssuerAddress);
  const byHash = new Map<string, Transaction>();
  for (const tx of txs) {
    if (tx.action !== "credential_issued") continue;
    const h = credHashFromTx(tx);
    if (!h) continue;
    const key = h.toLowerCase();
    const prev = byHash.get(key);
    if (!prev) {
      byHash.set(key, tx);
      continue;
    }
    // Prefer flagged on-chain rows over off-chain / synthetic.
    if (isOffChainTx(prev) && !isOffChainTx(tx)) {
      byHash.set(key, tx);
    }
  }

  const chainReadable = isChainReadable();

  return Promise.all(
    items.map(async (c) => {
      const stored = c.onChainTxHash?.trim() || null;
      if (stored && !/^0+$/i.test(stored)) {
        return { ...c, txHash: stored };
      }

      const tx = byHash.get(c.credentialHash.toLowerCase());
      if (!tx) return { ...c, txHash: null };

      if (!isOffChainTx(tx)) {
        return { ...c, txHash: tx.txHash };
      }

      // Legacy repair: non-zero hash + credential actually on Soroban.
      if (!/^0+$/i.test(tx.txHash) && chainReadable) {
        try {
          const onChain = await verifyCredentialOnChain(c.credentialHash);
          if (onChain.valid) {
            await storage.updateTransactionTxHash(tx.id, tx.txHash);
            log.info(
              { credentialHash: c.credentialHash, txHash: tx.txHash },
              "repaired legacy issue tx onChain flag",
            );
            return { ...c, txHash: tx.txHash };
          }
        } catch (err) {
          log.warn(
            { err: err instanceof Error ? err.message : String(err) },
            "legacy issue-tx repair skipped",
          );
        }
      }

      return { ...c, txHash: null };
    }),
  );
}
