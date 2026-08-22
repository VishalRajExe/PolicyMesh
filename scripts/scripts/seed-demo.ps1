# ==============================================================================
# PolicyMesh — Seed Demo Data (PowerShell)
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

Write-LogHeader "POLICYMESH DEMO SEED"
Write-Host ""

# --- Check backend ---
Write-LogInfo "Checking backend availability..."
try {
    $resp = Invoke-WebRequest -Uri $backendUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-LogSuccess "Backend is up"
} catch {
    Write-LogError "Backend is not reachable at $backendUrl"
    Write-Host ""
    Write-Host "Start the backend first:"
    Write-Host "  .\scripts\start.ps1"
    exit 1
}
Write-Host ""

# --- Try demo/seed endpoint ---
Write-LogInfo "Attempting backend demo seed endpoint..."
$seedBody = '{"scenario":"hackathon"}'
try {
    $seedResp = Invoke-ApiPost -Endpoint "/api/v1/demo/seed" -Body $seedBody
    if ($seedResp -and $seedResp.status -in @("ok","created")) {
        Write-LogSuccess "Demo seed endpoint available — using backend seed"
        $seedResp | ConvertTo-Json -Depth 5
        Write-Host ""
        Write-LogSuccess "Demo data loaded via backend API."
        exit 0
    }
} catch {}

# --- Manual seeding ---
Write-LogInfo "Using individual API endpoints..."
Write-Host ""

# Policy
Write-LogInfo "Creating EU PII policy..."
$policyBody = @{
    id = "EU-PII-001"
    name = "EU PII Data Protection"
    description = "Restricts EU PII from leaving EU jurisdictions"
    type = "data-classification"
    rules = @(
        @{ condition = 'source.region == "EU" AND data.classification == "PII"'; action = "DENY"; reason = "EU PII must not be transferred to non-EU regions" }
        @{ condition = 'source.region == "EU" AND target.region == "EU"'; action = "ALLOW"; reason = "EU to EU PII transfer allowed" }
    )
} | ConvertTo-Json -Depth 5
Invoke-ApiPost -Endpoint "/api/v1/policies" -Body $policyBody | Out-Null
Write-LogSuccess "✅ EU-PII-001"

# Services
Write-LogInfo "Creating services..."
Invoke-ApiPost -Endpoint "/api/v1/services" -Body '{"id":"orders-api","name":"Orders API","region":"EU"}' | Out-Null
Write-LogSuccess "✅ orders-api (EU)"

Invoke-ApiPost -Endpoint "/api/v1/services" -Body '{"id":"payments-api","name":"Payments API","region":"EU"}' | Out-Null
Write-LogSuccess "✅ payments-api (EU)"

Invoke-ApiPost -Endpoint "/api/v1/services" -Body '{"id":"analytics-api","name":"Analytics API","region":"US"}' | Out-Null
Write-LogSuccess "✅ analytics-api (US)"

# Data flows
Write-LogInfo "Creating data flows..."
Invoke-ApiPost -Endpoint "/api/v1/data-flows" -Body '{"source":"orders-api","target":"payments-api","dataClassification":"PII"}' | Out-Null
Write-LogSuccess "✅ orders → payments"

Invoke-ApiPost -Endpoint "/api/v1/data-flows" -Body '{"source":"orders-api","target":"analytics-api","dataClassification":"PII"}' | Out-Null
Write-LogSuccess "✅ orders → analytics"

# --- Done ---
Write-Host ""
Write-LogHeader "POLICYMESH DEMO SEED COMPLETE"
Write-Host ""
Write-Host "Scenario:"
Write-Host "  EU-PII-001   EU PII Protection Policy"
Write-Host ""
Write-Host "  orders-api     EU"
Write-Host "  payments-api   EU"
Write-Host "  analytics-api  US"
Write-Host ""
Write-Host "  orders → payments   (EU→EU, should ALLOW)"
Write-Host "  orders → analytics  (EU→US, should DENY)"
Write-Host ""
Write-Host "Next: .\scripts\run-demo.ps1"
Write-Host ""
exit 0
