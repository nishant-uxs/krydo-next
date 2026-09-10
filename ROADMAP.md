# Krydo Next — Roadmap

Base: Stellar / Soroban Krydo (copied from `krydo-stellar`).  
**Do not break the SIH demo** — that lives in the frozen `krydo-stellar` repo.

## Phase 1 — Interop core
- [ ] W3C VC **and** Verifiable Presentation (VP) export/import
- [ ] Public `POST /api/verify` accepting VP JSON → pass/fail + revoke status
- [ ] Standard QR payload (deep-link / VP reference), not proprietary-only blobs
- [ ] OpenAPI snippet for verifier integrators

## Phase 2 — India-stack friendly adapters
- [ ] Issuer adapter interface (DigiLocker-like attest **in**, no raw PII on-chain)
- [ ] Clear DPDP / off-chain PII framing in docs

## Phase 3 — Network & ops
- [ ] Clean multi-network config (testnet / mainnet)
- [ ] Optional L2 or fee documentation for scale (Stellar fees already low)

## Phase 4 — Thin mobile app
- [ ] Expo/React Native **verifier** (scan QR → call verify API)
- [ ] Optional holder “prove” sheet later — SDK first, UI second

## Phase 5 — Product depth
- [ ] Compound policies (e.g. age ≥ 18 AND KYC verified)
- [ ] Stronger revoke/audit timeline UX
- [ ] Time-bound / replay-protected presentations

## Non-goals (for now)
- Rewriting the whole web UI from scratch
- Tokenomics / multi-chain circus day one
- Touching `krydo-stellar` production deploy for experiments
