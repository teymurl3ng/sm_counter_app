# Host-side golden-ledger contract: ensure the instrumented regression exists,
# calls production services, and locks the captured expectation file.
param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$testFile = Join-Path $repoRoot "phase18\app\src\androidTest\java\com\saaf\milkshop\qa\NativeGoldenLedgerRegressionTest.kt"
$expectations = Join-Path $repoRoot "phase18\qa\golden_ledger_expectations.properties"

if (-not (Test-Path $testFile)) { throw "Missing golden ledger test: $testFile" }
if (-not (Test-Path $expectations)) { throw "Missing golden expectations: $expectations" }

$source = Get-Content $testFile -Raw
$requiredCalls = @(
    "SaleService",
    "CreditCollectionService",
    "CorrectionService",
    "ExpenseService",
    "VendorPurchaseService",
    "GoldenLedgerExpectations"
)
foreach ($name in $requiredCalls) {
    if ($source -notmatch [regex]::Escape($name)) {
        Write-Host "Golden ledger test no longer calls/uses $name"
        Write-Host "GOLDEN_LEDGER_CONTRACT: FAIL"
        exit 1
    }
}

$props = @{}
Get-Content $expectations | Where-Object { $_ -and -not $_.StartsWith("#") } | ForEach-Object {
    $pair = $_.Split("=", 2)
    $props[$pair[0].Trim()] = $pair[1].Trim()
}
foreach ($key in @("milk_stock_after","customer_balance_after","cash_balance_after","fuel_expense_balance_after","first_cash_sale_total")) {
    if (-not $props.ContainsKey($key)) {
        Write-Host "Missing expectation key: $key"
        Write-Host "GOLDEN_LEDGER_CONTRACT: FAIL"
        exit 1
    }
}

# Lock the captured numbers into the host gate so silent edits are visible in qaFast.
$locked = @{
    milk_stock_after = "89.0"
    customer_balance_after = "0.0"
    cash_balance_after = "1840.0"
    fuel_expense_balance_after = "300.0"
    first_cash_sale_total = "1000.0"
}
foreach ($key in $locked.Keys) {
    if ($props[$key] -ne $locked[$key]) {
        Write-Host "Expectation drift for $key : $($props[$key]) (locked $($locked[$key]))"
        Write-Host "GOLDEN_LEDGER_CONTRACT: FAIL"
        exit 1
    }
}

Write-Host "Golden ledger host contract OK (instrumented execution is enforced by qaScreen/qaCheckpoint)."
Write-Host "GOLDEN_LEDGER_CONTRACT: PASS"
exit 0
