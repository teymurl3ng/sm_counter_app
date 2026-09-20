# Protected-file / UI-only guard for native UI work.
# Fails when protected business/domain/SQL files change unless -AllowLogic / -Pallow.logic=true.
param(
    [switch]$AllowLogic,
    [string]$BaseRef = "HEAD"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

$protectedPrefixes = @(
    "phase18/milk-shop-core-logic-spec-v2.md",
    "phase18/milk_shop_schema_sql_only_final_v2.sql",
    "phase18/app/src/main/java/com/saaf/milkshop/domain/",
    "phase18/app/src/main/java/com/saaf/milkshop/data/",
    "phase18/app/schemas/",
    "bridge/contract.json",
    "bridge/CONTRACT.md",
    "scripts/verify-bridge-contract.py"
)

function Normalize-RepoPath([string]$path) {
    return ($path -replace "\\", "/").Trim().TrimStart("./")
}

function Is-Protected([string]$file) {
    $n = Normalize-RepoPath $file
    foreach ($prefix in $protectedPrefixes) {
        if ($n -eq $prefix -or $n.StartsWith($prefix)) { return $true }
    }
    return $false
}

$names = New-Object System.Collections.Generic.HashSet[string]
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$gitOut = & git -c core.safecrlf=false diff --name-only $BaseRef -- 2>$null
$gitCached = & git -c core.safecrlf=false diff --name-only --cached -- 2>$null
$gitOther = & git -c core.safecrlf=false ls-files --others --exclude-standard 2>$null
$ErrorActionPreference = $prevEap
$gitOut | ForEach-Object { if ($_) { [void]$names.Add((Normalize-RepoPath $_)) } }
$gitCached | ForEach-Object { if ($_) { [void]$names.Add((Normalize-RepoPath $_)) } }
$gitOther | ForEach-Object { if ($_) { [void]$names.Add((Normalize-RepoPath $_)) } }

$changed = @($names | Where-Object { $_ -and (Is-Protected $_) } | Sort-Object)

if ($changed.Count -eq 0) {
    Write-Host "checkUiOnly: no protected logic files changed"
    Write-Host "CHECK_UI_ONLY: PASS"
    exit 0
}

if ($AllowLogic) {
    Write-Host ""
    Write-Host "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    Write-Host "LOGIC EXCEPTION ACTIVE"
    Write-Host "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    Write-Host "Protected files changed with allow.logic override:"
    $changed | ForEach-Object { Write-Host "  - $_" }
    Write-Host "CHECK_UI_ONLY: PASS_WITH_LOGIC_EXCEPTION"
    exit 0
}

Write-Host "Protected business/domain/SQL files were modified during UI work:"
$changed | ForEach-Object { Write-Host "  - $_" }
Write-Host "Re-run with -AllowLogic / -Pallow.logic=true only with owner approval."
Write-Host "CHECK_UI_ONLY: FAIL"
exit 1
