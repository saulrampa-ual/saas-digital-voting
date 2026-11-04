<#
.SYNOPSIS
  Analyses backend and frontend with the local SonarQube (F1, docs/architecture/quality-and-security.md).

.DESCRIPTION
  1. Starts SonarQube (infra/compose.quality.yaml) if it is not running and waits until it is UP.
  2. Backend: mvnw verify (tests and JaCoCo reports) and the pinned sonar-maven-plugin.
  3. Frontend: @sonar/scan with frontend/sonar-project.properties.
  4. Prints the quality gate of both projects.
  The token is read from $env:SONAR_TOKEN (SonarQube > My Account > Security > Generate token, type
  "Global Analysis Token"); it is never written to a file.

.EXAMPLE
  $env:SONAR_TOKEN = "<token>"; pwsh scripts/quality/sonar.ps1
  pwsh scripts/quality/sonar.ps1 -SkipBackend
#>
param(
  [string]$HostUrl = 'http://localhost:9000',
  [switch]$SkipBackend,
  [switch]$SkipFrontend
)
$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
if (-not $env:SONAR_TOKEN) { throw 'Define $env:SONAR_TOKEN first (SonarQube > My Account > Security).' }

function Get-SonarStatus {
  try { (Invoke-RestMethod -Uri "$HostUrl/api/system/status" -TimeoutSec 5).status } catch { 'DOWN' }
}

if ((Get-SonarStatus) -ne 'UP') {
  Write-Host 'Starting SonarQube (infra/compose.quality.yaml)...'
  docker compose -f (Join-Path $root 'infra\compose.quality.yaml') up -d
  if ($LASTEXITCODE -ne 0) { throw 'docker compose failed' }
  $deadline = (Get-Date).AddMinutes(6)
  while ((Get-SonarStatus) -ne 'UP') {
    if ((Get-Date) -gt $deadline) { throw "SonarQube is not UP after 6 minutes ($HostUrl)" }
    Start-Sleep -Seconds 10
  }
}
Write-Host "SonarQube UP at $HostUrl"

if (-not $SkipBackend) {
  Push-Location (Join-Path $root 'backend')
  try {
    .\mvnw.cmd --batch-mode --no-transfer-progress verify `
      org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
      "-Dsonar.host.url=$HostUrl" "-Dsonar.token=$env:SONAR_TOKEN"
    if ($LASTEXITCODE -ne 0) { throw 'Backend analysis failed' }
  } finally { Pop-Location }
}

if (-not $SkipFrontend) {
  Push-Location (Join-Path $root 'frontend')
  try {
    npx --yes @sonar/scan@5.0.2 "-Dsonar.host.url=$HostUrl" "-Dsonar.token=$env:SONAR_TOKEN"
    if ($LASTEXITCODE -ne 0) { throw 'Frontend analysis failed' }
  } finally { Pop-Location }
}

# The background task may need a few seconds before the gate is computed
Start-Sleep -Seconds 10
$headers = @{ Authorization = "Bearer $env:SONAR_TOKEN" }
foreach ($key in 'verivoto-backend', 'verivoto-frontend') {
  try {
    $gate = Invoke-RestMethod -Uri "$HostUrl/api/qualitygates/project_status?projectKey=$key" -Headers $headers
    Write-Host ("{0}: quality gate {1} -> {2}/dashboard?id={0}" -f $key, $gate.projectStatus.status, $HostUrl)
  } catch { Write-Host "${key}: not analysed" }
}
