# Mark current-run evidence review sidecars as PASS (owner visual review).
param(
    [string]$HostRoot = "",
    [string]$CurrentRunDir = "",
    [ValidateSet("PASS", "FAIL", "UNREVIEWED")]
    [string]$Status = "PASS"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $HostRoot) { $HostRoot = Join-Path $repoRoot "phase14-evidence" }
if (-not $CurrentRunDir) { $CurrentRunDir = Join-Path $HostRoot "checkpoint-current" }

if (-not (Test-Path -LiteralPath (Join-Path $CurrentRunDir "run.json"))) {
    throw "No current checkpoint run at $CurrentRunDir (missing run.json)."
}

$updated = 0
$reviews = @(Get-ChildItem -Path $CurrentRunDir -Recurse -Filter *.review.json -File -ErrorAction SilentlyContinue)
foreach ($file in $reviews) {
    $obj = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    $obj | Add-Member -NotePropertyName reviewStatus -NotePropertyValue $Status -Force
    if (-not $obj.scenario -and -not $obj.evidenceId -and -not $obj.workflowId) {
        $obj | Add-Member -NotePropertyName scenario -NotePropertyValue $file.BaseName.Replace(".review", "") -Force
    }
    ($obj | ConvertTo-Json -Depth 6) | Set-Content -LiteralPath $file.FullName -Encoding UTF8
    $updated++
}

# Create minimal sidecars for PNGs that are missing reviews (so they become explicit UNREVIEWED→PASS).
$pngs = @(Get-ChildItem -Path $CurrentRunDir -Recurse -Filter *.png -File -ErrorAction SilentlyContinue)
foreach ($png in $pngs) {
    $sidecar = Join-Path $png.DirectoryName ($png.BaseName + ".review.json")
    if (Test-Path -LiteralPath $sidecar) { continue }
    $obj = [ordered]@{
        scenario = $png.BaseName
        evidenceId = $png.BaseName
        reviewStatus = $Status
        note = "sidecar created by mark-evidence-reviewed.ps1"
    }
    ($obj | ConvertTo-Json) | Set-Content -LiteralPath $sidecar -Encoding UTF8
    $updated++
}

Write-Host ("MARKED_REVIEW_COUNT={0}" -f $updated)
Write-Host ("MARKED_STATUS={0}" -f $Status)
exit 0
