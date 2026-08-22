# ==============================================================================
# PolicyMesh — Health Check (PowerShell)
# ==============================================================================
# Check the health of all PolicyMesh services.
# Usage: .\scripts\health-check.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH HEALTH CHECK"
    Write-Host ""
    Write-Host "Usage: .\scripts\health-check.ps1 [options]"
    Write-Host ""
    Write-Host "Check the health of all PolicyMesh services."
    Write-Host ""
    Write-CommonHelp
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

# --- Load environment ---
$envFiles = @(
    (Join-Path $script:RepoRoot ".env"),
    (Join-Path $script:RepoRoot "backend\.env"),
    (Join-Path $script:RepoRoot "infrastructure\env\.env.dev")
)
foreach ($f in $envFiles) { Import-EnvFile $f }

# --- Service URLs ---
$backendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:8080" }
$aiServiceUrl = if ($env:AI_SERVICE_URL) { $env:AI_SERVICE_URL } else { "http://localhost:8000" }
$frontendUrl = if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://localhost:5173" }

$pgPort = if ($env:POSTGRES_PORT) { [int]$env:POSTGRES_PORT } else { 5432 }
$redisPort = if ($env:REDIS_PORT) { [int]$env:REDIS_PORT } else { 6379 }
$kafkaPort = if ($env:KAFKA_PORT) { [int]$env:KAFKA_PORT } else { 9092 }

# --- Health checks ---
$total = 0; $passed = 0; $failed = 0

function Test-HttpHealth {
    param([string]$Url, [string]$HealthPath = "")
    try {
        $testUrl = if ($HealthPath) { "$Url$HealthPath" } else { $Url }
        $response = Invoke-WebRequest -Uri $testUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -in @(200, 201, 204)) {
            return @{ Up = $true; Code = $response.StatusCode }
        }
    } catch {}
    return @{ Up = $false; Code = 0 }
}

Write-LogHeader "POLICYMESH HEALTH CHECK"
Write-Host ""

# PostgreSQL
$script:total++
if (Test-PortInUse -Port $pgPort) {
    Write-CheckPass "PostgreSQL"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "PostgreSQL") -ForegroundColor Yellow
    $script:total--
}

# Redis
$script:total++
if (Test-PortInUse -Port $redisPort) {
    Write-CheckPass "Redis"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "Redis") -ForegroundColor Yellow
    $script:total--
}

# Kafka
$script:total++
if (Test-PortInUse -Port $kafkaPort) {
    Write-CheckPass "Kafka"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "Kafka") -ForegroundColor Yellow
    $script:total--
}

# Backend
$script:total++
$health = Test-HttpHealth -Url $backendUrl -HealthPath "/actuator/health"
if ($health.Up) {
    Write-CheckPass "Backend"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "Backend") -ForegroundColor Yellow
    $script:total--
}

# AI Service
$script:total++
$health = Test-HttpHealth -Url $aiServiceUrl -HealthPath "/health"
if (-not $health.Up) { $health = Test-HttpHealth -Url $aiServiceUrl }
if ($health.Up) {
    Write-CheckPass "AI Service"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "AI Service") -ForegroundColor Yellow
    $script:total--
}

# Frontend
$script:total++
$health = Test-HttpHealth -Url $frontendUrl
if ($health.Up) {
    Write-CheckPass "Frontend"
    $script:passed++
} else {
    Write-Host ("  ⏭  {0,-16} NOT RUNNING" -f "Frontend") -ForegroundColor Yellow
    $script:total--
}

# --- Summary ---
Write-Host ""
if ($failed -eq 0 -and $total -gt 0) {
    Write-LogSuccess "All $total service(s) healthy."
} elseif ($total -eq 0) {
    Write-LogWarning "No services detected."
} else {
    Write-LogWarning "$passed/$total services healthy. ($failed down)"
}

Write-Host ""
Write-Host "Service URLs:"
Write-Host "  Frontend:     $frontendUrl"
Write-Host "  Backend:      $backendUrl"
Write-Host "  AI Service:   $aiServiceUrl"
Write-Host ""

if ($failed -gt 0) { exit 1 }
exit 0
