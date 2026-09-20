# Summarize review sidecars for the current checkpoint evidence run.
param(
    [string]$HostRoot = ""
)

$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "check-evidence-review.ps1"
if ($HostRoot) {
    & $script -HostRoot $HostRoot
} else {
    & $script
}
exit $LASTEXITCODE
