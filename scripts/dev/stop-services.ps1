<#
.SYNOPSIS
  Stops the Java processes started by scripts/dev/start-services.ps1 (development only).
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$pidFile = Join-Path $root '.local\logs\pids.txt'
if (-not (Test-Path $pidFile)) { Write-Host 'No services started by start-services.ps1'; return }
Get-Content $pidFile | ForEach-Object {
    $process = Get-Process -Id $_ -ErrorAction SilentlyContinue
    if ($process) { Stop-Process -Id $_ -Force; Write-Host "Stopped pid $_" }
}
Remove-Item $pidFile
