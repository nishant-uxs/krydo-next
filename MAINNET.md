# Krydo — Mainnet migration guide

Step-by-step checklist to move Krydo from Stellar **Testnet** to **Mainnet** (public network).

> **Cost:** expect **~10–25 XLM** for deploying all three Soroban contracts plus a small buffer for the first issuer/credential txs. Per user action is typically **~0.02–0.1 XLM**. See [Stellar fees docs](https://developers.stellar.org/docs/learn/fundamentals/fees-resource-limits-metering).

---

## Before you start

| Item | Notes |
|------|--------|
| **New mainnet deployer** | Use a **separate** `G...` / `S...` key from testnet. Never reuse testnet secrets on mainnet. |
| **Fund the account** | Send **25–50 XLM** from an exchange or another wallet to the deployer `G...` address. |
| **Wallet on mainnet** | Freighter / xBull / Lobstr → switch network to **Mainnet** before Connect Wallet. |
| **Firestore** | Existing rows reference **testnet** tx hashes. For a clean mainnet launch, use a new Firebase project or purge old chain data. |
| **Security** | Mainnet = real money. Consider multi-sig root and an external audit before production financial use. |

Testnet deployment is backed up in [`contracts/deployment.testnet.json`](./contracts/deployment.testnet.json).

---

## 1. Install toolchain

```bash
# Rust + Stellar CLI — see https://developers.stellar.org/docs/tools/cli
rustc --version
stellar --version
```

---

## 2. Create & fund mainnet deployer

```bash
stellar keys generate deployer-mainnet --network mainnet
stellar keys address deployer-mainnet
# Fund the printed G... address with 25–50 XLM (exchange withdrawal, Lobstr, etc.)

stellar keys secret deployer-mainnet   # S... — keep private, never commit
```

---

## 3. Build contracts

```bash
npm install
npm run compile:contracts
```

WASM output: `contracts/target/wasm32v1-none/release/*.wasm`

---

## 4. Deploy to mainnet

```bash
# Windows (PowerShell)
$env:STELLAR_NETWORK="mainnet"
$env:DEPLOYER_SECRET="S..."   # mainnet secret only
npm run deploy:mainnet

# macOS / Linux
STELLAR_NETWORK=mainnet DEPLOYER_SECRET=S... npm run deploy:mainnet
```

This writes **`contracts/deployment.json`** with:

- `network: "mainnet"`
- Mainnet RPC + explorer URLs
- New `C...` contract IDs
- Your deployer as root `G...`

Verify on [Stellar Expert (public)](https://stellar.expert/explorer/public).

---

## 5. Bootstrap issuers (required)

With **no issuers**, holders cannot request credentials. As root:

1. Open the app → **Connect Wallet** (mainnet) → sign in as deployer `G...`
2. **Manage Issuers** → add at least one demo issuer (`add_issuer` wallet tx)
3. Optionally issue a sample credential for demo flows

---

## 6. Update Vercel (production)

In Vercel → **Settings → Environment Variables**:

| Variable | Mainnet value |
|----------|----------------|
| `STELLAR_NETWORK` | `mainnet` |
| `SOROBAN_RPC_URL` | `https://mainnet.sorobanrpc.com` (or your paid RPC) |
| `DEPLOYER_SECRET` | Mainnet `S...` (root identity + RPC reads) |
| `CORS_ORIGINS` | `https://krydo-stellar.vercel.app` (+ custom domain) |
| Firebase / JWT / secrets | unchanged |

Then redeploy:

```bash
npm run build
git add contracts/deployment.json
git commit -m "Deploy Krydo contracts to Stellar mainnet."
git push
# Vercel auto-deploys on push, or: vercel --prod
```

`contracts/deployment.json` is **baked into the client bundle** at build time — always rebuild after deploy.

Generate a local env template:

```bash
DEPLOYER_SECRET=S... npx tsx script/print-vercel-env.ts > vercel-env.mainnet.txt
```

---

## 7. Smoke test

| Check | Expected |
|-------|----------|
| `GET /api/network` | `"network": "mainnet"`, real `C...` ids |
| `GET /healthz` | `{"ok":true}` |
| Connect wallet (mainnet) | SIWS succeeds |
| Root adds issuer | Real mainnet tx on stellar.expert |
| Issue credential | Wallet-signed tx, explorer link works |
| ZK generate / verify | Off-chain OK; optional anchor on mainnet |

---

## 8. Roll back to testnet (dev)

```bash
cp contracts/deployment.testnet.json contracts/deployment.json
# Vercel: STELLAR_NETWORK=testnet, testnet DEPLOYER_SECRET, testnet RPC
npm run build
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `insufficient balance` on deploy | Add more XLM to deployer |
| Wallet wrong network | Switch wallet to Mainnet; passphrase must match `Public Global Stellar Network ; September 2015` |
| Root shows as User | Set `DEPLOYER_SECRET` on Vercel; redeploy; reconnect wallet |
| Old testnet txs in UI | Firestore still has testnet rows — clean or use fresh project |
| RPC timeouts | Use a dedicated Soroban RPC provider |

---

## Related docs

- [`DEPLOY.md`](./DEPLOY.md) — Firebase, indexes, hosting
- [`README.md`](./README.md) — architecture overview
- [`DOCUMENTATION.md`](./DOCUMENTATION.md) — full technical spec
