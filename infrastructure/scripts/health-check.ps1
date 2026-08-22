[CmdletBinding()]
param([ValidateSet('default', 'dev', 'demo')][string]$Mode = 'default')
$ErrorActionPreference = 'Continue'
$infraDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $infraDir 'env/.env'
if (-not (Test-Path $envFile)) { $envFile = Join-Path $infraDir 'env/.env.example' }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Write-Error 'Docker is required for PolicyMesh health checks.'; exit 1 }
$baseCompose = Join-Path $infraDir 'compose/docker-compose.yml'
$composeArgs = @('compose', '--env-file', $envFile, '-f', $baseCompose)
if ($Mode -ne 'default') { $composeArgs += @('-f', (Join-Path $infraDir "compose/docker-compose.$Mode.yml")) }
function Test-ComposeCommand([string[]]$Command) { & docker @composeArgs exec -T @Command *> $null; return $LASTEXITCODE -eq 0 }
function Show-Status([string]$Name, [string]$Value) { Write-Host ('{0,-16} {1}' -f $Name, $Value) }
$failed = $false
Write-Host '================================'
Write-Host '     POLICYMESH HEALTH CHECK'
Write-Host '================================'
if (Test-ComposeCommand @('postgres', 'pg_isready', '-U', ((Get-Content $envFile | Where-Object { $_ -match '^POSTGRES_USER=' }) -replace '^POSTGRES_USER=', ''), '-d', ((Get-Content $envFile | Where-Object { $_ -match '^POSTGRES_DB=' }) -replace '^POSTGRES_DB=', ''))) { Show-Status PostgreSQL 'UP' } else { Show-Status PostgreSQL 'DOWN'; $failed = $true }
if (Test-ComposeCommand @('redis', 'redis-cli', 'ping')) { Show-Status Redis 'UP' } else { Show-Status Redis 'DOWN'; $failed = $true }
if (Test-ComposeCommand @('kafka', '/opt/kafka/bin/kafka-topics.sh', '--bootstrap-server', 'localhost:9092', '--list')) { Show-Status Kafka 'UP' } else { Show-Status Kafka 'DOWN'; $failed = $true }
$aiPort = ((Get-Content $envFile | Where-Object { $_ -match '^AI_SERVICE_PORT=' }) -replace '^AI_SERVICE_PORT=', '')
try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 "http://localhost:$aiPort/health" | Out-Null; Show-Status 'AI Service' 'UP' } catch { Show-Status 'AI Service' 'DOWN'; $failed = $true }
Show-Status Backend 'NOT CONFIGURED (no Dockerfile)'
Show-Status Frontend 'NOT CONFIGURED (no Dockerfile)'
Write-Host '================================'
if ($failed) { exit 1 }
