# Machine inventory / orphan coverage check for native Compose tags.
param(
    [switch]$RequireZeroUntested,
    [string]$InventoryPath = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

if (-not $InventoryPath) {
    $InventoryPath = Join-Path $repoRoot "phase18\qa\native_control_inventory.tsv"
}
$tagsFile = Join-Path $repoRoot "phase18\app\src\main\java\com\saaf\milkshop\nativeui\NativeUiTags.kt"
$androidTestRoot = Join-Path $repoRoot "phase18\app\src\androidTest\java"

if (-not (Test-Path $InventoryPath)) { throw "Missing inventory: $InventoryPath" }
if (-not (Test-Path $tagsFile)) { throw "Missing NativeUiTags: $tagsFile" }

$lines = Get-Content $InventoryPath | Where-Object { $_ -and -not $_.StartsWith("#") }
if ($lines.Count -lt 2) { throw "Inventory has no data rows" }

$header = $lines[0].Split("`t")
$requiredCols = @("screen","tag","workflow_id","risk_class","workflow_test","look_tier","status")
foreach ($col in $requiredCols) {
    if ($header -notcontains $col) { throw "Inventory missing column: $col" }
}
$colIndex = @{}
for ($i = 0; $i -lt $header.Count; $i++) { $colIndex[$header[$i]] = $i }

$rows = @()
$tagSeen = @{}
$dupTags = @()
foreach ($line in $lines | Select-Object -Skip 1) {
    $parts = $line.Split("`t")
    if ($parts.Count -lt $header.Count) { throw "Malformed inventory row: $line" }
    $row = @{
        screen = $parts[$colIndex["screen"]]
        tag = $parts[$colIndex["tag"]]
        workflow_id = $parts[$colIndex["workflow_id"]]
        risk_class = $parts[$colIndex["risk_class"]]
        workflow_test = $parts[$colIndex["workflow_test"]]
        look_tier = $parts[$colIndex["look_tier"]]
        status = $parts[$colIndex["status"]].ToUpperInvariant()
    }
    if ($tagSeen.ContainsKey($row.tag)) { $dupTags += $row.tag } else { $tagSeen[$row.tag] = $true }
    $rows += $row
}

# Static tags from NativeUiTags.kt
$tagSource = Get-Content $tagsFile -Raw
$staticTags = [regex]::Matches($tagSource, 'const val \w+\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique

# Dynamic prefix families declared by factory helpers (not expected as literal inventory rows).
$dynamicPrefixes = @(
    "auth_account_",
    "cashier_product_",
    "cashier_customer_",
    "collect_customer_",
    "collect_payment_account_",
    "ops_receive_milk_vendor_",
    "ops_record_spoilage_product_",
    "ops_record_spoilage_reason_"
)

$orphanTags = @()
foreach ($tag in $staticTags) {
    $isDynamicLiteral = $false
    foreach ($prefix in $dynamicPrefixes) {
        # Keep legacy concrete customer aliases inventoried; skip only pure factory-only forms.
        if ($tag.StartsWith($prefix) -and $tag -ne "cashier_customer_ahmed" -and $tag -ne "cashier_customer_bilal") {
            # factory returns may also appear as const for milk/yogurt — those are static.
        }
    }
    if (-not $tagSeen.ContainsKey($tag)) {
        $orphanTags += $tag
    }
}

# Duplicate stable tags in source
$literalDupes = @()
$literalCounts = @{}
foreach ($tag in $staticTags) { } # uniqueness already enforced by Sort-Object -Unique on const values
$valueMatches = [regex]::Matches($tagSource, '=\s*"([^"]+)"')
$valueCounts = @{}
foreach ($m in $valueMatches) {
    $v = $m.Groups[1].Value
    if ($valueCounts.ContainsKey($v)) { $valueCounts[$v]++ } else { $valueCounts[$v] = 1 }
}
foreach ($k in $valueCounts.Keys) {
    if ($valueCounts[$k] -gt 1) { $literalDupes += $k }
}

# Missing claimed workflow test identifiers in androidTest sources
$testCorpus = Get-ChildItem -Path $androidTestRoot -Recurse -Filter "*.kt" | ForEach-Object { Get-Content $_.FullName -Raw } | Out-String
$missingTests = @()
foreach ($row in $rows) {
    if (-not $row.workflow_test -or $row.workflow_test -eq "-" -or $row.workflow_test -eq "PREVIEW") { continue }
    $ids = $row.workflow_test.Split(";") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    foreach ($id in $ids) {
        if ($testCorpus -notmatch [regex]::Escape($id)) {
            $missingTests += "$($row.tag) -> $id"
        }
    }
}

# Stale inventory: tag not present in NativeUiTags static consts and not a dynamic prefix family marker
$stale = @()
foreach ($row in $rows) {
    $tag = $row.tag
    if ($staticTags -contains $tag) { continue }
    if ($tag -match '^.+_\*$') { continue }
    $stale += $tag
}

$untested = @($rows | Where-Object { $_.status -eq "UNTESTED" })

$failed = $false
if ($dupTags.Count -gt 0) {
    Write-Host "Duplicate inventory tags: $($dupTags -join ', ')"
    $failed = $true
}
if ($literalDupes.Count -gt 0) {
    Write-Host "Duplicate NativeUiTags string literals: $($literalDupes -join ', ')"
    $failed = $true
}
if ($orphanTags.Count -gt 0) {
    Write-Host "NativeUiTags without inventory rows:"
    $orphanTags | ForEach-Object { Write-Host "  - $_" }
    $failed = $true
}
if ($missingTests.Count -gt 0) {
    Write-Host "Inventory claims missing workflow tests:"
    $missingTests | ForEach-Object { Write-Host "  - $_" }
    $failed = $true
}
if ($stale.Count -gt 0) {
    Write-Host "Stale inventory tags not found in NativeUiTags:"
    $stale | ForEach-Object { Write-Host "  - $_" }
    $failed = $true
}

Write-Host ("inventory rows={0} untested={1} orphans={2}" -f $rows.Count, $untested.Count, $orphanTags.Count)

if ($RequireZeroUntested -and $untested.Count -gt 0) {
    Write-Host "Checkpoint requires untested=0; still untested:"
    $untested | ForEach-Object { Write-Host ("  - [{0}] {1} ({2})" -f $_.screen, $_.tag, $_.workflow_id) }
    $failed = $true
}

if ($failed) {
    Write-Host "NATIVE_INVENTORY: FAIL"
    exit 1
}
Write-Host "NATIVE_INVENTORY: PASS"
Write-Host ("UNTESTED_COUNT={0}" -f $untested.Count)
exit 0
