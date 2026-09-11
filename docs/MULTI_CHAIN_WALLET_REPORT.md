# Multi-chain wallet — implementation report (krydo-next)

Date: 2026-09-12

## Status

| Area | Status |
|------|--------|
| Android login gate (Stitch) | Done — session = JWT + holder |
| Shared `ChainType` / `WalletAccount` + JWT `chain` | Done |
| Web Reown AppKit + SIWE | Done (needs `VITE_REOWN_PROJECT_ID`) |
| Android Reown AppKit + SIWE | Done (needs `reown.projectId`) |
| Docs + tests | Done |

## SDKs

- **Stellar (unchanged):** `@creit.tech/stellar-wallets-kit` + SIWS
- **EVM web:** `@reown/appkit@1.6.9`, `@reown/appkit-adapter-wagmi@1.6.9`, `wagmi@2.14.16`, `viem@2.21.54`, `siwe@2.3.2`, `ethers@6` (siwe peer)
- **EVM Android:** `com.reown:android-bom:1.4.11` (`android-core` + `appkit`); Kotlin plugin **2.2.0** (required for Reown metadata)

## Networks (EVM)

Ethereum, Polygon, Base, Arbitrum, Optimism, BNB, Avalanche (`shared/wallet.ts` + AppKit networks).

## Key files

- `shared/wallet.ts` — CAIP helpers / chain ids
- `server/auth/siwe.ts` — `/api/auth/siwe/nonce`, `/api/auth/siwe/verify`
- `server/auth/jwt.ts` — optional `chain` claim
- `client/src/lib/reown.ts`, `evm-auth.ts`, `evm-providers.tsx`, `wallet.tsx`
- `client/src/components/wallet-button.tsx`, `evm-connect-actions.tsx`
- Android: `LoginScreen`, `KrydoRoot` gate, `wallet/*` (AppKit + SIWE)
- `docs/WALLET.md`

## Env

| Var | Where |
|-----|--------|
| `VITE_REOWN_PROJECT_ID` | Web build |
| `reown.projectId` | `apps/android/local.properties` → `BuildConfig.REOWN_PROJECT_ID` |

## Limitations

- EVM is auth/identity only — Soroban issuance stays Stellar `G…`
- No silent identity merge / no Solana
- Android Stellar: paste SIWS JWT from web (no native SIWS yet)
- Nonces in-memory (not multi-instance safe)
- Without Reown project id, EVM connect is disabled (no mocks)

## Verify

```bash
npm test -- server/auth/siwe.test.ts shared/wallet.test.ts
npm run check
cd apps/android && ./gradlew :app:assembleDebug
```

Verified locally (2026-09-12): `tsc` OK, SIWE/wallet tests 7/7, `assembleDebug` SUCCESS.