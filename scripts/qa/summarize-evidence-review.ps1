# Summarize review sidecars under phase14-evidence/native-*-a06/
param(
    [string]$HostRoot = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $HostRoot) { $HostRoot = Join-Path $repoRoot "phase14-evidence" }

$pngs = @(Get-ChildItem -Path $HostRoot -Recurse -Filter *.png -ErrorAction SilentlyContinue)
$reviews = @(Get-ChildItem -Path $HostRoot -Recurse -Filter *.review.json -ErrorAction SilentlyContinue)
$unreviewed = 0
foreach ($review in $reviews) {
    $json = Get-Content $review.FullName -Raw | ConvertFrom-Json
    if ($json.reviewStatus -eq "UNREVIEWED") { $unreviewed++ }
}

Write-Host ("SCREENSHOT_COUNT={0}" -f $pngs.Count)
Write-Host ("REVIEW_SIDECAR_COUNT={0}" -f $reviews.Count)
Write-Host ("UNREVIEWED_COUNT={0}" -f $unreviewed)
if ($unreviewed -gt 0) {
    Write-Host "EVIDENCE_REVIEW: UNREVIEWED_PRESENT"
} else {
    Write-Host "EVIDENCE_REVIEW: CLEAR"
}
exit 0
