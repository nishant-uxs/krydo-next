import type { Request, Response, NextFunction } from "express";
import type { AuthPayload } from "./jwt";

/**
 * Centralized authorization helpers.
 *
 * Authentication (`requireAuth`) proves wallet identity via JWT.
 * These helpers decide whether that identity may touch a given resource.
 *
 * Stellar StrKey addresses are case-sensitive — comparisons are exact.
 */

export function canAccessAsSelf(auth: AuthPayload, address: string): boolean {
  return auth.sub === address;
}

export function canAccessCredential(
  auth: AuthPayload,
  cred: { holderAddress: string; issuerAddress: string },
): boolean {
  if (auth.role === "root") return true;
  if (auth.sub === cred.holderAddress) return true;
  if (auth.sub === cred.issuerAddress) return true;
  return false;
}

/**
 * Authenticated caller must own the `:address` path param (exact StrKey match).
 * Root may still only use their own address in the path for list endpoints —
 * those routes expand to global data when the wallet role is root.
 */
export function requireSelfAddress(param = "address") {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.auth) {
      return res.status(401).json({ message: "Authentication required" });
    }
    const target = req.params[param];
    if (!target || typeof target !== "string") {
      return res.status(400).json({ message: "Address missing from request" });
    }
    if (!canAccessAsSelf(req.auth, target)) {
      return res.status(403).json({ message: "Forbidden: can only access your own resources" });
    }
    return next();
  };
}
