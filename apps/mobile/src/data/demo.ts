/**
 * Synthetic demo credentials for Krydo Mobile v1 foundation.
 * Clearly fake — never real PII.
 */

export interface StoredCredential {
  id: string;
  title: string;
  claimType: string;
  issuerName: string;
  issuerAddress: string;
  holderAddress: string;
  holderName: string;
  status: "active" | "revoked" | "expired";
  issuedAt: string;
  expiresAt: string | null;
  credentialHash: string;
  /** Non-sensitive display summary only — not raw claim payloads. */
  displaySummary: string;
}

export const DEMO_HOLDER = {
  name: "Alex Demo",
  email: "demo@example.invalid",
  address: "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H",
} as const;

export const DEMO_CREDENTIALS: StoredCredential[] = [
  {
    id: "11111111-1111-4111-8111-111111111111",
    title: "B.Tech Computer Science",
    claimType: "identity_verification",
    issuerName: "Example University",
    issuerAddress: "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    holderAddress: DEMO_HOLDER.address,
    holderName: DEMO_HOLDER.name,
    status: "active",
    issuedAt: "2024-06-15T00:00:00.000Z",
    expiresAt: "2030-06-15T00:00:00.000Z",
    credentialHash: "a1".repeat(32),
    displaySummary: "Degree credential (demo)",
  },
  {
    id: "22222222-2222-4222-8222-222222222222",
    title: "Credit Score Range",
    claimType: "credit_score",
    issuerName: "Example Credit Bureau",
    issuerAddress: "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF",
    holderAddress: DEMO_HOLDER.address,
    holderName: DEMO_HOLDER.name,
    status: "active",
    issuedAt: "2025-01-10T00:00:00.000Z",
    expiresAt: "2027-01-10T00:00:00.000Z",
    credentialHash: "b2".repeat(32),
    displaySummary: "Score band credential (demo)",
  },
];

export const DEMO_ACTIVITY = [
  {
    id: "act-1",
    title: "Opened Krydo Mobile",
    detail: "Foundation build — demo mode",
    at: "2026-09-10T10:00:00.000Z",
  },
  {
    id: "act-2",
    title: "Loaded demo credentials",
    detail: "2 synthetic credentials available",
    at: "2026-09-10T10:01:00.000Z",
  },
];
