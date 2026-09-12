import type { Express, Request, Response } from "express";
import { z } from "zod";
import { storage } from "../storage";
import {
  getDeployment,
  isBlockchainReady,
  isIssuerOnChain,
} from "../blockchain";
import { stellarAddressSchema, type WalletRole } from "@shared/schema";
import { DEPLOYMENT, AUDIT_ID } from "@shared/contracts";
import { verifySep53Message } from "@shared/sep53";
import { NETWORK_LABEL } from "@shared/contracts";
import { issueNonce, consumeNonce } from "./nonce-store";
import { signAuthToken, requireAuth } from "./jwt";
import { sensitiveLimiter } from "../middleware/security";
import { childLogger } from "../logger";

const log = childLogger("auth/siws");

/**
 * Sign-in-with-Stellar (SIWS).
 *
 * The client fetches a server-issued nonce, builds a canonical human-readable
 * message embedding that nonce + its own address, and has Freighter sign it
 * per SEP-53. We verify with `verifySep53Message` (NOT raw Keypair.verify on
 * UTF-8 bytes — Freighter signs SHA-256("Stellar Signed Message:\\n" + msg)).
 */
const verifySchema = z.object({
  address: stellarAddressSchema,
  message: z.string().min(20).max(4_000),
  // base64 or hex-encoded 64-byte ed25519 signature.
  signature: z.string().min(16).max(1_024),
});

/** WC one-tap: address from Freighter session (no second SEP-53 popup). */
const wcSessionSchema = z.object({
  address: stellarAddressSchema,
  chainId: z.string().min(3).max(64).optional(),
  topic: z.string().min(8).max(128).optional(),
  provider: z.literal("freighter-wc").default("freighter-wc"),
});

async function resolveRoleAndIssueSession(address: string) {
  const deployerAddr = getDeployment()?.deployer || DEPLOYMENT.deployer || "";
  let role: WalletRole = "user";
  let label = "User";

  if (deployerAddr && address === deployerAddr) {
    role = "root";
    label = "Root Authority";
  } else {
    const issuer = await storage.getIssuerByAddress(address);
    if (issuer && issuer.active) {
      role = "issuer";
      label = issuer.name;
    } else if (isBlockchainReady()) {
      try {
        if (await isIssuerOnChain(address)) {
          role = "issuer";
          label = "Trusted Issuer";
        }
      } catch {
        /* fall through to user */
      }
    }
  }

  const previous = await storage.getWallet(address);
  const wallet = await storage.connectWallet(address, role, label);
  const roleChanged = !previous || previous.role !== role;
  const neverAnchored = !previous || !previous.onChainTxHash;
  const needsRoleAnchor = !!(AUDIT_ID && (roleChanged || neverAnchored));
  const token = signAuthToken({ sub: address, role, chain: "stellar" });
  return { token, wallet, needsRoleAnchor };
}

/**
 * Shared SIWS verify: address bind + SEP-53 + single-use nonce.
 */
async function verifySiwsOwnership(
  address: string,
  message: string,
  signature: string,
): Promise<{ ok: true } | { ok: false; status: number; message: string }> {
  if (!message.includes(address)) {
    return { ok: false, status: 401, message: "Message does not match address" };
  }
  const nonceMatch = message.match(/Nonce:\s*([a-fA-F0-9]{8,})/);
  if (!nonceMatch) {
    return { ok: false, status: 401, message: "Message is missing a nonce" };
  }
  const nonce = nonceMatch[1];

  if (!verifySep53Message(address, message, signature)) {
    return { ok: false, status: 401, message: "Signature verification failed" };
  }

  if (!(await consumeNonce(nonce, address))) {
    return { ok: false, status: 401, message: "Invalid or expired nonce" };
  }

  return { ok: true };
}

export function registerAuthRoutes(app: Express) {
  /** GET /api/auth/nonce?address=G... — returns a server-issued nonce to sign. */
  app.get("/api/auth/nonce", sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      const address = stellarAddressSchema.parse(req.query.address);
      const { nonce, expiresAt } = await issueNonce(address);
      res.json({ nonce, expiresAt });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      res.status(500).json({ message: err.message });
    }
  });

  /**
   * POST /api/auth/wc-session — Freighter WalletConnect one-tap login.
   * After the user Approves the WC session in Freighter, the mobile app sends
   * the revealed G… address. No second SEP-53 sign popup (Freighter mobile
   * signMessage is unreliable across WC versions).
   * Web login still uses cryptographically bound POST /api/auth/verify.
   */
  app.post("/api/auth/wc-session", sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      const { address, chainId } = wcSessionSchema.parse(req.body);
      if (chainId && !chainId.startsWith("stellar:")) {
        return res.status(400).json({ message: "chainId must be a stellar CAIP-2 id" });
      }
      const session = await resolveRoleAndIssueSession(address);
      log.info({ address, chainId, provider: "freighter-wc" }, "wc-session login");
      res.json(session);
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      log.error({ err }, "wc-session failed");
      res.status(401).json({ message: err.message || "WalletConnect session login failed" });
    }
  });

  /** POST /api/auth/verify — verifies the signed message, issues a JWT. */
  app.post("/api/auth/verify", sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      const { address, message, signature } = verifySchema.parse(req.body);
      const verified = await verifySiwsOwnership(address, message, signature);
      if (!verified.ok) {
        return res.status(verified.status).json({ message: verified.message });
      }
      const session = await resolveRoleAndIssueSession(address);
      res.json(session);
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      res.status(401).json({ message: err.message || "Authentication failed" });
    }
  });

  /** POST /api/auth/role-anchor — record a wallet-signed role audit tx. */
  app.post("/api/auth/role-anchor", requireAuth, sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      if (!req.auth) return res.status(401).json({ message: "Not authenticated" });
      const schema = z.object({
        txHash: z.string().regex(/^[0-9a-f]{64}$/i, "Invalid Stellar tx hash"),
      });
      const { txHash } = schema.parse(req.body);
      const address = req.auth.sub;

      const { waitForClientTx } = await import("../blockchain");
      const result = await waitForClientTx(txHash, { timeoutMs: 45_000 });
      if (result.status === "unknown") {
        return res.status(422).json({
          message: `Role-anchor tx not found on Stellar. Retry signing on ${NETWORK_LABEL}.`,
        });
      }
      if (result.status === "reverted") {
        return res.status(422).json({ message: "Role-anchor tx reverted on-chain." });
      }
      if (result.status !== "confirmed") {
        return res.status(422).json({ message: "Role-anchor tx still pending on Stellar. Retry shortly." });
      }

      await storage.updateWalletOnChainTxHash(address, txHash);
      await storage.createTransaction({
        txHash,
        action: "role_assigned_onchain",
        fromAddress: address,
        data: { role: req.auth.role, onChain: true, walletSigned: true },
        blockNumber: result.blockNumber,
      });

      const wallet = await storage.getWallet(address);
      res.json({ success: true, wallet, txHash });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      log.error({ err: err.message }, "role-anchor failed");
      res.status(500).json({ message: err.message });
    }
  });

  /** GET /api/auth/me — returns the current session wallet if JWT is valid. */
  app.get("/api/auth/me", async (req: Request, res: Response) => {
    if (!req.auth) return res.status(401).json({ message: "Not authenticated" });
    const wallet = await storage.getWallet(req.auth.sub);
    if (!wallet) return res.status(404).json({ message: "Wallet not found" });
    res.json({ wallet });
  });
}
