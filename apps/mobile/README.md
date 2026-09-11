# Krydo Mobile (v1 foundation) — DEPRECATED

> **Deprecated:** prefer the native Kotlin app in [`apps/android`](../android). This Expo client is kept for reference only.

React Native + Expo TypeScript client for the Krydo presentation protocol.

**Product name:** Krydo  
**Package ID:** `dev.krydo.mobile`  
**Deep link scheme:** `krydo://`

This is an early **mobile wallet foundation**, not a finished cryptographic wallet.

Honest trust boundary:

- Presentation request / VP types come from the existing Krydo backend + `shared/` protocol.
- Proof generation in this build uses **`MockProofProver`** and is always labeled **DEMO / MOCK PROOF**.
- Do **not** claim device-only proving or that the server never sees claims. Real on-device ZK is a future phase.

---

## Requirements

- Node.js 20+ (tested with Node 22)
- npm
- **Physical Android phone** (primary workflow)
- USB cable
- Android **Developer Options** + **USB Debugging**
- Android SDK Platform Tools (`adb` on PATH)
- Expo CLI via `npx` (no global install required)

Optional:

- Android emulator (supported by Expo, but **not** the primary documented path)
- Expo Go (limited; **not** the primary architecture — use a **development build**)

---

## Physical Android device (USB) — primary workflow

### 1. Phone setup

1. Open **Settings → About phone**
2. Tap **Build number** seven times to enable Developer Options
3. Open **Settings → Developer options**
4. Enable **USB debugging**
5. Connect the phone to your computer with a USB cable
6. Accept the **Allow USB debugging** prompt on the phone

### 2. Verify ADB

```bash
adb devices
```

You should see your device as `device` (not `unauthorized`).

If `adb` is missing, install [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) and add them to your PATH.

### 3. Install mobile dependencies

```bash
cd apps/mobile
npm install
```

### 4. Configure API base URL (physical device)

Emulator can use `10.0.2.2` to reach the host machine. A **physical phone cannot**.

Copy env example and set your computer's LAN IP:

```bash
cp .env.example .env
```

```env
EXPO_PUBLIC_API_BASE_URL=http://192.168.x.x:5000
EXPO_PUBLIC_USE_MOCK_DATA=true
```

Keep `EXPO_PUBLIC_USE_MOCK_DATA=true` for offline demo UI. Set `false` only when the Krydo API is reachable from the phone.

### 5. Generate native Android project + development build

This app is prepared for **`expo-dev-client`** (not Expo Go–only):

```bash
npx expo prebuild --platform android
npx expo run:android
```

`expo run:android` installs the **development build** on the USB-connected device and starts Metro.

### 6. Start Metro later (already installed build)

Preferred on USB (forwards Metro through `adb reverse`, no Wi‑Fi needed):

```powershell
.\scripts\usb-metro.ps1
```

Or manually:

```bash
adb reverse tcp:8081 tcp:8081
npx expo start --dev-client --localhost
```

If the Dev Client asks for a URL, use `http://localhost:8081` (with USB reverse) — **not** that URL in Prove/Scan. Prove expects `krydo://present?request=...` (use **Try demo request**).

After adding native Expo modules (e.g. `expo-asset`), rebuild/reinstall the debug APK — JS-only Metro reload is not enough.

---

## Android emulator (secondary)

```bash
npx expo run:android
```

With default `.env.example`, `http://10.0.2.2:5000` points at the host loopback from the emulator.

---

## Expo Go (optional / limited)

```bash
npm run start:go
```

Expo Go may run basic UI, but Krydo is aimed at **development builds** for future native crypto/ZK. Prefer `expo-dev-client`.

---

## Scripts

| Script | Purpose |
|--------|---------|
| `npm start` | Metro with dev client |
| `npm run start:go` | Metro (Expo Go) |
| `npm run android` | Build/install on Android (`expo run:android`) |
| `npm run prebuild` | Generate `android/` native project |
| `npm run typecheck` | TypeScript |
| `npm test` | Vitest unit tests |

---

## Navigation

```text
Home · Credentials · Prove · Scan · Settings
```

Deep link:

```text
krydo://present?request=<requestId>
  → /present?request=<id>
  → /prove/<id>
```

HTTPS fallback (for future App Links):

```text
https://krydo.dev/present?request=<requestId>
```

---

## Architecture

```text
CredentialStore     — credential list/get/save (swappable storage)
VerificationClient  — presentation request/create/verify API
ProofProver         — MockProofProver now; KrydoMobileZkProver later
QRRequestParser     — validates krydo:// / https present URIs
buildPresentation   — reuses shared createPresentation when not mock
```

---

## Testing

```bash
cd apps/mobile
npm test
npm run typecheck
```

---

## Limitations (v1)

- Mock proofs only — not real ZK
- No private-key wallet / SIWS on mobile yet
- QR camera capture is a placeholder (URI paste + parser work)
- Presentation request store on the server is still in-memory (P1 caveat)
- Physical device launch requires local Android SDK + USB device

---

## Do not

- Treat DEMO / MOCK PROOF as cryptographic verification
- Commit secrets or production API keys
- Expect Expo Go to be the long-term wallet runtime
