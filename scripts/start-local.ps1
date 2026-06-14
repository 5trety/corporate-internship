$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root '.codex-run-logs'
$mysqlBase = 'C:\Program Files\MySQL\MySQL Server 8.4'
$mysqld = Join-Path $mysqlBase 'bin\mysqld.exe'
$mysql = Join-Path $mysqlBase 'bin\mysql.exe'
$mysqladmin = Join-Path $mysqlBase 'bin\mysqladmin.exe'
$mysqlRoot = Join-Path $root '.local-mysql'
$mysqlData = Join-Path $mysqlRoot 'data'
$mysqlLog = Join-Path $logs 'mysql-3307.err.log'
$mysqlPid = Join-Path $logs 'mysql-3307.pid'
$backendPid = Join-Path $logs 'backend-8081.pid'
$frontendPid = Join-Path $logs 'frontend-5174.pid'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe'

New-Item -ItemType Directory -Force -Path $logs, $mysqlRoot | Out-Null

function Stop-ExistingProjectProcess([int]$port, [string]$pidFile) {
  if (Test-Path $pidFile) {
    $existingPid = [int](Get-Content $pidFile -ErrorAction SilentlyContinue)
    if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
      Stop-Process -Id $existingPid -Force
      Start-Sleep -Seconds 1
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
  }

  $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
  foreach ($connection in $connections) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)" -ErrorAction SilentlyContinue
    if ($process -and $process.CommandLine -like "*$root*") {
      Stop-Process -Id $connection.OwningProcess -Force
      Start-Sleep -Seconds 1
    }
  }
}

function Wait-Port([int]$port, [int]$timeoutSeconds) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  do {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
      return
    }
    Start-Sleep -Milliseconds 500
  } while ((Get-Date) -lt $deadline)

  throw "Port $port did not start within $timeoutSeconds seconds"
}

function Invoke-Native([string]$filePath, [string[]]$arguments) {
  & $filePath @arguments
  if ($LASTEXITCODE -ne 0) {
    throw "$filePath failed with exit code $LASTEXITCODE"
  }
}

function Test-MySqlRootConnection {
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  try {
    $output = & $mysql --protocol=tcp --host=127.0.0.1 --port=3307 --user=root --batch --skip-column-names --execute="SELECT 1;" 2>$null
  } finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }
  return $output -contains '1'
}

if (!(Test-Path $mysqld)) {
  throw "MySQL not found: $mysqld"
}

if (!(Test-Path (Join-Path $mysqlData 'auto.cnf'))) {
  New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
  Invoke-Native $mysqld @('--initialize-insecure', "--basedir=$mysqlBase", "--datadir=$mysqlData", '--console')
}

if (!(Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue)) {
  $mysqlArgs = @(
    '--no-defaults',
    "--basedir=`"$mysqlBase`"",
    "--datadir=`"$mysqlData`"",
    '--port=3307',
    '--bind-address=127.0.0.1',
    '--mysqlx=0',
    "--log-error=`"$mysqlLog`""
  ) -join ' '
  $mysqlProcess = Start-Process -FilePath $mysqld -ArgumentList $mysqlArgs -WindowStyle Hidden -PassThru
  $mysqlProcess.Id | Set-Content -Path $mysqlPid
  Wait-Port 3307 45
}

if (!(Test-MySqlRootConnection)) {
  throw "MySQL ping failed"
}

Stop-ExistingProjectProcess 8081 $backendPid
Stop-ExistingProjectProcess 5174 $frontendPid

Push-Location (Join-Path $root 'backend-demo')
try {
  .\mvnw.cmd -q -DskipTests package
  $jar = Join-Path (Get-Location) 'target\backend-demo-0.0.1-SNAPSHOT.jar'
  $backend = Start-Process -FilePath $java -ArgumentList @('-jar', $jar, '--server.port=8081') `
    -WorkingDirectory (Get-Location) `
    -RedirectStandardOutput (Join-Path $logs 'backend-8081.out.log') `
    -RedirectStandardError (Join-Path $logs 'backend-8081.err.log') `
    -WindowStyle Hidden -PassThru
  $backend.Id | Set-Content -Path $backendPid
} finally {
  Pop-Location
}
Wait-Port 8081 45

Push-Location (Join-Path $root 'frontend-admin')
try {
  if (!(Test-Path 'node_modules')) {
    npm install
  }
  $env:VITE_API_PROXY_TARGET = 'http://localhost:8081'
  $frontend = Start-Process -FilePath (Get-Command npm.cmd).Source -ArgumentList @('run', 'dev', '--', '--port', '5174', '--host', '0.0.0.0') `
    -WorkingDirectory (Get-Location) `
    -RedirectStandardOutput (Join-Path $logs 'frontend-5174.out.log') `
    -RedirectStandardError (Join-Path $logs 'frontend-5174.err.log') `
    -WindowStyle Hidden -PassThru
  $frontend.Id | Set-Content -Path $frontendPid
} finally {
  Pop-Location
}
Wait-Port 5174 45

Write-Host "Project started"
Write-Host "Frontend: https://localhost:5174/"
Write-Host "Backend: http://localhost:8081/api"
Write-Host "MySQL: 127.0.0.1:3307 user=root password=(empty)"
Write-Host "Logs: $logs"
