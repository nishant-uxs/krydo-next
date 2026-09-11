import type { Express } from "express";
import { storage } from "../storage";
import { requireAuth } from "../auth/jwt";
import { requireSelfAddress } from "../auth/authorize";
import { readPageOpts, sendPage } from "../middleware/pagination";

/** Millisecond threshold: credentials expiring within 30 days count as "expiring soon". */
const EXPIRING_SOON_WINDOW_MS = 30 * 86_400_000;

/**
 * Dashboard statistics + transaction history endpoints.
 *
 * All address-scoped reads require auth + self (IDOR hardening).
 * Root wallets still see global stats/transactions when calling with their
 * own address (server expands based on wallet.role === "root").
 */
export function registerStatsRoutes(app: Express) {
  app.get(
    "/api/stats/:address",
    requireAuth,
    requireSelfAddress("address"),
    async (req, res) => {
    try {
      const address = req.params.address as string;
      const wallet = await storage.getWallet(address);
      const role = wallet?.role || "user";
      res.json(await storage.getStats(address, role));
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Internal server error";
      res.status(500).json({ message });
    }
  });

  app.get(
    "/api/transactions/:address",
    requireAuth,
    requireSelfAddress("address"),
    async (req, res) => {
    try {
      const address = req.params.address as string;
      const opts = readPageOpts(req);
      const wallet = await storage.getWallet(address);
      const page = wallet?.role === "root"
        ? await storage.listTransactionsPaged(undefined, opts)
        : await storage.listTransactionsPaged(address, opts);
      sendPage(res, page);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Internal server error";
      res.status(500).json({ message });
    }
  });

  app.get(
    "/api/transactions/recent/:address",
    requireAuth,
    requireSelfAddress("address"),
    async (req, res) => {
    try {
      const address = req.params.address as string;
      const wallet = await storage.getWallet(address);
      // Recent = small non-paginated list, capped at 10 items.
      const page = wallet?.role === "root"
        ? await storage.listTransactionsPaged(undefined, { limit: 10 })
        : await storage.listTransactionsPaged(address, { limit: 10 });
      res.json(page.items);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Internal server error";
      res.status(500).json({ message });
    }
  });

  /**
   * Issuer analytics — counts and buckets of everything this issuer has ever
   * touched. Auth + self required (aggregate counts still reveal activity).
   */
  app.get(
    "/api/stats/issuer/:address",
    requireAuth,
    requireSelfAddress("address"),
    async (req, res) => {
    try {
      const address = req.params.address as string;
      const issuer = await storage.getIssuerByAddress(address);
      if (!issuer) return res.status(404).json({ message: "Issuer not found" });

      const credentials = await storage.getCredentialsByIssuer(address);
      const now = Date.now();

      const totalIssued = credentials.length;
      const activeCount = credentials.filter(c => c.status === "active").length;
      const revokedCount = credentials.filter(c => c.status !== "active").length;
      const expiredCount = credentials.filter(
        c => c.expiresAt && c.expiresAt.getTime() < now,
      ).length;
      const expiringSoonCount = credentials.filter(
        c =>
          c.status === "active" &&
          c.expiresAt &&
          c.expiresAt.getTime() >= now &&
          c.expiresAt.getTime() < now + EXPIRING_SOON_WINDOW_MS,
      ).length;

      // Group by claimType so the dashboard can render a breakdown chart.
      const byClaimType: Record<string, number> = {};
      for (const c of credentials) {
        byClaimType[c.claimType] = (byClaimType[c.claimType] ?? 0) + 1;
      }

      res.json({
        issuer: { name: issuer.name, active: issuer.active, category: issuer.category },
        totalIssued,
        activeCount,
        revokedCount,
        expiredCount,
        expiringSoonCount,
        byClaimType,
        generatedAt: new Date().toISOString(),
      });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Internal server error";
      res.status(500).json({ message });
    }
  });
}
