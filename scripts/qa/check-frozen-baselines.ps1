# Verify PHASE0 frozen logic-spec and SQL blueprint hashes.
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

$expected = @{
    "phase18/milk-shop-core-logic-spec-v2.md" = "45e5bddabe8861a1b3df7c5666ac7673078439a89fa5628061ad293a865f9dea"
    "phase18/milk_shop_schema_sql_only_final_v2.sql" = "fb701c90d2dd97a1e6d962154692c48ea0f4e77e9300f9dfac653d4ab67bbc51"
}

$failed = $false
foreach ($rel in $expected.Keys) {
    $path = Join-Path $repoRoot $rel
    if (-not (Test-Path $path)) {
        Write-Host "Missing frozen baseline: $rel"
        $failed = $true
        continue
    }
    # Use the BCL directly so this gate works in Windows PowerShell 5.1 and PowerShell 7.
    $stream = [System.IO.File]::OpenRead($path)
    try {
        $algorithm = [System.Security.Cryptography.SHA256]::Create()
        try {
            $hashBytes = $algorithm.ComputeHash($stream)
        } finally {
            $algorithm.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
    $hash = ([System.BitConverter]::ToString($hashBytes)).Replace("-", "").ToLowerInvariant()
    $want = $expected[$rel].ToLowerInvariant()
    if ($hash -ne $want) {
        Write-Host "Hash drift for $rel"
        Write-Host "  expected: $want"
        Write-Host "  actual:   $hash"
        $failed = $true
    } else {
        Write-Host "OK $rel"
    }
}

if ($failed) {
    Write-Host "FROZEN_BASELINES: FAIL"
    exit 1
}
Write-Host "FROZEN_BASELINES: PASS"
exit 0
