# Run isolated connected instrumentation for one screen or the full checkpoint set.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("auth", "sale", "collect", "activity", "more", "work", "checkpoint")]
    [string]$Screen,
    [string]$Serial = "",
    [string]$Phase18Dir = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $Phase18Dir) { $Phase18Dir = Join-Path $repoRoot "phase18" }

$map = @{
    auth = @(
        "com.saaf.milkshop.NativeAuthenticationScreenContractTest",
        "com.saaf.milkshop.NativeAuthenticationFacadeTest",
        "com.saaf.milkshop.NativeFoundationUiTest"
    )
    sale = @(
        "com.saaf.milkshop.NativeFoundationUiTest",
        "com.saaf.milkshop.NativeSaleHardeningContractTest",
        "com.saaf.milkshop.NativeSaleScreenContractTest",
        "com.saaf.milkshop.Phase8SaleOperationsTest",
        "com.saaf.milkshop.qa.NativeGoldenLedgerRegressionTest"
    )
    collect = @(
        "com.saaf.milkshop.NativeCollectionFacadeTest",
        "com.saaf.milkshop.NativeCollectionScreenContractTest",
        "com.saaf.milkshop.qa.NativeCollectionAcceptanceGapsTest",
        "com.saaf.milkshop.qa.NativeCollectionDeviceEvidenceTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#collectPaymentWritesRoomShowsReceiptAndOffersTheNextVisibleActions",
        "com.saaf.milkshop.qa.NativeGoldenLedgerRegressionTest"
    )
    activity = @(
        "com.saaf.milkshop.NativeActivityFacadeTest",
        "com.saaf.milkshop.NativeActivityScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#collectPaymentWritesRoomShowsReceiptAndOffersTheNextVisibleActions"
    )
    more = @(
        "com.saaf.milkshop.NativeMoreFacadeTest",
        "com.saaf.milkshop.NativeMoreScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#moreMenuHardwareBackReturnsToSellAndKeepsSession",
        "com.saaf.milkshop.NativeFoundationUiTest#switchAccountHardwareBackKeepsSessionAndReturnsToMore",
        "com.saaf.milkshop.NativeFoundationUiTest#switchAccountUiBackKeepsSessionAndReturnsToMore"
    )
    work = @(
        "com.saaf.milkshop.NativeOperationsWorkFacadeTest",
        "com.saaf.milkshop.NativeOperationsWorkScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsWorkHomeShowsPhysicalContext",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsNavPreviewDestinationsShowExplicitNotices",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsReceiveMilkOpensFamilyPreview",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsWorkHardwareBackEndsSessionLikeSell"
    )
}

if ($Screen -eq "checkpoint") {
    $classes = @($map.auth + $map.sale + $map.collect + $map.activity + $map.more + $map.work | Select-Object -Unique)
} else {
    $classes = @($map[$Screen])
}

if ($classes.Count -eq 0) {
    Write-Host "No tests registered for screen=$Screen"
    exit 1
}

$preflightArgs = @()
if ($Serial) { $preflightArgs += @("-RequestedSerial", $Serial) }
$preflightOut = & (Join-Path $PSScriptRoot "device-preflight.ps1") @preflightArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$parsed = ($preflightOut | Select-String -Pattern '^ANDROID_SERIAL=(.+)$').Matches.Groups[1].Value
if ($parsed) { $env:ANDROID_SERIAL = $parsed }
elseif ($Serial) { $env:ANDROID_SERIAL = $Serial }
elseif (-not $env:ANDROID_SERIAL) { $env:ANDROID_SERIAL = "R8VXA02SNKH" }

$classArg = ($classes -join ",")
Write-Host "Running connected tests on $($env:ANDROID_SERIAL) classCount=$($classes.Count)"
Set-Location $Phase18Dir
& .\gradlew.bat `
    ":app:connectedDebugAndroidTest" `
    "-Psaaf.instrumentationSandbox=true" `
    "-Pandroid.testInstrumentationRunnerArguments.class=$classArg"
exit $LASTEXITCODE
