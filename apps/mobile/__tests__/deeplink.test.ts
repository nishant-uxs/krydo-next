import { describe, it, expect } from "vitest";
import { parseKrydoPresentationUri } from "../src/qr/QRRequestParser";

describe("deep link → prove route mapping", () => {
  it("maps krydo URI to /prove/:requestId", () => {
    const parsed = parseKrydoPresentationUri(
      "krydo://present?request=aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
    );
    expect(parsed.ok).toBe(true);
    if (parsed.ok) {
      const href = `/prove/${parsed.requestId}`;
      expect(href).toBe("/prove/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
    }
  });
});
