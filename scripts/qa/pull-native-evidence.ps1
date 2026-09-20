# Pull native Compose evidence PNGs / review manifests from the device of record.
param(
    [string]$Serial = "R8VXA02SNKH",
    [string]$Screen = "all",
    [string]$HostRoot = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $HostRoot) {
    $HostRoot = Join-Path $repoRoot "phase14-evidence"
}

$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "adb" }
if ($env:ANDROID_SERIAL) { $Serial = $env:ANDROID_SERIAL }

$deviceRoot = "/sdcard/Download/phase14-native-evidence"
$screens = if ($Screen -eq "all") { @("auth", "sale", "collect") } else { @($Screen.ToLowerInvariant()) }

New-Item -ItemType Directory -Force -Path $HostRoot | Out-Null
$pulled = 0
foreach ($name in $screens) {
    $deviceDir = "$deviceRoot/native-$name-a06"
    $hostDir = Join-Path $HostRoot "native-$name-a06"
    New-Item -ItemType Directory -Force -Path $hostDir | Out-Null
    $listing = & $adb -s $Serial shell "ls $deviceDir 2>/dev/null"
    if (-not $listing) {
        Write-Host "No device evidence yet for $name ($deviceDir)"
        continue
    }
    & $adb -s $Serial pull $deviceDir $HostRoot
    if ($LASTEXITCODE -ne 0) { throw "adb pull failed for $deviceDir" }
    $pulled += @(Get-ChildItem -Path $hostDir -Filter *.png -ErrorAction SilentlyContinue).Count
}

Write-Host "Pulled PNG count (host): $pulled"
Write-Host "EVIDENCE_PULL: PASS"
exit 0
