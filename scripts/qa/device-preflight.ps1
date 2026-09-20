# Ensure the device-of-record is selected before connected instrumentation.
param(
    [string]$PreferredSerial = "R8VXA02SNKH",
    [string]$RequestedSerial = ""
)

$ErrorActionPreference = "Stop"

$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "adb" }
if (-not (Get-Command $adb -ErrorAction SilentlyContinue) -and -not (Test-Path $adb)) {
    throw "adb not found. Set ANDROID_HOME or put adb on PATH."
}

$raw = & $adb devices | Where-Object { $_ -match "\tdevice$" }
$serials = @($raw | ForEach-Object { ($_ -split "\s+")[0] })

if ($serials.Count -eq 0) {
    Write-Host "No connected adb devices in 'device' state."
    Write-Host "DEVICE_PREFLIGHT: FAIL"
    exit 1
}

$target = $RequestedSerial
if (-not $target) { $target = $env:ANDROID_SERIAL }
if (-not $target) {
    if ($serials -contains $PreferredSerial) {
        $target = $PreferredSerial
    } elseif ($serials.Count -eq 1) {
        Write-Host "Preferred serial $PreferredSerial is not connected."
        Write-Host "Connected: $($serials -join ', ')"
        Write-Host "Pass -Pqa.device=<serial> or set ANDROID_SERIAL only when deliberately targeting that device."
        Write-Host "DEVICE_PREFLIGHT: FAIL"
        exit 1
    } else {
        Write-Host "Multiple devices connected and no -Pqa.device / ANDROID_SERIAL provided."
        Write-Host "Connected: $($serials -join ', ')"
        Write-Host "Preferred device of record: $PreferredSerial"
        Write-Host "DEVICE_PREFLIGHT: FAIL"
        exit 1
    }
}

if ($serials -notcontains $target) {
    Write-Host "Requested serial $target is not connected."
    Write-Host "Connected: $($serials -join ', ')"
    Write-Host "DEVICE_PREFLIGHT: FAIL"
    exit 1
}

# If preferred exists and request omitted preferred, require preferred when multiple devices.
if (-not $RequestedSerial -and -not $env:ANDROID_SERIAL -and ($serials -contains $PreferredSerial) -and $target -ne $PreferredSerial) {
    Write-Host "Refusing non-preferred device while $PreferredSerial is available."
    Write-Host "DEVICE_PREFLIGHT: FAIL"
    exit 1
}

$model = (& $adb -s $target shell getprop ro.product.model).Trim()
Write-Host "Using device serial=$target model=$model"
# Emit a parseable assignment for callers; also set for this process.
$env:ANDROID_SERIAL = $target
Write-Output "ANDROID_SERIAL=$target"
Write-Host "DEVICE_PREFLIGHT: PASS"
exit 0
