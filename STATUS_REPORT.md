# Krydo — Complete Status Report

**Date:** 10 Sep 2026  
**Audience:** Team Krydo (decision doc before next build)  
**Scope:** Where we are, what the idea is, what to build next — **no SIH deadline framing**.

---

## 1. Three repos (do not mix)

| Repo / folder | Role | Touch? |
|---------------|------|--------|
| **`krydo-stellar`** | Live SIH/demo product on Stellar Testnet | **Freeze** — show this |
| **`krydo-next`** | Evolution fork for interop / SDK / mobile | **Build here** |
| **`Kry-Decentralized-Infra`** | Older Ethereum/Sepolia sibling + SIH PPT assets | Reference / SIH docs only |

**Live demo (do not break):** https://krydo-stellar.vercel.app  

**New GitHub (this fork):** https://github.com/nishant-uxs/krydo-next  

---

## 2. Product idea (one sentence)

**Krydo** lets a holder prove a predicate about a credential (`score ≥ 700`, `age ≥ 18`, issuer whitelisted) **without revealing the raw value**, with trust anchors on-chain and PII off-chain.

### Why it matters
- Today KYC / eligibility often means full PDF / Aadhaar / bank dump oversharing  
- DigiLocker-style systems share **documents**; Krydo aims for **selective disclosure**  
- Target buyers long-term: banks, PSUs/BEL-style vendor clearance, campus credentials — **complement** national ID rails, don’t “replace DigiLocker” in the pitch  

### Core loop (already works on Stellar)
1. Root whitelists issuers (`KrydoAuthority`)  
2. Issuer issues credential hash on-chain + payload off-chain (`KrydoCredentials`)  
3. Holder generates Sigma/Pedersen ZK proof (browser / API)  
4. Verifier learns only pass/fail (+ can check revoke)  
5. Audit anchors optional (`KrydoAudit`)  

---

## 3. Current technical status (`krydo-next` = copy of stellar)

### Already strong
- **Stack:** React + Express + Firestore + Soroban (Rust) + Stellar Wallets Kit  
- **Auth:** SIWS (SEP-53) + JWT — wallet never sends seed to server  
- **Crypto:** Pedersen commitments + Sigma ZK (no SNARK trusted setup)  
- **Contracts:** Authority / Credentials / Audit  
- **Interop seed:** W3C VC 2.0 **export** (`shared/vc.ts`, `GET .../vc`)  
- **Quality bar:** ~168 Vitest tests, CI, DEPLOY/MAINNET/SECURITY docs, polished UI  
- **Git history on `krydo-next`:** 8 clean commits from bootstrap → client → deploy  

### Gaps (honest)
| Gap | Status |
|-----|--------|
| Verifiable **Presentation** (VP) import/export | Missing / incomplete vs VC export |
| Public standard **`POST /verify`** for third parties | Not a clean integrator API yet |
| Standard **QR → VP** payload | QR exists; not fully “interop standard” story |
| DigiLocker / India-stack **issuer adapter** | Idea only |
| **Mobile app** | Not started (correct — should be thin verifier later) |
| Multi-network polish | Testnet first; mainnet docs exist |
| Compound policies (AND/OR predicates) | Limited vs roadmap |
| Time-bound / replay-safe presentations | Weak / missing |
| Startup traction (pilots, LOIs) | Product ≠ company yet |

### Secrets / ops note
- `krydo-next` was copied **without** `.env` and Firebase admin JSON (good)  
- Local run needs `npm install` + `.env` from `.env.example`  
- Never commit service-account JSON  

---

## 4. Strategic options (ideas)

### A. Interop-first (recommended default)
Make Krydo a **credential layer other apps can call**:
- VP export/import  
- `POST /api/verify`  
- Stable QR schema  
- Tiny OpenAPI / SDK  

**Win:** “Banks integrate without using our UI.”

### B. Vertical wedge
Pick **one** buyer story and own it:
- PSU/BEL vendor clearance + revoke, **or**  
- Campus degree verify, **or**  
- Credit eligibility without full bureau dump  

**Win:** Clear sales story; avoid “national identity” vagueness.

### C. Thin mobile verifier
Expo app: scan QR → hit verify API → green/red.  
**Not** a second full Krydo rewrite.

### D. Dual-chain later
Ethereum sibling already exists; don’t merge day one. Long-term: same VC/VP SDK, different anchors.

### E. What not to do now
- Full UI rewrite  
- Token / multi-chain circus  
- DigiLocker clone  
- Breaking `krydo-stellar` for experiments  
- Building a fat native “everything” app before SDK  

---

## 5. Suggested phased plan (`krydo-next`)

### Phase 1 — Interop core *(highest leverage)*
1. VP model + export/import  
2. Public verify endpoint (VP in → status out, includes revoke check)  
3. Documented QR payload  
4. OpenAPI stub + Postman/curl examples  

### Phase 2 — India-stack friendly
1. Issuer adapter interface (attest-in, no raw PII on-chain)  
2. DPDP / off-chain PII framing in docs  

### Phase 3 — Network & ops
1. Cleaner testnet/mainnet config switching  
2. Fee / scale story (Stellar already cheap — document it)  

### Phase 4 — Thin mobile
1. Verifier-only Expo app on top of Phase 1 API  
2. Holder “prove” sheet only if needed  

### Phase 5 — Depth
1. Compound policies  
2. Audit/revoke timeline UX  
3. Time-bound + replay protection  

*(Same checklist lives in `ROADMAP.md`.)*

---

## 6. Startup / YC lens (brief)

| Bar | Assessment |
|-----|------------|
| SIH / college demo | Strong (live + ZK + contracts) |
| Serious B2B seed | Needs wedge + pilots |
| YC-ready now | No — need usage, not only tech |

**Positioning that works:** selective-disclosure / verify API **on top of** existing ID flows — not “replace DigiLocker.”

---

## 7. Decision waiting on you

Before coding features, choose priority (pick one primary):

1. **Phase 1 interop** (VP + `/verify` + QR)  
2. **Vertical wedge** UX (e.g. vendor clearance flows)  
3. **Mobile verifier** skeleton (only after / parallel to verify API)  
4. **Docs/SDK packaging** only  
5. **Something else** you specify  

Also confirm:
- GitHub visibility: **public** (default, matches stellar) or private?  
- Repo name: **`krydo-next`** OK?  

---

## 8. Bottom line

- **SIH show:** `krydo-stellar` (untouched).  
- **Build future:** `krydo-next` on GitHub.  
- **Best next technical bet:** interop (VP + verify API), then thin app.  
- **Best product bet:** one narrow B2B wedge + real verifier integrations.  

*Report frozen here — implementation starts after your call on priorities.*
