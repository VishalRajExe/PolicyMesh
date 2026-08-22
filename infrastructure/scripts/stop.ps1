$ErrorActionPreference = 'Stop'
$infraDir = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $infraDir 'env/.env'
if (-not (Test-Path $envFile)) { Copy-Item (Join-Path $infraDir 'env/.env.example') $envFile }
& docker compose --env-file $envFile -f (Join-Path $infraDir 'compose/docker-compose.yml') down
