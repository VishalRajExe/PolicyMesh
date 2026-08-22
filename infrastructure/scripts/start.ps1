[CmdletBinding()]
param([ValidateSet('default', 'dev', 'demo')][string]$Mode = 'default')
$ErrorActionPreference = 'Stop'
$infraDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $infraDir 'env/.env'
$baseCompose = Join-Path $infraDir 'compose/docker-compose.yml'
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker is required.' }
& docker compose version | Out-Null
if (-not (Test-Path $envFile)) {
  Copy-Item (Join-Path $infraDir 'env/.env.example') $envFile
  Write-Host "Created DEVELOPMENT ONLY $envFile; review its credentials before sharing."
}
$composeArgs = @('compose', '--env-file', $envFile, '-f', $baseCompose)
if ($Mode -ne 'default') { $composeArgs += @('-f', (Join-Path $infraDir "compose/docker-compose.$Mode.yml")) }
& docker @composeArgs up -d --build --remove-orphans
$ready = $false
for ($i = 1; $i -le 30; $i++) {
  & (Join-Path $PSScriptRoot 'health-check.ps1') -Mode $Mode *> $null
  if ($LASTEXITCODE -eq 0) { $ready = $true; break }
  Start-Sleep -Seconds 3
}
& (Join-Path $PSScriptRoot 'health-check.ps1') -Mode $Mode
if (-not $ready) { throw 'PolicyMesh services did not become healthy within 90 seconds.' }
Write-Host 'AI service: http://localhost:8000'
Write-Host 'PostgreSQL: localhost:5432 | Redis: localhost:6379 | Kafka: localhost:9092'
