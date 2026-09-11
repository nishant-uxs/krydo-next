# Connect Krydo Android USB phone to Metro without typing a LAN URL.
# Run from apps/mobile:
#   .\scripts\usb-metro.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Error "adb not found: $adb" }

Write-Host "==> USB device" -ForegroundColor Cyan
& $adb start-server | Out-Null
& $adb devices

Write-Host "==> Reverse Metro port 8081" -ForegroundColor Cyan
& $adb reverse --remove-all 2>$null
& $adb reverse tcp:8081 tcp:8081
& $adb reverse --list

Write-Host "==> Starting Metro (dev-client, localhost)" -ForegroundColor Cyan
Write-Host "On phone: open Krydo, use http://localhost:8081 if asked"
npx expo start --dev-client --localhost
