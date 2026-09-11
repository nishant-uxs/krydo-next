/**
 * Typed Krydo API client for presentation + verify endpoints.
 * Does not duplicate server business logic.
 */

import { getConfig } from "../config/env";
import { safeLog } from "../config/logger";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly body?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export interface PresentationRequestDto {
  id: string;
  version: string;
  verifier: string;
  audience: string;
  challenge: string;
  reason: string | null;
  requestedCredentials: Array<{ claimType: string }>;
  policy: {
    kind: string;
    claimType: string;
    threshold?: number;
    targetValue?: string;
    memberSet?: string[];
    fields?: string[];
  };
  createdAt: string;
  expiresAt: string;
  deepLink: string;
}

export interface PresentationVerifyResultDto {
  valid: boolean;
  message: string;
  checks?: Record<string, boolean>;
  credential?: {
    credentialHash: string;
    claimType: string;
    status: string;
    issuerAddress: string;
    holderAddress: string;
    expiresAt: string | null;
  } | null;
  issuerName?: string | null;
  proof?: { type: string; proofId?: string } | null;
}

export interface VerificationClient {
  getPresentationRequest(requestId: string): Promise<PresentationRequestDto>;
  createPresentation(input: {
    requestId: string;
    credentialId: string;
    proofId?: string;
    authToken: string;
  }): Promise<unknown>;
  verifyPresentation(presentation: unknown): Promise<PresentationVerifyResultDto>;
  verifyCredential(credentialHash: string): Promise<unknown>;
}

async function parseJson(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    throw new ApiError("Malformed JSON response", res.status, text);
  }
}

export class HttpVerificationClient implements VerificationClient {
  constructor(private readonly baseUrl: string = getConfig().apiBaseUrl) {}

  private async request(
    method: string,
    path: string,
    opts: { body?: unknown; authToken?: string } = {},
  ): Promise<unknown> {
    const headers: Record<string, string> = {
      Accept: "application/json",
    };
    if (opts.body !== undefined) headers["Content-Type"] = "application/json";
    if (opts.authToken) headers.Authorization = `Bearer ${opts.authToken}`;

    let res: Response;
    try {
      res = await fetch(`${this.baseUrl}${path}`, {
        method,
        headers,
        body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
      });
    } catch {
      throw new ApiError("Network failure — check API_BASE_URL and device connectivity", 0);
    }

    const data = await parseJson(res);
    if (!res.ok) {
      const msg =
        data && typeof data === "object" && "message" in data
          ? String((data as { message: unknown }).message)
          : `HTTP ${res.status}`;
      safeLog("api error", { path, status: res.status });
      throw new ApiError(msg, res.status, data);
    }
    return data;
  }

  async getPresentationRequest(requestId: string): Promise<PresentationRequestDto> {
    return (await this.request(
      "GET",
      `/api/presentations/request/${encodeURIComponent(requestId)}`,
    )) as PresentationRequestDto;
  }

  async createPresentation(input: {
    requestId: string;
    credentialId: string;
    proofId?: string;
    authToken: string;
  }): Promise<unknown> {
    return this.request("POST", "/api/presentations/create", {
      authToken: input.authToken,
      body: {
        requestId: input.requestId,
        credentialId: input.credentialId,
        ...(input.proofId ? { proofId: input.proofId } : {}),
      },
    });
  }

  async verifyPresentation(presentation: unknown): Promise<PresentationVerifyResultDto> {
    return (await this.request("POST", "/api/presentations/verify", {
      body: { presentation },
    })) as PresentationVerifyResultDto;
  }

  async verifyCredential(credentialHash: string): Promise<unknown> {
    return this.request("POST", "/api/verify", {
      body: { credentialHash },
    });
  }
}

/** Deterministic mock client for offline / demo UI. */
export class MockVerificationClient implements VerificationClient {
  private requests = new Map<string, PresentationRequestDto>();

  seedRequest(req: PresentationRequestDto): void {
    this.requests.set(req.id, req);
  }

  async getPresentationRequest(requestId: string): Promise<PresentationRequestDto> {
    const found = this.requests.get(requestId);
    if (found) {
      if (new Date(found.expiresAt).getTime() <= Date.now()) {
        throw new ApiError("Presentation request expired", 410);
      }
      return found;
    }
    // Demo fallback for deep-link testing without a live backend.
    return {
      id: requestId,
      version: "krydo-vp-request-v1",
      verifier: "GDEMOVERIFIER000000000000000000000000000000000000000",
      audience: "https://verifier.example.invalid",
      challenge: "c".repeat(64),
      reason: "Demo eligibility check (mock request)",
      requestedCredentials: [{ claimType: "identity_verification" }],
      policy: { kind: "credential_status", claimType: "identity_verification" },
      createdAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 600_000).toISOString(),
      deepLink: `krydo://present?request=${requestId}`,
    };
  }

  async createPresentation(_input?: {
    requestId: string;
    credentialId: string;
    proofId?: string;
    authToken: string;
  }): Promise<unknown> {
    return {
      "@context": ["https://www.w3.org/ns/credentials/v2"],
      type: ["VerifiablePresentation", "KrydoPresentation"],
      id: `urn:uuid:demo-vp`,
      demo: true,
      note: "DEMO / MOCK PRESENTATION — not cryptographically valid",
    };
  }

  async verifyPresentation(_presentation?: unknown): Promise<PresentationVerifyResultDto> {
    return {
      valid: true,
      message: "DEMO verification result — mock only",
      checks: {
        structure: true,
        holderBinding: true,
        challenge: true,
        audience: true,
        expiration: true,
        credentialStatus: true,
        issuerTrusted: true,
        proof: true,
        replay: false,
      },
      credential: {
        credentialHash: "a1".repeat(32),
        claimType: "identity_verification",
        status: "active",
        issuerAddress: "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        holderAddress: "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H",
        expiresAt: "2030-06-15T00:00:00.000Z",
      },
      issuerName: "Example University",
      proof: { type: "KrydoCredentialReference2025" },
    };
  }

  async verifyCredential(): Promise<unknown> {
    return { valid: true, message: "DEMO credential status" };
  }
}

let client: VerificationClient | null = null;

export function getVerificationClient(): VerificationClient {
  if (!client) {
    client = getConfig().useMockData
      ? new MockVerificationClient()
      : new HttpVerificationClient();
  }
  return client;
}

export function __setVerificationClientForTests(c: VerificationClient | null): void {
  client = c;
}
