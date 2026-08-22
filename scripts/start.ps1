# ==============================================================================
# PolicyMesh — Start (PowerShell)
# ==============================================================================
# Start PolicyMesh locally.
# Usage: .\scripts\start.ps1 [-InfraOnly] [-BackendOnly] [-Demo] [-Help]
# ==============================================================================

param(
    [switch]$InfraOnly,
    [switch]$BackendOnly,
    [switch]$Demo,
    [switch]$NoColor,
    [switch]$Verbose,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

$StartBackend = $true
$StartFrontend = $true
$StartAiService = $true
$StartInfra = $true

if ($Help) {
    Write-LogHeader "POLICYMESH START"
    Write-Host ""
    Write-Host "Usage: .\scripts\start.ps1 [options]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -InfraOnly    Start only Docker infrastructure"
    Write-Host "  -BackendOnly  Start infra + backend only"
    Write-Host "  -Demo         Start the demo stack"
    Write-Host "  -NoColor      Disable colored output"
    Write-Host "  -Verbose      Enable verbose output"
    Write-Host "  -Help         Show this help message"
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

if ($InfraOnly) {
    $StartBackend = $false
    $StartFrontend = $false
    $StartAiService = $false
}

if ($BackendOnly) {
    $StartFrontend = $false
    $StartAiService = $false
}

if ($Demo) {
    $StartBackend = $true
    $StartFrontend = $true
    $StartAiService = $true
}

# --- Load environment ---
$envFiles = @(
    (Join-Path $script:RepoRoot ".env"),
    (Join-Path $script:RepoRoot "backend\.env"),
    (Join-Path $script:RepoRoot "infrastructure\env\.env.dev")
)
foreach ($f in $envFiles) { Import-EnvFile $f }

# --- Detect compose ---
$composeFile = Find-ComposeFile
$composeCmd = Find-ComposeCommand

if (-not $composeFile) {
    Write-LogWarning "No docker-compose.yml found — Docker infrastructure will not be started."
}
if (-not $composeCmd) {
    Write-LogWarning "Docker Compose not found — Docker infrastructure will not be started."
}

# --- Port conflict detection ---
Write-Host ""
Write-LogInfo "Checking port availability..."
Write-Host ""

$portLabels = @{
    5432 = "PostgreSQL"
    6379 = "Redis"
    9092 = "Kafka"
    8080 = "Backend"
    8000 = "AI Service"
    5173 = "Frontend"
}

$portConflicts = 0
foreach ($port in $portLabels.Keys) {
    if (Test-PortInUse -Port $port) {
        Write-LogWarning "Port $port ($($portLabels[$port])) is already in use."
        $portConflicts++
    }
}

if ($portConflicts -gt 0) {
    Write-Host ""
    Write-LogWarning "Some ports are in use. Services may already be running."
    Write-LogInfo "Run .\scripts\health-check.ps1 to check status."
    Write-Host ""
}

# --- Start Docker infrastructure ---
$totalSteps = 3
$currentStep = 0

$backendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://localhost:8080" }
$aiServiceUrl = if ($env:AI_SERVICE_URL) { $env:AI_SERVICE_URL } else { "http://localhost:8000" }
$frontendUrl = if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://localhost:5173" }

if ($composeFile -and $composeCmd) {
    $currentStep++
    Write-Step $currentStep $totalSteps "Starting Docker infrastructure"
    Write-Host ""

    $composeFull = "$composeCmd -f `"$script:RepoRoot\$composeFile`""
    $cmd = "$composeFull up -d"

    try {
        Push-Location $script:RepoRoot
        Invoke-Expression $cmd 2>$null
        Write-LogSuccess "Docker infrastructure started"
    } catch {
        Write-LogError "Docker Compose failed to start."
        Write-Host ""
        Write-Host "Diagnostics:"
        Invoke-Expression "$composeFull ps" 2>$null
        exit 1
    } finally {
        Pop-Location
    }
    Write-Host ""

    # Wait for infrastructure services
    $currentStep++
    Write-Step $currentStep $totalSteps "Waiting for infrastructure services"
    Write-Host ""

    $pgPort = if ($env:POSTGRES_PORT) { [int]$env:POSTGRES_PORT } else { 5432 }
    $redisPort = if ($env:REDIS_PORT) { [int]$env:REDIS_PORT } else { 6379 }
    $kafkaPort = if ($env:KAFKA_PORT) { [int]$env:KAFKA_PORT } else { 9092 }

    $pgOk = Wait-ForPostgres -Port $pgPort
    if (-not $pgOk) {
        Write-LogError "PostgreSQL failed to start."
        exit 1
    }

    $redisOk = Wait-ForRedis -Port $redisPort
    if (-not $redisOk) {
        Write-LogError "Redis failed to start."
        exit 1
    }

    $kafkaOk = Wait-ForKafka -Port $kafkaPort
    if (-not $kafkaOk) {
        Write-LogError "Kafka failed to start."
        exit 1
    }

    Write-Host ""
}

# --- Start backend ---
if ($StartBackend -and (Test-Path (Join-Path $script:RepoRoot "backend"))) {
    $currentStep++
    Write-Step $currentStep $totalSteps "Starting backend"
    Write-Host ""

    $backendDir = Join-Path $script:RepoRoot "backend"
    if ((Test-Path (Join-Path $backendDir "pom.xml")) -and (Test-CommandExists "mvn")) {
        Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-q" -WorkingDirectory $backendDir -WindowStyle Hidden -RedirectStandardOutput "C:\temp\pm-backend.log" -RedirectStandardError "C:\temp\pm-backend-err.log" 2>$null
        Write-LogInfo "Backend starting in background..."
        Wait-ForHttp -Url "$backendUrl/actuator/health" -Label "Backend"
    } elseif ((Test-Path (Join-Path $backendDir "package.json")) -and (Test-CommandExists "npm")) {
        Start-Process -FilePath "npm" -ArgumentList "start" -WorkingDirectory $backendDir -WindowStyle Hidden
        Write-LogInfo "Backend starting in background..."
        Wait-ForHttp -Url $backendUrl -Label "Backend"
    } else {
        Write-LogWarning "No runnable backend found"
    }
    Write-Host ""
}

# --- Start AI service ---
if ($StartAiService -and (Test-Path (Join-Path $script:RepoRoot "ai-service"))) {
    $currentStep++
    Write-Step $currentStep $totalSteps "Starting AI service"
    Write-Host ""

    $aiDir = Join-Path $script:RepoRoot "ai-service"
    if ((Test-CommandExists "python") -or (Test-CommandExists "python3")) {
        $pythonCmd = if (Test-CommandExists "python3") { "python3" } else { "python" }
        Start-Process -FilePath $pythonCmd -ArgumentList "-m","uvicorn","main:app","--host","0.0.0.0","--port","8000" -WorkingDirectory $aiDir -WindowStyle Hidden
        Write-LogInfo "AI service starting in background..."
        Wait-ForHttp -Url $aiServiceUrl -Label "AI Service"
    } else {
        Write-LogWarning "No runnable AI service found (python not installed)"
    }
    Write-Host ""
}

# --- Start frontend ---
if ($StartFrontend -and (Test-Path (Join-Path $script:RepoRoot "frontend"))) {
    $currentStep++
    Write-Step $currentStep $totalSteps "Starting frontend"
    Write-Host ""

    $feDir = Join-Path $script:RepoRoot "frontend"
    if ((Test-Path (Join-Path $feDir "package.json")) -and (Test-CommandExists "npm")) {
        Start-Process -FilePath "npm" -ArgumentList "run","dev" -WorkingDirectory $feDir -WindowStyle Hidden
        Write-LogInfo "Frontend starting in background..."
        Wait-ForHttp -Url $frontendUrl -Label "Frontend"
    } else {
        Write-LogWarning "No runnable frontend found"
    }
    Write-Host ""
}

# --- Final status ---
Write-LogHeader "POLICYMESH READY"
Write-Host ""
Write-Host "Frontend:     $frontendUrl"
Write-Host "Backend:      $backendUrl"
Write-Host "AI Service:   $aiServiceUrl"
Write-Host ""
Write-Host "PostgreSQL:   ✅"
Write-Host "Redis:        ✅"
Write-Host "Kafka:        ✅"
Write-Host ""
Write-Host "Next: .\scripts\health-check.ps1"
Write-Host ""
exit 0
