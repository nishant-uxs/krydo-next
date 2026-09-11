# Krydo — Render CLI deploy helper (Windows)
#
# Install CLI once:
#   .\scripts\install-render-cli.ps1
#
# Auth:
#   $env:RENDER_API_KEY = "rnd_..."   # Dashboard → Account Settings → API Keys
#
# Required secrets for a real backend (set in Render dashboard Env):
#   FIREBASE_SERVICE_ACCOUNT, FIREBASE_PROJECT_ID, DEPLOYER_SECRET, CORS_ORIGINS
#
# Usage:
#   cd E:\projects\krydo-next
#   $env:RENDER_API_KEY="rnd_..."
#   .\scripts\render-deploy.ps1

$ErrorActionPreference = "Stop"
$cli = "E:\projects\krydo-next\tools\render-cli\render.exe"
if (-not (Test-Path $cli)) {
  $cmd = Get-Command render -ErrorAction SilentlyContinue
  if ($cmd) { $cli = $cmd.Source }
}
if (-not $cli -or -not (Test-Path $cli)) {
  Write-Error "Render CLI not found. Run .\scripts\install-render-cli.ps1 first."
}

if (-not $env:RENDER_API_KEY) {
  Write-Error "Set RENDER_API_KEY (Dashboard → Account Settings → API Keys)."
}

Write-Host "==> Render whoami"
& $cli whoami

Write-Host "==> Services"
& $cli services -o json

if ($env:RENDER_SERVICE_ID) {
  Write-Host "==> Deploy $($env:RENDER_SERVICE_ID)"
  & $cli deploys create $env:RENDER_SERVICE_ID --wait --confirm
} else {
  Write-Host @"

No RENDER_SERVICE_ID set.
1. Push krydo-next main (with render.yaml + contracts/deployment.json).
2. Dashboard → New → Blueprint → apply render.yaml OR create web service.
3. Fill secrets, then:
   `$env:RENDER_SERVICE_ID = 'srv-...'`
   .\scripts\render-deploy.ps1
4. curl.exe https://YOUR.onrender.com/healthz
5. Android Settings → paste URL → mock OFF → Test connection

"@
}
