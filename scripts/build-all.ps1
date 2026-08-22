# ==============================================================================
# PolicyMesh — Build All (PowerShell)
# ==============================================================================
# Build all PolicyMesh components.
# Usage: .\scripts\build-all.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH BUILD ALL"
    Write-Host ""
    Write-Host "Usage: .\scripts\build-all.ps1 [options]"
    Write-Host ""
    Write-Host "Build all available components."
    Write-Host ""
    Write-CommonHelp
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

$total = 0; $passed = 0; $failed = 0

Write-LogHeader "POLICYMESH BUILD ALL"
Write-Host ""

# --- Backend ---
$backendDir = Join-Path $script:RepoRoot "backend"
if ((Test-Path $backendDir) -and (Test-Path (Join-Path $backendDir "pom.xml")) -and (Test-CommandExists "mvn")) {
    $total++
    Write-LogInfo "Building backend..."
    Push-Location $backendDir
    try { mvn package -q -DskipTests 2>&1; Write-CheckPass "Backend"; $passed++ }
    catch { Write-CheckFail "Backend"; $failed++ }
    finally { Pop-Location }
}

# --- CI Checker ---
$ciDir = Join-Path $script:RepoRoot "ci-checker"
if ((Test-Path $ciDir) -and (Test-Path (Join-Path $ciDir "pom.xml")) -and (Test-CommandExists "mvn")) {
    $total++
    Write-LogInfo "Building CI checker..."
    Push-Location $ciDir
    try { mvn package -q -DskipTests 2>&1; Write-CheckPass "CI Checker"; $passed++ }
    catch { Write-CheckFail "CI Checker"; $failed++ }
    finally { Pop-Location }
}

# --- AI Service ---
$aiDir = Join-Path $script:RepoRoot "ai-service"
if (Test-Path $aiDir) {
    $total++
    Write-LogInfo "Checking AI service..."
    if ((Test-Path (Join-Path $aiDir "requirements.txt")) -or (Test-Path (Join-Path $aiDir "pyproject.toml"))) {
        Write-CheckPass "AI Service (config OK)"; $passed++
    } else {
        Write-Host ("  ⏭  {0,-16} (no build config)" -f "AI Service") -ForegroundColor Yellow
        $total--
    }
}

# --- Frontend ---
$frontendDir = Join-Path $script:RepoRoot "frontend"
if ((Test-Path $frontendDir) -and (Test-Path (Join-Path $frontendDir "package.json")) -and (Test-CommandExists "npm")) {
    $total++
    Write-LogInfo "Building frontend..."
    Push-Location $frontendDir
    try { npm run build 2>&1; Write-CheckPass "Frontend"; $passed++ }
    catch { Write-CheckFail "Frontend"; $failed++ }
    finally { Pop-Location }
}

# --- Summary ---
Write-Host ""
if ($total -eq 0) {
    Write-LogWarning "No components to build."
} elseif ($failed -eq 0) {
    Write-LogSuccess "ALL $total COMPONENT(S) BUILT SUCCESSFULLY"
} else {
    Write-LogError "$failed of $total build(s) FAILED"
}
Write-Host ""

if ($failed -gt 0) { exit 1 }
exit 0
