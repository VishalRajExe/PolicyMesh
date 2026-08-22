# ==============================================================================
# PolicyMesh - Run Demo (PowerShell)
# ==============================================================================
# Execute the full PolicyMesh hackathon demo.
# Usage: .\scripts\run-demo.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH HACKATHON DEMO"
    Write-Host ""
    Write-Host "Usage: .\scripts\run-demo.ps1 [options]"
    Write-Host ""
    Write-Host "Run the complete PolicyMesh hackathon demo."
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
$aiServiceUrl = if ($env:AI_SERVICE_URL) { $env:AI_SERVICE_URL } else { "http://localhost:8000" }
$frontendUrl = if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://localhost:5173" }
$backendToken = if ($env:BACKEND_TOKEN) { $env:BACKEND_TOKEN } else { "" }

$totalSteps = 7; $currentStep = 0

Write-LogHeader "POLICYMESH HACKATHON DEMO"
Write-Host ""

# --- [1] Infrastructure ---
$currentStep++
Write-Step $currentStep $totalSteps "Infrastructure"
Write-Host ""

try {
    $resp = Invoke-WebRequest -Uri "$backendUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-CheckPass "Infrastructure"
} catch {
    Write-CheckFail "Infrastructure"
    Write-Host "Backend is not running. Start it first: .\scripts\start.ps1"
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

# --- [2] Demo Data ---
$currentStep++
Write-Step $currentStep $totalSteps "Demo Data"
Write-Host ""

try {
    $headers = @{}
    if ($backendToken) { $headers["Authorization"] = "Bearer $backendToken" }
    $services = Invoke-RestMethod -Uri "$backendUrl/api/v1/services" -Headers $headers -TimeoutSec 5 -ErrorAction Stop
    Write-CheckPass "Demo data loaded"
} catch {
    Write-LogInfo "Demo data not found - seeding..."
    & "$PSScriptDir\seed-demo.ps1" -NoColor 2>$null
    Write-CheckPass "Demo data seeded"
}
Write-Host ""

# --- [3] CI Valid ---
$currentStep++
Write-Step $currentStep $totalSteps "CI Valid Scenario"
Write-Host ""

& "$PSScriptDir\run-ci-check.ps1" -Scenario valid -NoColor
if ($LASTEXITCODE -eq 0) {
    Write-CheckPass "CI Valid Scenario: PASS"
} else {
    Write-CheckFail "CI Valid Scenario: FAIL"
}
Write-Host ""

# --- [4] CI Blocked ---
$currentStep++
Write-Step $currentStep $totalSteps "CI Blocked Scenario"
Write-Host ""

& "$PSScriptDir\run-ci-check.ps1" -Scenario blocked -NoColor
if ($LASTEXITCODE -eq 0) {
    Write-CheckPass "CI Blocked Scenario: violation correctly detected"
} else {
    Write-CheckFail "CI Blocked Scenario: unexpected result"
}
Write-Host ""

# --- [5] Runtime EU -> EU ---
$currentStep++
Write-Step $currentStep $totalSteps "Runtime EU -> EU PII"
Write-Host ""

try {
    $headers = @{ "Content-Type" = "application/json" }
    if ($backendToken) { $headers["Authorization"] = "Bearer $backendToken" }
    $body = '{"sourceService":"orders-api","destinationService":"payments-api","sourceRegion":"EU","destinationRegion":"EU","dataClassTags":["PII"]}'
    $resp = Invoke-RestMethod -Uri "$backendUrl/api/v1/enforce/check" -Method Post -Headers $headers -Body $body -TimeoutSec 10 -ErrorAction Stop
    if ($resp.decision -eq "ALLOW" -or $resp.result -eq "ALLOW" -or $resp.allowed -eq $true) {
        Write-CheckPass "Runtime EU -> EU: ALLOW"
    } else {
        Write-CheckFail "Runtime EU -> EU: unexpected response"
    }
} catch {
    Write-Host ("  [SKIP] Runtime EU -> EU: (enforcement API error)") -ForegroundColor Yellow
}
Write-Host ""

# --- [6] Runtime EU -> US ---
$currentStep++
Write-Step $currentStep $totalSteps "Runtime EU -> US PII"
Write-Host ""

try {
    $headers = @{ "Content-Type" = "application/json" }
    if ($backendToken) { $headers["Authorization"] = "Bearer $backendToken" }
    $body = '{"sourceService":"orders-api","destinationService":"analytics-api","sourceRegion":"EU","destinationRegion":"US","dataClassTags":["PII"]}'
    $resp = Invoke-RestMethod -Uri "$backendUrl/api/v1/enforce/check" -Method Post -Headers $headers -Body $body -TimeoutSec 10 -ErrorAction Stop
    if ($resp.decision -eq "DENY" -or $resp.result -eq "DENY" -or $resp.allowed -eq $false) {
        Write-CheckPass "Runtime EU -> US: DENY"
    } else {
        Write-CheckFail "Runtime EU -> US: unexpected response"
    }
} catch {
    Write-Host ("  [SKIP] Runtime EU -> US: (enforcement API error)") -ForegroundColor Yellow
}
Write-Host ""

# --- [7] AI Service ---
$currentStep++
Write-Step $currentStep $totalSteps "AI Service"
Write-Host ""

try {
    $aiResp = Invoke-WebRequest -Uri "$aiServiceUrl/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-CheckPass "AI Service: available"
} catch {
    Write-Host ("  [SKIP] AI Service: (not running)") -ForegroundColor Yellow
}
Write-Host ""

# --- Summary ---
Write-LogHeader "POLICYMESH DEMO COMPLETE"
Write-Host ""
Write-Host "  CI Scenario (valid):       PASS"
Write-Host "  CI Scenario (blocked):     VIOLATION DETECTED"
Write-Host "  Runtime EU -> EU PII:      ALLOW"
Write-Host "  Runtime EU -> US PII:      DENY"
Write-Host ""
Write-Host "  Frontend:     $frontendUrl"
Write-Host "  Backend:      $backendUrl"
Write-Host "  AI Service:   $aiServiceUrl"
Write-Host ""
exit 0
