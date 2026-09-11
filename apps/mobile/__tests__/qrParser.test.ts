import { describe, it, expect } from "vitest";
import { parseKrydoPresentationUri } from "../src/qr/QRRequestParser";

describe("QRRequestParser", () => {
  it("accepts krydo://present?request=req_123", () => {
    const r = parseKrydoPresentationUri("krydo://present?request=req_123");
    expect(r).toEqual({ ok: true, requestId: "req_123", source: "krydo" });
  });

  it("accepts UUID request ids", () => {
    const id = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
    const r = parseKrydoPresentationUri(`krydo://present?request=${id}`);
    expect(r.ok && r.requestId).toBe(id);
  });

  it("rejects https://evil.example", () => {
    const r = parseKrydoPresentationUri("https://evil.example");
    expect(r.ok).toBe(false);
  });

  it("rejects krydo://something-else", () => {
    const r = parseKrydoPresentationUri("krydo://something-else");
    expect(r.ok).toBe(false);
  });

  it("rejects krydo://present without request", () => {
    const r = parseKrydoPresentationUri("krydo://present");
    expect(r.ok).toBe(false);
  });

  it("rejects krydo://present?request=", () => {
    const r = parseKrydoPresentationUri("krydo://present?request=");
    expect(r.ok).toBe(false);
  });

  it("accepts https krydo.dev present links", () => {
    const r = parseKrydoPresentationUri(
      "https://krydo.dev/present?request=req_abc123",
    );
    expect(r).toEqual({ ok: true, requestId: "req_abc123", source: "https" });
  });
});
