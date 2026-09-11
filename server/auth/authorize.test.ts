import { describe, it, expect, vi, beforeEach } from "vitest";
import type { Request, Response, NextFunction } from "express";
import {
  canAccessAsSelf,
  canAccessCredential,
  requireSelfAddress,
} from "./authorize";
import type { AuthPayload } from "./jwt";

const HOLDER = "GBXFXNDLV4LSWA4VB7YIL5GBD7BVNR22SGBTDKMO2SBZZHDXSKZYCP7L";
const ISSUER = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF";
const OTHER = "GBLZQQXJ3W3XHL3N7Q7H2W5YV5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y5Y";

describe("authorize helpers", () => {
  it("canAccessAsSelf requires exact StrKey match", () => {
    const auth = { sub: HOLDER, role: "user" } as AuthPayload;
    expect(canAccessAsSelf(auth, HOLDER)).toBe(true);
    expect(canAccessAsSelf(auth, OTHER)).toBe(false);
  });

  it("canAccessCredential allows holder, issuer, and root", () => {
    const cred = { holderAddress: HOLDER, issuerAddress: ISSUER };
    expect(canAccessCredential({ sub: HOLDER, role: "user" }, cred)).toBe(true);
    expect(canAccessCredential({ sub: ISSUER, role: "issuer" }, cred)).toBe(true);
    expect(canAccessCredential({ sub: OTHER, role: "root" }, cred)).toBe(true);
    expect(canAccessCredential({ sub: OTHER, role: "user" }, cred)).toBe(false);
  });
});

describe("requireSelfAddress middleware", () => {
  let next: NextFunction;

  beforeEach(() => {
    next = vi.fn();
  });

  function mockRes() {
    const res: Partial<Response> = {};
    res.status = vi.fn().mockReturnValue(res);
    res.json = vi.fn().mockReturnValue(res);
    return res as Response;
  }

  it("401 when unauthenticated", () => {
    const guard = requireSelfAddress("address");
    const req = { params: { address: HOLDER } } as unknown as Request;
    const res = mockRes();
    guard(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it("allows owner", () => {
    const guard = requireSelfAddress("address");
    const req = {
      params: { address: HOLDER },
      auth: { sub: HOLDER, role: "user" },
    } as unknown as Request;
    const res = mockRes();
    guard(req, res, next);
    expect(next).toHaveBeenCalledOnce();
  });

  it("403 when another authenticated user requests the resource", () => {
    const guard = requireSelfAddress("address");
    const req = {
      params: { address: HOLDER },
      auth: { sub: OTHER, role: "user" },
    } as unknown as Request;
    const res = mockRes();
    guard(req, res, next);
    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });
});
