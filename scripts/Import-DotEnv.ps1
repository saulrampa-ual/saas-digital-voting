<#
.SYNOPSIS
  Loads KEY=VALUE lines of a .env file into environment variables of the current PowerShell session.
.EXAMPLE
  . .\scripts\Import-DotEnv.ps1            # loads infra/.env
  . .\scripts\Import-DotEnv.ps1 other.env
#>
param([string]$Path = "infra/.env")

if (-not (Test-Path $Path)) { throw "File not found: $Path" }

Get-Content $Path | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value, 'Process')
}
Write-Host "Loaded variables from $Path"
