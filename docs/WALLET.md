# Multi-chain wallets (Krydo)

Krydo supports **Stellar** (primary / Soroban) and **EVM** (auth + identity layer) on web and Android.

## Auth rails

| Chain | Connect SDK | Sign-in | JWT `chain` claim |
|-------|-------------|---------|-------------------|
| Stellar | [@creit.tech/stellar-wallets-kit](https://github.com/Creit-Tech/Stellar-Wallets-Kit) | SIWS — `GET /api/auth/nonce`, `POST /api/auth/verify` | `stellar` |
| EVM | [Reown AppKit](https://docs.reown.com/appkit/overview) | SIWE — `GET /api/auth/siwe/nonce`, `POST /api/auth/siwe/verify` | `eip155:<id>` |

EVM holders are **not** Soroban subjects. Credential issuance remains Stellar `G…` addresses only. There is no silent identity merge between rails.

## Supported EVM networks (wired)

Ethereum (1), Polygon (137), Base (8453), Arbitrum (42161), Optimism (10), BNB (56), Avalanche (43114).

Solana and other non-EVM rails are out of scope.

## Environment

### Web

```bash
# .env / Vite
VITE_REOWN_PROJECT_ID=<from https://dashboard.reown.com>
```

Without this, the Connect dialog still offers Stellar; EVM is disabled with a clear label.

### Android

```properties
# apps/android/local.properties
reown.projectId=<same Reown project id>
```

Build injects `BuildConfig.REOWN_PROJECT_ID`. Empty → AppKit stays off; login still works via Stellar JWT paste.

### Server

SIWE domain/URI are derived from the request `Host` / `X-Forwarded-*` headers. Nonces are **in-memory** (same store as SIWS) — not shared across multi-instance deploys unless you add Redis later.

## Android login gate

`LoginScreen` (Stitch Identity Core UI) is the gate until `authToken` + `holderAddress` are set. Deep links (`krydo://present?request=…`) wait for a session. Settings is advanced (API URL, JWT paste fallback, sign-out).

## Web Connect UI

`WalletButton` opens a dialog: **Connect Stellar** (SIWS) and **Connect EVM** (AppKit + SIWE when configured).

## Limitations (v1)

- No native Stellar SIWS on Android yet (paste web JWT + `G…`).
- No Soroban ops for `0x` addresses.
- No account-linking merge UI beyond listing linked accounts in local storage.
- In-memory nonces: horizontal scale needs a shared store.
- Reown project id required for real EVM connect (no mock wallets).

## Deploy checklist

1. Set `VITE_REOWN_PROJECT_ID` on the web build host.
2. Set `reown.projectId` for Android release builds.
3. Allow WalletConnect / AppKit domains in CSP if you tighten headers.
4. Keep SIWS paths unchanged for existing Stellar users.
