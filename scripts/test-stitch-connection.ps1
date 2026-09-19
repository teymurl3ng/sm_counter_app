[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$stitchKey = [Environment]::GetEnvironmentVariable('STITCH_API_KEY', 'User')
if ([string]::IsNullOrWhiteSpace($stitchKey)) {
    $stitchKey = [Environment]::GetEnvironmentVariable('STITCH_API_KEY', 'Process')
}
if ([string]::IsNullOrWhiteSpace($stitchKey)) {
    Write-Host 'Stitch is not authenticated yet. Run scripts/set-stitch-api-key.ps1 locally.'
    exit 2
}

$stitchHeaders = @{
    'X-Goog-Api-Key' = $stitchKey
    'Accept' = 'application/json, text/event-stream'
}
$stitchKey = $null
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

function Invoke-StitchRequest {
    param([hashtable]$Message)
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri 'https://stitch.googleapis.com/mcp' -Method Post -Headers $stitchHeaders -ContentType 'application/json' -Body ($Message | ConvertTo-Json -Depth 20 -Compress) -TimeoutSec 45 -MaximumRedirection 0
    } catch {
        # Suppress raw response/exception text to avoid logging credential echoes.
        $status = 'unavailable'
        if ($null -ne $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        throw "Stitch request failed (HTTP $status). Check the key, network, and Stitch account access."
    }
    if ($response.Headers['Mcp-Session-Id']) {
        $stitchHeaders['Mcp-Session-Id'] = [string]$response.Headers['Mcp-Session-Id']
    }
    if ([string]::IsNullOrWhiteSpace($response.Content)) { return $null }
    $payload = [string]$response.Content
    if ($payload.TrimStart().StartsWith('{')) {
        try { $result = $payload | ConvertFrom-Json } catch { throw 'Stitch returned invalid JSON. Response text was suppressed.' }
    } else {
        $result = $null
        foreach ($eventBlock in ($payload -split '\r?\n\r?\n')) {
            $data = (($eventBlock -split '\r?\n' | Where-Object { $_ -match '^data:' }) -replace '^data: ?', '') -join "`n"
            if ([string]::IsNullOrWhiteSpace($data)) { continue }
            try { $candidate = $data | ConvertFrom-Json } catch { throw 'Stitch returned invalid event data. Response text was suppressed.' }
            if ($candidate.id -eq $Message.id) { $result = $candidate; break }
        }
        if ($null -eq $result -and $Message.ContainsKey('id')) {
            throw 'Stitch returned no matching response.'
        }
    }
    if ($null -ne $result.error) {
        throw "Stitch returned an MCP error (code $($result.error.code)). Response text was suppressed."
    }
    return $result.result
}

try {
    $init = Invoke-StitchRequest @{
        jsonrpc = '2.0'; id = 1; method = 'initialize'
        params = @{ protocolVersion = '2025-03-26'; capabilities = @{}; clientInfo = @{ name = 'saaf-stitch-connection-check'; version = '1.0.0' } }
    }
    if (-not $init.protocolVersion) { throw 'Stitch initialization did not return a protocol version.' }
    $stitchHeaders['MCP-Protocol-Version'] = [string]$init.protocolVersion
    $null = Invoke-StitchRequest @{ jsonrpc = '2.0'; method = 'notifications/initialized' }
    $allTools = @()
    $cursor = $null
    $requestId = 2
    do {
        $listParams = @{}
        if ($cursor) { $listParams.cursor = $cursor }
        $listing = Invoke-StitchRequest @{ jsonrpc = '2.0'; id = $requestId; method = 'tools/list'; params = $listParams }
        $allTools += @($listing.tools)
        $cursor = $listing.nextCursor
        $requestId++
    } while ($cursor)
    if ($allTools.Count -eq 0) { throw 'Stitch returned no tools.' }
    Write-Host "MCP initialization and tool discovery passed: $($allTools.Count) tools available."
    Write-Host ('Tools: ' + (($allTools | ForEach-Object { $_.name }) -join ', '))
    $projectReader = $allTools | Where-Object { $_.name -eq 'list_projects' } | Select-Object -First 1
    if ($null -eq $projectReader) { throw 'Read-only list_projects tool is unavailable.' }
    $projects = Invoke-StitchRequest @{ jsonrpc = '2.0'; id = $requestId; method = 'tools/call'; params = @{ name = 'list_projects'; arguments = @{} } }
    if ($projects.isError) { throw 'Stitch project access failed. Project response text was suppressed.' }
    Write-Host 'Authenticated read-only project access passed. Project contents were not printed.'
    Write-Host 'No project was created and no design-generation request was sent.'
} catch {
    Write-Host $_.Exception.Message
    exit 1
} finally {
    $stitchHeaders.Clear()
}
exit 0
