[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Write-Host 'Paste your Stitch API key at the masked prompt, then press Enter.'
Write-Host 'It will be saved in your Windows user environment, outside this repository.'
$stitchSecret = Read-Host 'Stitch API key' -AsSecureString
$stitchSecretPointer = [IntPtr]::Zero
try {
    $stitchSecretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($stitchSecret)
    $stitchKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($stitchSecretPointer).Trim()
    if ([string]::IsNullOrWhiteSpace($stitchKey) -or $stitchKey -match '\s') {
        throw 'No usable key entered. Nothing was saved.'
    }
    [Environment]::SetEnvironmentVariable('STITCH_API_KEY', $stitchKey, 'User')
    [Environment]::SetEnvironmentVariable('STITCH_API_KEY', $stitchKey, 'Process')
    Write-Host 'Stitch key saved. Its value has not been printed or written to the repository.'
} finally {
    if ($stitchSecretPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($stitchSecretPointer)
    }
    $stitchKey = $null
    $stitchSecret.Dispose()
}

& (Join-Path $PSScriptRoot 'test-stitch-connection.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Next: fully quit and reopen Codex so its MCP connection can read the saved key.'
