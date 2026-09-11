# Run Krydo mobile on USB-connected Android phone.
# Usage (PowerShell):
#   cd E:\projects\krydo-next\apps\mobile
#   .\scripts\run-android-device.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
  Write-Error "adb not found at $adb. Install Android SDK Platform-Tools."
}

Write-Host "==> Checking phone..." -ForegroundColor Cyan
& $adb start-server | Out-Null
$devices = & $adb devices | Select-String "`tdevice$"
if (-not $devices) {
  Write-Host "Phone not detected as 'device'." -ForegroundColor Red
  Write-Host "1) Unlock phone"
  Write-Host "2) USB mode = File transfer"
  Write-Host "3) Allow USB debugging popup"
  Write-Host "4) Run: & `"$adb`" devices"
  exit 1
}
Write-Host "Phone OK:" $devices -ForegroundColor Green

$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Set-Content -Path (Join-Path $root "android\local.properties") -Value ("sdk.dir=" + ($sdk -replace '\\','/'))

# Prefer only arm64 for modern phones (faster first build)
$env:ORG_GRADLE_PROJECT_reactNativeArchitectures = "arm64-v8a"

Write-Host "==> Building + installing Krydo (first build can take 10-20 min)..." -ForegroundColor Cyan
npx expo run:android

Write-Host "==> Done. If Metro didn't start, run: npm start" -ForegroundColor Green
