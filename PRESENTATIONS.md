# Verifiable Presentations (P1)

Protocol foundation for presentation requests, challenges, and short-lived Verifiable Presentations (VPs). Intended for the future Krydo mobile wallet.

## Concepts

| Object | Lifetime | Who creates it |
|--------|----------|----------------|
| **Credential (VC)** | Long-lived | Issuer |
| **Presentation Request** | Short-lived | Verifier (authenticated) |
| **Verifiable Presentation (VP)** | Short-lived, one-shot | Holder (for one request) |

A VP is **not** another copy of the VC. It binds:

```text
holder + challenge + audience + credential reference + (optional ZK proof)
```

## Endpoints

### `POST /api/presentations/request`

**Auth:** required (`requireAuth`). Verifier = JWT `sub`.

**Body (strict — extra keys rejected):**

```json
{
  "audience": "https://lender.example",
  "reason": "Loan eligibility check",
  "ttlSeconds": 600,
  "requestedCredentials": [{ "claimType": "credit_score" }],
  "policy": {
    "kind": "credential_status",
    "claimType": "credit_score"
  }
}
```

Server generates: `id`, `challenge` (32-byte hex), `createdAt`, `expiresAt`, `verifier`, `deepLink`.

Client-supplied `challenge`, `verifier`, `id`, or `expiresAt` are **rejected**.

**Policies (typed, not string expressions):**

| `kind` | Notes |
|--------|--------|
| `credential_status` | Prove active credential of type (no ZK required) |
| `range_above` / `range_below` | Requires ZK proof (`threshold`) |
| `equality` | Requires ZK (`targetValue`) |
| `membership` | Requires ZK (`memberSet`) |
| `non_zero` | Requires ZK |
| `selective_disclosure` | Requires ZK (`fields`) |

### `GET /api/presentations/request/:requestId`

**Auth:** public (opaque UUID). Safe for QR / deep-link retrieval.

Returns request metadata + challenge. **Never** contains `claimData` / `claimSummary`.

Statuses: `404` missing, `410` expired or already used.

Deep link shape (for future mobile):

```text
krydo://present?request=<requestId>
```

HTTPS equivalent: `{origin}/present?request=<requestId>` (SPA routing later).

### `POST /api/presentations/create`

**Auth:** required. Only the **credential holder** may create a VP.

```json
{
  "requestId": "…",
  "credentialId": "…",
  "proofId": "…" 
}
```

`proofId` required when policy needs ZK. Returns a typed VP **without** plaintext claims.

Does **not** sign with the holder’s private key — protocol serialization for wallet integration.

### `POST /api/presentations/verify`

**Auth:** public.

```json
{ "presentation": { /* VerifiablePresentation */ } }
```

Checks:

1. Structure  
2. Holder ↔ credential subject  
3. Challenge binding  
4. Audience / domain binding  
5. Request + VP expiration  
6. Credential status (active / not expired)  
7. Issuer trust (existing issuer registry)  
8. Existing ZK engine when proof type is `KrydoZkProof2025`  
9. **Replay:** challenge consumed after successful verify  

Response is status-only (no `claimData` / `claimSummary`):

```json
{
  "valid": true,
  "message": "Presentation verified",
  "checks": {
    "structure": true,
    "holderBinding": true,
    "challenge": true,
    "audience": true,
    "expiration": true,
    "credentialStatus": true,
    "issuerTrusted": true,
    "proof": true,
    "replay": false
  },
  "credential": {
    "credentialHash": "…",
    "claimType": "credit_score",
    "status": "active",
    "issuerAddress": "G…",
    "holderAddress": "G…",
    "expiresAt": "…"
  },
  "issuerName": "…",
  "proof": { "type": "KrydoCredentialReference2025" }
}
```

## Challenge lifecycle

```text
create request → challenge issued (random, TTL-bound, audience-bound)
       ↓
holder builds VP with that challenge
       ↓
verify succeeds → challenge consumed
       ↓
same VP again → REJECTED (replay)
```

## Privacy guarantees

- Presentation verify does **not** return plaintext claims.
- Request retrieval does **not** include credential payloads.
- VP references credential by id/hash/type — not `claimData`.

## Honest limitations

- Presentation requests + challenges are **in-memory** (same multi-instance caveat as SIWS nonces).
- ZK proving for policy proofs still uses the **server-side** prover when generating proofs (`POST /api/zk/generate`).
- No holder private-key / device signature on the VP yet (mobile wallet phase).
- No compound AND/OR policy engine yet (typed single policy only).
- Not a full W3C Data Integrity proof suite — Krydo-native proof types.

## Types

- `shared/presentation.ts` — request + VP schemas, `createPresentation`
- `shared/presentation-policy.ts` — policy abstraction
- `server/presentations/request-store.ts` — challenge store
- `server/presentations/verify.ts` — verification engine
