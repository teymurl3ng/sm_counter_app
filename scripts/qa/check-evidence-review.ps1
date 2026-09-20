# Account visual evidence for the current checkpoint run only.
# Scopes to phase14-evidence/checkpoint-current/ (plus optional retained-baselines/).
# Missing or invalid review sidecar => UNREVIEWED.
# Stale PNGs elsewhere under phase14-evidence are ignored.
param(
    [string]$HostRoot = "",
    [string]$CurrentRunDir = "",
    [string]$RetainedDir = "",
    [switch]$RequireReviewed
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $HostRoot) { $HostRoot = Join-Path $repoRoot "phase14-evidence" }
if (-not $CurrentRunDir) { $CurrentRunDir = Join-Path $HostRoot "checkpoint-current" }
if (-not $RetainedDir) { $RetainedDir = Join-Path $HostRoot "retained-baselines" }

function Get-EvidenceId {
    param($Review, [string]$PngStem)
    if ($null -eq $Review) { return $null }
    foreach ($key in @("scenario", "evidenceId", "evidence_id", "workflowId", "workflow_id")) {
        $value = $Review.$key
        if ($value -and "$value".Trim().Length -gt 0) { return "$value".Trim() }
    }
    return $null
}

$roots = @()
$runMeta = Join-Path $CurrentRunDir "run.json"
$hasCurrentRun = Test-Path -LiteralPath $runMeta
if ($hasCurrentRun) { $roots += $CurrentRunDir }
if (Test-Path -LiteralPath $RetainedDir) { $roots += $RetainedDir }

$pngs = @()
foreach ($root in $roots) {
    $pngs += @(Get-ChildItem -Path $root -Recurse -Filter *.png -File -ErrorAction SilentlyContinue)
}

$evidence = 0
$unreviewed = 0
$pass = 0
$fail = 0
$missingSidecar = 0
$details = New-Object System.Collections.Generic.List[string]

foreach ($png in $pngs) {
    $evidence++
    $sidecarPath = Join-Path $png.DirectoryName ($png.BaseName + ".review.json")
    if (-not (Test-Path -LiteralPath $sidecarPath)) {
        $missingSidecar++
        $unreviewed++
        $details.Add(("MISSING_SIDECAR`t{0}" -f $png.FullName)) | Out-Null
        continue
    }

    try {
        $reviewObj = Get-Content -LiteralPath $sidecarPath -Raw | ConvertFrom-Json
    } catch {
        $missingSidecar++
        $unreviewed++
        $details.Add(("INVALID_SIDECAR`t{0}" -f $sidecarPath)) | Out-Null
        continue
    }

    $id = Get-EvidenceId -Review $reviewObj -PngStem $png.BaseName
    $rawStatus = if ($null -eq $reviewObj.reviewStatus) { "" } else { "$($reviewObj.reviewStatus)".Trim() }
    if (-not $id -or -not $rawStatus) {
        $unreviewed++
        $details.Add(("INCOMPLETE_SIDECAR`t{0}" -f $sidecarPath)) | Out-Null
        continue
    }

    switch ($rawStatus.ToUpperInvariant()) {
        "PASS" { $pass++ }
        "FAIL" {
            $fail++
            $details.Add(("FAIL`t{0}`t{1}" -f $id, $sidecarPath)) | Out-Null
        }
        "UNREVIEWED" {
            $unreviewed++
            $details.Add(("UNREVIEWED`t{0}`t{1}" -f $id, $sidecarPath)) | Out-Null
        }
        default {
            $unreviewed++
            $details.Add(("UNKNOWN_STATUS`t{0}`t{1}" -f $rawStatus, $sidecarPath)) | Out-Null
        }
    }
}

$visual = if ($evidence -eq 0) {
    "NONE"
} elseif ($unreviewed -gt 0 -or $fail -gt 0) {
    "BLOCKED"
} else {
    "PASS"
}

$summary = [ordered]@{
    currentRunPresent = $hasCurrentRun
    evidence = $evidence
    unreviewed = $unreviewed
    reviewedPass = $pass
    reviewedFail = $fail
    missingSidecar = $missingSidecar
    visualAcceptance = $visual
}
New-Item -ItemType Directory -Force -Path $HostRoot | Out-Null
$summaryPath = Join-Path $HostRoot "checkpoint-evidence-summary.json"
($summary | ConvertTo-Json) | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host ("EVIDENCE_COUNT={0}" -f $evidence)
Write-Host ("UNREVIEWED_COUNT={0}" -f $unreviewed)
Write-Host ("REVIEWED_PASS_COUNT={0}" -f $pass)
Write-Host ("REVIEWED_FAIL_COUNT={0}" -f $fail)
Write-Host ("MISSING_SIDECAR_COUNT={0}" -f $missingSidecar)
Write-Host ("CURRENT_RUN_PRESENT={0}" -f ($(if ($hasCurrentRun) { "YES" } else { "NO" })))
Write-Host ("VISUAL_ACCEPTANCE={0}" -f $visual)
foreach ($line in $details) { Write-Host $line }

if ($visual -eq "NONE" -and -not $hasCurrentRun) {
    Write-Host "EVIDENCE_REVIEW: NO_CURRENT_RUN (stale host PNGs outside checkpoint-current are ignored)"
} elseif ($visual -eq "NONE") {
    Write-Host "EVIDENCE_REVIEW: NO_EVIDENCE_THIS_RUN"
} elseif ($visual -eq "BLOCKED") {
    Write-Host "EVIDENCE_REVIEW: BLOCKED"
} else {
    Write-Host "EVIDENCE_REVIEW: PASS"
}

if ($RequireReviewed -and $visual -eq "BLOCKED") {
    Write-Host "Visual acceptance blocked: unreviewed or FAIL evidence remains."
    exit 1
}

exit 0
