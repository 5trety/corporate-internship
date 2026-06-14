$ErrorActionPreference = 'SilentlyContinue'

$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root '.codex-run-logs'
$pidFiles = @(
  (Join-Path $logs 'frontend-5174.pid'),
  (Join-Path $logs 'backend-8081.pid'),
  (Join-Path $logs 'mysql-3307.pid')
)

foreach ($pidFile in $pidFiles) {
  if (Test-Path $pidFile) {
    $existingPid = [int](Get-Content $pidFile)
    if ($existingPid -and (Get-Process -Id $existingPid)) {
      Stop-Process -Id $existingPid -Force
    }
    Remove-Item $pidFile -Force
  }
}

Write-Host "Local services stopped"
