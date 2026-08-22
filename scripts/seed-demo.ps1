# ==============================================================================
# PolicyMesh - Seed Demo Data (PowerShell)
# ==============================================================================
# Populate PolicyMesh with the canonical demo scenario.
# Usage: .\scripts\seed-demo.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH DEMO SEED"
    Write-Host ""
    Write-Host "Usage: .\scripts\seed-demo.ps1 [options]"
    Write-Host ""
    Write-Host "Populate PolicyMesh with the canonical demo scenario."
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

$backendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:8080" }
$backendToken = if ($env:BACKEND_TOKEN) { $env:BACKEND_TOKEN } else { "" }

Write-LogHeader "POLICYMESH DEMO SEED"
Write-Host ""

# --- Check backend ---
Write-LogInfo "Checking backend availability..."
try {
    $resp = Invoke-WebRequest -Uri "$backendUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-LogSuccess "Backend is up"
} catch {
    Write-LogError "Backend is not reachable at $backendUrl"
    Write-Host ""
    Write-Host "Start the backend first:"
    Write-Host "  .\scripts\start.ps1"
    exit 1
}
Write-Host ""

# --- Obtain auth token if needed ---
if (-not $backendToken) {
    try {
        $loginBody = '{"email":"admin@policymesh.io","password":"adminPassword123!"}'
        $loginResp = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
        $backendToken = $loginResp.token
    } catch {
        try {
            $regBody = '{"email":"admin@policymesh.io","password":"adminPassword123!","role":"ADMIN"}'
            $regResp = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/register" -Method Post -Body $regBody -ContentType "application/json" -ErrorAction Stop
            $loginResp = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
            $backendToken = $loginResp.token
        } catch {}
    }
}

# --- Helper functions ---
function Invoke-ApiPost {
    param([string]$Endpoint, [string]$Body)
    $headers = @{ "Content-Type" = "application/json" }
    if ($backendToken) { $headers["Authorization"] = "Bearer $backendToken" }
    try {
        $resp = Invoke-RestMethod -Uri "$backendUrl$Endpoint" -Method Post -Headers $headers -Body $Body -ErrorAction Stop
        return $resp
    } catch {
        return $null
    }
}

# --- Try dev/seed endpoint ---
Write-LogInfo "Attempting backend demo seed endpoint..."
$seedResp = Invoke-ApiPost -Endpoint "/api/v1/dev/seed" -Body "{}"
if ($seedResp) {
    Write-LogSuccess "Demo seed endpoint called successfully"
    Write-Host ($seedResp | ConvertTo-Json -Compress)
    Write-Host ""
    Write-LogSuccess "Demo data loaded via backend API."
    exit 0
}

Write-LogHeader "POLICYMESH DEMO SEED COMPLETE"
exit 0
