# Download official Render CLI (Windows amd64) into tools/render-cli/
$ErrorActionPreference = "Stop"
$out = "E:\projects\krydo-next\tools\render-cli"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$rel = Invoke-RestMethod -Uri "https://api.github.com/repos/render-oss/cli/releases/latest" -Headers @{ "User-Agent" = "krydo" }
$asset = $rel.assets | Where-Object { $_.name -match "windows_amd64" } | Select-Object -First 1
if (-not $asset) { throw "No windows_amd64 asset in $($rel.tag_name)" }

$zip = Join-Path $out "render.zip"
Write-Host "Downloading $($asset.name) ($($rel.tag_name))..."
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath $out -Force

$exe = Get-ChildItem $out -Recurse -Filter "render.exe" | Select-Object -First 1
if (-not $exe) {
  # some releases ship as `cli_x.y.z.exe`
  $exe = Get-ChildItem $out -Recurse -Filter "*.exe" | Select-Object -First 1
  if ($exe -and $exe.Name -ne "render.exe") {
    Copy-Item $exe.FullName (Join-Path $out "render.exe") -Force
    $exe = Get-Item (Join-Path $out "render.exe")
  }
}
if (-not $exe) { throw "render.exe not found after extract" }

Write-Host "Installed: $($exe.FullName)"
& $exe.FullName --version
