import jwt from "jsonwebtoken";
import type { Request, Response, NextFunction } from "express";
import { config } from "../config";
import type { WalletRole } from "@shared/schema";

export interface AuthPayload {
  sub: string; // Stellar StrKey wallet address (case-sensitive)
  role: WalletRole;
  iat?: number;
  exp?: number;
  iss?: string;
  aud?: string | string[];
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      auth?: AuthPayload;
    }
  }
}

/** Keep 7d TTL — product has no refresh-token path yet. */
const TOKEN_TTL = "7d";

/**
 * Explicit JWT constraints.
 * - HS256 only (rejects alg=none / RS* confusion)
 * - iss/aud bound so tokens are not reusable across unrelated services
 *
 * Remaining limitation: no server-side revocation list / jti denylist.
 * Compromised tokens remain valid until exp unless JWT_SECRET is rotated.
 */
const JWT_ISSUER = "krydo";
const JWT_AUDIENCE = "krydo-api";

const SIGN_OPTIONS: jwt.SignOptions = {
  expiresIn: TOKEN_TTL,
  algorithm: "HS256",
  issuer: JWT_ISSUER,
  audience: JWT_AUDIENCE,
};

const VERIFY_OPTIONS: jwt.VerifyOptions = {
  algorithms: ["HS256"],
  issuer: JWT_ISSUER,
  audience: JWT_AUDIENCE,
};

export function signAuthToken(payload: Omit<AuthPayload, "iat" | "exp" | "iss" | "aud">): string {
  // Payload is intentionally minimal: wallet address + role only.
  return jwt.sign({ sub: payload.sub, role: payload.role }, config.JWT_SECRET, SIGN_OPTIONS);
}

export function verifyAuthToken(token: string): AuthPayload | null {
  try {
    const decoded = jwt.verify(token, config.JWT_SECRET, VERIFY_OPTIONS);
    if (typeof decoded !== "object" || decoded === null) return null;
    const obj = decoded as jwt.JwtPayload;
    if (typeof obj.sub !== "string" || typeof obj.role !== "string") return null;
    return {
      sub: obj.sub,
      role: obj.role as WalletRole,
      iat: typeof obj.iat === "number" ? obj.iat : undefined,
      exp: typeof obj.exp === "number" ? obj.exp : undefined,
      iss: typeof obj.iss === "string" ? obj.iss : undefined,
      aud: obj.aud,
    };
  } catch {
    return null;
  }
}

function extractToken(req: Request): string | null {
  const header = req.headers.authorization;
  if (header && header.startsWith("Bearer ")) {
    return header.slice(7).trim();
  }
  return null;
}

/** Populates req.auth if a valid token is present. Never rejects. */
export function attachAuth(req: Request, _res: Response, next: NextFunction) {
  const token = extractToken(req);
  if (token) {
    const payload = verifyAuthToken(token);
    if (payload) req.auth = payload;
  }
  next();
}

/** Guard: rejects with 401 if no valid token. */
export function requireAuth(req: Request, res: Response, next: NextFunction) {
  if (!req.auth) {
    return res.status(401).json({ message: "Authentication required" });
  }
  next();
}

/** Guard factory: rejects unless the authenticated role is in the whitelist. */
export function requireRole(...allowed: WalletRole[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.auth) {
      return res.status(401).json({ message: "Authentication required" });
    }
    if (!allowed.includes(req.auth.role)) {
      return res.status(403).json({ message: "Insufficient role" });
    }
    next();
  };
}

/**
 * Guard: authenticated user must match the address in the given param/body field.
 * Prevents acting on behalf of another wallet via forged request bodies.
 */
export function requireSelf(field: { param?: string; bodyKey?: string }) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.auth) {
      return res.status(401).json({ message: "Authentication required" });
    }
    const candidate = field.param
      ? (req.params[field.param] as string | undefined)
      : field.bodyKey
        ? (req.body?.[field.bodyKey] as string | undefined)
        : undefined;
    if (!candidate || typeof candidate !== "string") {
      return res.status(400).json({ message: "Address missing from request" });
    }
    if (candidate !== req.auth.sub) {
      return res.status(403).json({ message: "Address does not match authenticated wallet" });
    }
    next();
  };
}
