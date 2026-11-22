<#
.SYNOPSIS
  Local development only. Restores the Keycloak master-realm administrator of infra/.env
  (KC_BOOTSTRAP_ADMIN_USERNAME / KC_BOOTSTRAP_ADMIN_PASSWORD) when Keycloak rejects it with "Invalid user credentials".
.DESCRIPTION
  KC_BOOTSTRAP_ADMIN_* only create the administrator the first time the master realm is created; if that account was
  changed or deleted later, the demo and load scripts can no longer log in. This script:
    1. stops the keycloak container and runs the official recovery command "kc.sh bootstrap-admin user" once, which
       creates a temporary administrator "tmp-admin" with the password of infra/.env;
    2. starts keycloak again and, with tmp-admin, creates the administrator of infra/.env if it is missing, sets its
       password to the one in infra/.env and gives it the master role "admin";
    3. deletes tmp-admin.
  It never prints passwords or tokens. The realm "verivoto" and its data are not touched.
.EXAMPLE
  .\scripts\dev\keycloak-restore-local-admin.ps1
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $root 'infra\.env'
. (Join-Path $root 'scripts\Import-DotEnv.ps1') $envFile
$username = if ($env:KC_BOOTSTRAP_ADMIN_USERNAME) { $env:KC_BOOTSTRAP_ADMIN_USERNAME } else { 'local-admin' }
$password = $env:KC_BOOTSTRAP_ADMIN_PASSWORD
if (-not $password) { throw 'KC_BOOTSTRAP_ADMIN_PASSWORD is empty in infra/.env' }
$kc = 'http://localhost:8080'
$compose = @('compose', '-f', (Join-Path $root 'infra\compose.yaml'), '--env-file', $envFile)

function Invoke-Compose {
    & docker @compose @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose $($args -join ' ') failed" }
}

Write-Host 'Stopping keycloak and creating the temporary administrator tmp-admin...'
Invoke-Compose stop keycloak
Invoke-Compose run --rm --no-deps keycloak bootstrap-admin user --username tmp-admin --password:env KC_BOOTSTRAP_ADMIN_PASSWORD --no-prompt
Invoke-Compose start keycloak

Write-Host 'Waiting for keycloak...'
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    try { Invoke-WebRequest -Uri "$kc/realms/master" -UseBasicParsing -TimeoutSec 5 | Out-Null; break } catch { }
    if ((Get-Date) -gt $deadline) { throw 'Keycloak did not start in 3 minutes (docker compose logs keycloak)' }
    Start-Sleep -Seconds 3
}

$token = (Invoke-RestMethod -Method Post -Uri "$kc/realms/master/protocol/openid-connect/token" -Body @{
    grant_type = 'password'; client_id = 'admin-cli'; username = 'tmp-admin'; password = $password }).access_token
$headers = @{ Authorization = "Bearer $token" }
$api = "$kc/admin/realms/master"
function Find-User([string]$name) {
    @(Invoke-RestMethod -Uri "$api/users?username=$([uri]::EscapeDataString($name))&exact=true" -Headers $headers) |
        Where-Object { $_.username -eq $name } | Select-Object -First 1
}

$admin = Find-User $username
if (-not $admin) {
    Invoke-RestMethod -Method Post -Uri "$api/users" -Headers $headers -ContentType 'application/json' `
        -Body (@{ username = $username; enabled = $true } | ConvertTo-Json) | Out-Null
    $admin = Find-User $username
    Write-Host "Created $username"
}
Invoke-RestMethod -Method Put -Uri "$api/users/$($admin.id)/reset-password" -Headers $headers -ContentType 'application/json' `
    -Body (@{ type = 'password'; value = $password; temporary = $false } | ConvertTo-Json) | Out-Null
Invoke-RestMethod -Method Put -Uri "$api/users/$($admin.id)" -Headers $headers -ContentType 'application/json' `
    -Body (@{ enabled = $true; requiredActions = @() } | ConvertTo-Json) | Out-Null
$role = Invoke-RestMethod -Uri "$api/roles/admin" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$api/users/$($admin.id)/role-mappings/realm" -Headers $headers -ContentType 'application/json' `
    -Body (ConvertTo-Json -InputObject @(@{ id = $role.id; name = $role.name })) | Out-Null
Write-Host "Password of $username set from infra/.env; role admin assigned"

$temporary = Find-User 'tmp-admin'
if ($temporary) {
    Invoke-RestMethod -Method Delete -Uri "$api/users/$($temporary.id)" -Headers $headers | Out-Null
    Write-Host 'Deleted tmp-admin'
}
Write-Host 'Done. Check: node scripts/dev/keycloak-api-audience.mjs'
