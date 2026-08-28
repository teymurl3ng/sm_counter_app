[CmdletBinding()]
param(
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$workspaceRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$uiRoot = Join-Path $workspaceRoot 'ui'
$distRoot = Join-Path $uiRoot 'dist'
$androidAssetsRoot = Join-Path $workspaceRoot 'phase18\app\src\main\assets\hybrid'

& python (Join-Path $workspaceRoot 'scripts\verify-bridge-contract.py')
if ($LASTEXITCODE -ne 0) { throw 'Bridge contract verification failed.' }

Push-Location $uiRoot
try {
    if (-not $SkipInstall -and -not (Test-Path -LiteralPath (Join-Path $uiRoot 'node_modules'))) {
        & npm ci --ignore-scripts --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'UI dependency installation failed.' }
    }
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw 'UI production build failed.' }
} finally {
    Pop-Location
}

$indexPath = Join-Path $distRoot 'index.html'
if (-not (Test-Path -LiteralPath $indexPath)) {
    throw "Vite did not create $indexPath"
}

$normalizedTarget = [IO.Path]::GetFullPath($androidAssetsRoot)
if (-not $normalizedTarget.StartsWith($workspaceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to replace assets outside the workspace: $androidAssetsRoot"
}
if (Test-Path -LiteralPath $androidAssetsRoot) {
    $resolvedTarget = (Resolve-Path -LiteralPath $androidAssetsRoot).Path
    if (-not $resolvedTarget.StartsWith($workspaceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to replace assets outside the workspace: $resolvedTarget"
    }
    Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
}
New-Item -ItemType Directory -Path (Split-Path -Parent $androidAssetsRoot) -Force | Out-Null
New-Item -ItemType Directory -Path $androidAssetsRoot -Force | Out-Null
Copy-Item -Path (Join-Path $distRoot '*') -Destination $androidAssetsRoot -Recurse -Force

Write-Output "Packaged hybrid UI: $androidAssetsRoot"
