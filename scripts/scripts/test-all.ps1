# ==============================================================================
# PolicyMesh — Test All (PowerShell)
# ==============================================================================
# Run all available test suites.
# Usage: .\scripts\test-all.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH TEST ALL"
    Write-Host ""
    Write-Host "Usage: .\scripts\test-all.ps1 [options]"
    Write-Host ""
    Write-Host "Run all available test suites."
    Write-Host ""
    Write-CommonHelp
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

$totalSuites = 0; $passedSuites = 0; $failedSuites = 0

# --- Load environment ---
$envFiles = @(
    (Join-Path $script:RepoRoot ".env"),
    (Join-Path $script:RepoRoot "backend\.env")
)
foreach ($f in $envFiles) { Import-EnvFile $f }

Write-LogHeader "POLICYMESH TEST ALL"
Write-Host ""

# --- Backend tests ---
$backendDir = Join-Path $script:RepoRoot "backend"
if ((Test-Path $backendDir) -and (Test-Path (Join-Path $backendDir "pom.xml")) -and (Test-CommandExists "mvn")) {
    $totalSuites++
    Write-LogInfo "Running backend tests..."
    Write-Host ""
    Push-Location $backendDir
    try { mvn test 2>&1; $passedSuites++ }
    catch { $failedSuites++ }
    finally { Pop-Location }
    Write-Host ""
}

# --- CI Checker tests ---
$ciDir = Join-Path $script:RepoRoot "ci-checker"
if (Test-Path $ciDir) {
    $totalSuites++
    Write-LogInfo "Running CI checker tests..."
    Write-Host ""
    Push-Location $ciDir
    try {
        if ((Test-Path (Join-Path $ciDir "pom.xml")) -and (Test-CommandExists "mvn")) {
            mvn test 2>&1; $passedSuites++
        } elseif ((Test-Path (Join-Path $ciDir "requirements.txt")) -and (Test-CommandExists "python")) {
            python -m pytest 2>&1; $passedSuites++
        } else {
            Write-LogWarning "No runnable test found"
            $totalSuites--
        }
    } catch { $failedSuites++ }
    finally { Pop-Location }
    Write-Host ""
}

# --- AI Service tests ---
$aiDir = Join-Path $script:RepoRoot "ai-service"
if ((Test-Path $aiDir) -and (Test-CommandExists "python")) {
    $totalSuites++
    Write-LogInfo "Running AI service tests..."
    Write-Host ""
    Push-Location $aiDir
    try {
        python -m pytest 2>&1; $passedSuites++
    } catch { $failedSuites++ }
    finally { Pop-Location }
    Write-Host ""
}

# --- Frontend tests ---
$frontendDir = Join-Path $script:RepoRoot "frontend"
if ((Test-Path $frontendDir) -and (Test-Path (Join-Path $frontendDir "package.json")) -and (Test-CommandExists "npm")) {
    $totalSuites++
    Write-LogInfo "Running frontend tests..."
    Write-Host ""
    Push-Location $frontendDir
    try { npm test 2>&1; $passedSuites++ }
    catch { $failedSuites++ }
    finally { Pop-Location }
    Write-Host ""
}

# --- Summary ---
Write-Host ""
if ($totalSuites -eq 0) {
    Write-LogWarning "No test suites found."
} elseif ($failedSuites -eq 0) {
    Write-LogSuccess "ALL $totalSuites SUITE(S) PASSED ✅"
} else {
    Write-LogError "$failedSuites of $totalSuites suite(s) FAILED"
}
Write-Host ""

if ($failedSuites -gt 0) { exit 1 }
exit 0
