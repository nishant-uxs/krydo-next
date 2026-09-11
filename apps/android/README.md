# Krydo Android (Kotlin)

Native **Kotlin + Jetpack Compose** wallet client for the Krydo presentation protocol.

- Package: `dev.krydo.mobile`
- Deep link: `krydo://present?request=<id>`
- **No Expo / Metro** — install the APK and it runs standalone

## Setup

1. Open `apps/android` in Android Studio, or build from CLI:

```powershell
cd apps/android
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"   # or your JDK 17+
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

2. Open the app → **Settings**
3. Paste your hosted API base URL, e.g. `https://your-service.onrender.com` (no trailing slash required)
4. Tap **Save URL**, then **Test connection (/healthz)**
5. Turn **Use mock data** OFF to hit the live backend; leave ON for offline demo

## Demo flow (works offline)

1. Prove → **Try demo request**
2. Select a credential → **Create DEMO presentation**
3. **Present / Verify** → result screen

Proofs in this build are labeled **DEMO / MOCK PROOF** (not device-only ZK).

## API used

| Method | Path | Auth |
|--------|------|------|
| GET | `/healthz` | public |
| GET | `/api/presentations/request/:id` | public |
| POST | `/api/presentations/verify` | public |

Authenticated `POST /api/presentations/create` is out of scope for this build (needs SIWS/JWT).

## Branding

Launcher icon + in-app logo live under `app/src/main/res/` (`mipmap-*/ic_launcher.png`, `drawable/krydo_logo.png`). Source assets also in `brand/`.

## Live backend (Render)

1. Host `krydo-next` with [`render.yaml`](../../render.yaml) / [`DEPLOY.md`](../../DEPLOY.md).
2. In app **Settings**, paste `https://YOUR-SERVICE.onrender.com`.
3. Tap **Test connection**, then turn **Use mock data** OFF.
4. From repo root (after API key): `.\scripts\install-render-cli.ps1` then `.\scripts\render-deploy.ps1`.

## Note

The older Expo app under `apps/mobile` is **deprecated** in favor of this Kotlin client.
