<#
.SYNOPSIS
  Starts the four services and the gateway as local Java processes against infra/compose.yaml (development only).
.DESCRIPTION
  Loads infra/.env, sets the database users of each service, optionally builds the jars, starts each jar in the
  background with its logs in .local/logs (ignored by git) and waits until every /actuator/health answers UP.
  The infrastructure (PostgreSQL, Keycloak, Mailpit) must already be running:
    docker compose -f infra/compose.yaml --env-file infra/.env up -d
  Stop everything with scripts/dev/stop-services.ps1.
.PARAMETER Build
  Builds the jars first (mvnw package, tests skipped). Use it after any backend change.
.PARAMETER NoRateLimit
  Starts the gateway with GATEWAY_VOTER_ACCESS_RATE_LIMIT_ENABLED=false (load test H1c from one machine).
.EXAMPLE
  .\scripts\dev\start-services.ps1 -Build -NoRateLimit
#>
param([switch]$Build, [switch]$NoRateLimit)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $root

# Variables are set only while the services start: the Java processes keep their own copy, and the PowerShell session
# gets its previous values back at the end, so later commands (mvnw verify, npm) do not inherit them.
$envFile = Join-Path $root 'infra\.env'
$names = @(Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object { ($_ -split '=', 2)[0].Trim() }) +
    @('IDENTITY_DB_USERNAME', 'ELECTION_DB_USERNAME', 'VOTER_ACCESS_DB_USERNAME', 'BALLOT_BOX_DB_USERNAME',
      'GATEWAY_VOTER_ACCESS_RATE_LIMIT_ENABLED')
$previous = @{}
foreach ($n in $names) { $previous[$n] = [Environment]::GetEnvironmentVariable($n, 'Process') }
try {
. (Join-Path $root 'scripts\Import-DotEnv.ps1') $envFile
$env:IDENTITY_DB_USERNAME = 'identity'
$env:ELECTION_DB_USERNAME = 'election'
$env:VOTER_ACCESS_DB_USERNAME = 'voter_access'
$env:BALLOT_BOX_DB_USERNAME = 'ballot_box'
if ($NoRateLimit) {
    $env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_ENABLED = 'false'
    Write-Host 'Gateway: per-IP limit of voter-access DISABLED (H1c)'
}

if ($Build) {
    & (Join-Path $root 'backend\mvnw.cmd') -f (Join-Path $root 'backend\pom.xml') -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
}

$logs = Join-Path $root '.local\logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$pidFile = Join-Path $logs 'pids.txt'
if (Test-Path $pidFile) { throw "Services may be running already ($pidFile exists). Run scripts\dev\stop-services.ps1 first." }

$services = [ordered]@{
    'identity-service'     = 8081
    'election-service'     = 8082
    'voter-access-service' = 8083
    'ballot-box-service'   = 8084
    'api-gateway'          = 8000
}

# A port that already answers belongs to another process (an old run, the IDE...): the new service could not bind it
# and the health check below would be answered by that other process.
foreach ($name in $services.Keys) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { $busy = $client.ConnectAsync('127.0.0.1', $services[$name]).Wait(500) } catch { $busy = $false } finally { $client.Dispose() }
    if ($busy) { throw "Port $($services[$name]) ($name) is already in use by another process. Find it with: Get-CimInstance Win32_Process -Filter ""name='java.exe'"" | Select-Object ProcessId, CommandLine" }
}

foreach ($name in $services.Keys) {
    $jar = Join-Path $root "backend\services\$name\target\$name-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { throw "Missing $jar. Run again with -Build." }
    $process = Start-Process -FilePath 'java' -ArgumentList @('-jar', "`"$jar`"") -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs "$name.out.log") -RedirectStandardError (Join-Path $logs "$name.err.log")
    Add-Content -Path $pidFile -Value $process.Id
    Write-Host "Started $name (pid $($process.Id), port $($services[$name]))"
}
} finally {
    foreach ($n in $names) { [Environment]::SetEnvironmentVariable($n, $previous[$n], 'Process') }
}

$deadline = (Get-Date).AddMinutes(3)
foreach ($name in $services.Keys) {
    $url = "http://localhost:$($services[$name])/actuator/health"
    while ($true) {
        $alive = Get-Process -Id (Get-Content $pidFile)[[array]::IndexOf(@($services.Keys), $name)] -ErrorAction SilentlyContinue
        if (-not $alive) { throw "$name stopped while starting; see $logs\$name.out.log" }
        try {
            if ((Invoke-RestMethod -Uri $url -TimeoutSec 5).status -eq 'UP') { Write-Host "$name UP"; break }
        } catch { }
        if ((Get-Date) -gt $deadline) { throw "$name did not answer UP in 3 minutes; see $logs\$name.out.log" }
        Start-Sleep -Seconds 3
    }
}
Write-Host "All services UP. Logs in $logs"
