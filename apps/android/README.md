# Krydo Android (Kotlin)

Native **Kotlin + Jetpack Compose** wallet client for the live Krydo API.

- Package: `dev.krydo.mobile`
- Deep link: `krydo://present?request=<uuid>`
- Default API: `https://krydo.onrender.com`
- **No Expo / Metro / mock demo mode**

## Setup

1. Build & install:

```powershell
cd apps/android
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

2. Open **Settings**
3. Confirm API URL `https://krydo.onrender.com`
4. Paste your Stellar holder address (`G…`)
5. Sign in on the web app with Freighter (SIWS) and paste the JWT
6. Save → Credentials → Refresh

## Live prove flow

1. Verifier creates a presentation request (web / API)
2. Open `krydo://present?request=<uuid>` or paste the id in Prove
3. Select a real credential → Create presentation → Verify

## API used

| Method | Path | Auth |
|--------|------|------|
| GET | `/healthz` | public |
| GET | `/api/credentials/:address` | JWT (self) |
| GET | `/api/presentations/request/:id` | public |
| POST | `/api/presentations/create` | JWT (holder) |
| POST | `/api/presentations/verify` | public |

## Branding

Launcher icon + in-app logo under `app/src/main/res/` and `brand/`.
