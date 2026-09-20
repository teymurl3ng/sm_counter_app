# Pull native Compose evidence PNGs / review sidecars for the *current* checkpoint run.
# Writes only under phase14-evidence/checkpoint-current/ and a run.json manifest.
# Does not treat unrelated historical PNGs under phase14-evidence as this run's evidence.
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

$currentRoot = Join-Path $HostRoot "checkpoint-current"
if (Test-Path -LiteralPath $currentRoot) {
    Remove-Item -LiteralPath $currentRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $currentRoot | Out-Null

$pulledPngs = New-Object System.Collections.Generic.List[string]
$pulledReviews = New-Object System.Collections.Generic.List[string]
$pulledScreens = New-Object System.Collections.Generic.List[string]

foreach ($name in $screens) {
    $deviceDir = "$deviceRoot/native-$name-a06"
    $listing = & $adb -s $Serial shell "ls `"$deviceDir`" 2>/dev/null"
    if (-not $listing -or ($listing -match "No such file")) {
        Write-Host "No device evidence yet for $name ($deviceDir)"
        continue
    }

    $hostScreenDir = Join-Path $currentRoot "native-$name-a06"
    New-Item -ItemType Directory -Force -Path $hostScreenDir | Out-Null
    & $adb -s $Serial pull $deviceDir $currentRoot
    if ($LASTEXITCODE -ne 0) { throw "adb pull failed for $deviceDir" }

    $pngFiles = @(Get-ChildItem -Path $hostScreenDir -Filter *.png -File -ErrorAction SilentlyContinue)
    $reviewFiles = @(Get-ChildItem -Path $hostScreenDir -Filter *.review.json -File -ErrorAction SilentlyContinue)
    if ($pngFiles.Count -gt 0) {
        $pulledScreens.Add($name) | Out-Null
    }
    foreach ($png in $pngFiles) { $pulledPngs.Add($png.Name) | Out-Null }
    foreach ($rev in $reviewFiles) { $pulledReviews.Add($rev.Name) | Out-Null }
}

$run = [ordered]@{
    pulledAt = (Get-Date).ToString("o")
    serial = $Serial
    screens = @($pulledScreens)
    pngCount = $pulledPngs.Count
    reviewSidecarCount = $pulledReviews.Count
    pngs = @($pulledPngs)
    reviewSidecars = @($pulledReviews)
    source = "device:$deviceRoot"
}
$run | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $currentRoot "run.json") -Encoding UTF8

Write-Host ("Pulled PNG count (current run): {0}" -f $pulledPngs.Count)
Write-Host ("Pulled review sidecar count (current run): {0}" -f $pulledReviews.Count)
Write-Host "EVIDENCE_PULL: PASS"
exit 0
