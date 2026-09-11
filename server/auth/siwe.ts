import type { Express, Request, Response } from "express";
import { z } from "zod";
import { SiweMessage } from "siwe";
import {
  SUPPORTED_EVM_NUMERIC_IDS,
  evmCaip2,
  isEvmAddress,
  normalizeEvmAddress,
} from "@shared/wallet";
import { issueNonce, consumeNonce } from "./nonce-store";
import { signAuthToken } from "./jwt";
import { sensitiveLimiter } from "../middleware/security";
import { childLogger } from "../logger";

const log = childLogger("auth/siwe");

const nonceQuerySchema = z.object({
  address: z.string().refine(isEvmAddress, "Invalid EVM address"),
  chainId: z.coerce.number().int().positive(),
});

const verifySchema = z.object({
  message: z.string().min(20).max(8_000),
  signature: z.string().min(16).max(2_048),
});

function requestOrigin(req: Request): { domain: string; uri: string } {
  const proto = (req.headers["x-forwarded-proto"] as string)?.split(",")[0]?.trim() || req.protocol;
  const host =
    (req.headers["x-forwarded-host"] as string)?.split(",")[0]?.trim() ||
    req.headers.host ||
    "localhost";
  const domain = host.split(":")[0];
  const uri = `${proto}://${host}`;
  return { domain, uri };
}

/**
 * Sign-In with Ethereum (SIWE) — parallel to SIWS.
 * Does not replace Stellar auth. Issues the same JWT family with chain=eip155:N.
 */
export function registerSiweAuthRoutes(app: Express) {
  /** GET /api/auth/siwe/nonce?address=0x…&chainId=1 */
  app.get("/api/auth/siwe/nonce", sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      const { address, chainId } = nonceQuerySchema.parse(req.query);
      if (!SUPPORTED_EVM_NUMERIC_IDS.has(chainId)) {
        return res.status(400).json({ message: `Unsupported chainId ${chainId}` });
      }
      const normalized = normalizeEvmAddress(address);
      const caip2 = evmCaip2(chainId);
      const { nonce, expiresAt } = issueNonce(normalized, caip2);
      const { domain, uri } = requestOrigin(req);
      res.json({
        nonce,
        expiresAt,
        chainId,
        chain: caip2,
        domain,
        uri,
        statement: "Sign in to Krydo with your Ethereum wallet.",
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      log.error({ err }, "siwe nonce failed");
      res.status(500).json({ message: err.message ?? "Failed to issue SIWE nonce" });
    }
  });

  /** POST /api/auth/siwe/verify — { message, signature } */
  app.post("/api/auth/siwe/verify", sensitiveLimiter, async (req: Request, res: Response) => {
    try {
      const { message, signature } = verifySchema.parse(req.body);
      const { domain, uri } = requestOrigin(req);

      let siwe: SiweMessage;
      try {
        siwe = new SiweMessage(message);
      } catch {
        return res.status(400).json({ message: "Invalid SIWE message" });
      }

      if (!SUPPORTED_EVM_NUMERIC_IDS.has(siwe.chainId)) {
        return res.status(400).json({ message: `Unsupported chainId ${siwe.chainId}` });
      }

      // Domain / URI binding — reject cross-domain auth.
      if (siwe.domain !== domain) {
        return res.status(401).json({ message: "SIWE domain mismatch" });
      }
      if (siwe.uri && siwe.uri !== uri && !siwe.uri.startsWith(uri)) {
        // Allow trailing-slash / path variants on same origin
        try {
          const a = new URL(siwe.uri);
          const b = new URL(uri);
          if (a.host !== b.host || a.protocol !== b.protocol) {
            return res.status(401).json({ message: "SIWE URI mismatch" });
          }
        } catch {
          return res.status(401).json({ message: "SIWE URI mismatch" });
        }
      }

      const address = normalizeEvmAddress(siwe.address);
      const caip2 = evmCaip2(siwe.chainId);

      if (!consumeNonce(siwe.nonce, address, caip2)) {
        return res.status(401).json({ message: "Invalid or expired nonce" });
      }

      const result = await siwe.verify({
        signature,
        domain,
        nonce: siwe.nonce,
        time: new Date().toISOString(),
      });

      if (!result.success) {
        return res.status(401).json({
          message: result.error?.type ?? "SIWE verification failed",
        });
      }

      // EVM wallets are holders/users only in v1 (no Soroban issuer mapping).
      const token = signAuthToken({
        sub: address,
        role: "user",
        chain: caip2,
      });

      res.json({
        token,
        wallet: {
          address,
          role: "user",
          label: "EVM Wallet",
          chain: caip2,
          chainType: "EVM",
          onChainTxHash: null,
        },
        needsRoleAnchor: false,
      });
    } catch (err: any) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ message: err.issues[0].message });
      }
      log.error({ err }, "siwe verify failed");
      res.status(401).json({ message: err.message ?? "SIWE verification failed" });
    }
  });
}
