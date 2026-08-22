# ==============================================================================
# PolicyMesh — Setup (PowerShell)
# ==============================================================================
# Prepare a fresh development environment.
# Usage: .\scripts\setup.ps1 [-NoInteractive] [-Help]
# ==============================================================================

param(
    [switch]$NoInteractive,
    [switch]$NoColor,
    [switch]$Verbose,
    [switch]$Help
)

$ErrorActionPreference = "Stop"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH SETUP"
    Write-Host ""
    Write-Host "Usage: .\scripts\setup.ps1 [options]"
    Write-Host ""
    Write-CommonHelp
    Write-Host "  -NoInteractive  Fail instead of prompting for missing config"
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

# --- Check prerequisites ---
Write-LogHeader "POLICYMESH SETUP"
Write-Host ""

$missing = 0

function Test-Tool {
    param([string]$Name, [bool]$Required = $true)
    if (Test-CommandExists $Name) {
        Write-CheckPass $Name
    } elseif ($Required) {
        Write-CheckFail $Name
        $script:missing++
    } else {
        Write-CheckSkip $Name
    }
}

Write-LogInfo "Checking prerequisites..."
Write-Host ""

Test-Tool "git"
Test-Tool "docker"
Test-Tool "java"
Test-Tool "mvn"
Test-Tool "node"
Test-Tool "npm"
Test-Tool "python"
Test-Tool "pip"

# Docker Compose
$composeCmd = Find-ComposeCommand
if ($composeCmd) {
    Write-CheckPass "Docker Compose"
} else {
    Write-CheckFail "Docker Compose"
    $script:missing++
}

Write-Host ""

if ($missing -gt 0) {
    Write-LogError "$missing required tool(s) missing."
    Write-Host ""
    Write-Host "Install the missing tools above and re-run: .\scripts\setup.ps1"
    exit 1
}

# --- Environment files ---
Write-Host ""
Write-LogInfo "Setting up environment files..."
Write-Host ""

function Copy-EnvIfMissing {
    param([string]$ExamplePath, [string]$TargetPath, [string]$Label)
    if (Test-Path $ExamplePath) {
        if (Test-Path $TargetPath) {
            Write-LogSuccess "$Label already exists — skipping"
        } else {
            Copy-Item $ExamplePath $TargetPath
            Write-LogSuccess "$Label created from example"
        }
    }
}

$rootEnv = Join-Path $script:RepoRoot ".env.example"
$rootTarget = Join-Path $script:RepoRoot ".env"
Copy-EnvIfMissing -ExamplePath $rootEnv -TargetPath $rootTarget -Label ".env"

$backendEnv = Join-Path $script:RepoRoot "backend\.env.example"
$backendTarget = Join-Path $script:RepoRoot "backend\.env"
Copy-EnvIfMissing -ExamplePath $backendEnv -TargetPath $backendTarget -Label "backend/.env"

$infraEnv = Join-Path $script:RepoRoot "infrastructure\env\.env.dev.example"
$infraTarget = Join-Path $script:RepoRoot "infrastructure\env\.env.dev"
Copy-EnvIfMissing -ExamplePath $infraEnv -TargetPath $infraTarget -Label "infrastructure/env/.env.dev"

# --- Install dependencies ---
Write-Host ""
Write-LogInfo "Installing project dependencies..."
Write-Host ""

# Backend
$backendDir = Join-Path $script:RepoRoot "backend"
if ((Test-Path $backendDir) -and (Test-Tool "mvn" -Quiet)) {
    Write-LogInfo "Building backend..."
    Push-Location $backendDir
    try { mvn dependency:resolve -q 2>$null; Write-LogSuccess "Backend dependencies resolved" }
    catch { Write-LogWarning "Backend dependency resolution failed" }
    finally { Pop-Location }
}

# Frontend
$frontendDir = Join-Path $script:RepoRoot "frontend"
if ((Test-Path $frontendDir) -and (Test-Tool "npm" -Quiet)) {
    Write-LogInfo "Installing frontend dependencies..."
    Push-Location $frontendDir
    try { npm install --silent 2>$null; Write-LogSuccess "Frontend dependencies installed" }
    catch { Write-LogWarning "Frontend install failed" }
    finally { Pop-Location }
}

# AI Service
$aiDir = Join-Path $script:RepoRoot "ai-service"
if ((Test-Path $aiDir) -and (Test-Tool "pip" -Quiet)) {
    $reqFile = Join-Path $aiDir "requirements.txt"
    if (Test-Path $reqFile) {
        Write-LogInfo "Installing AI service dependencies..."
        Push-Location $aiDir
        try { pip install -q -r requirements.txt 2>$null; Write-LogSuccess "AI service dependencies installed" }
        catch { Write-LogWarning "AI service install failed" }
        finally { Pop-Location }
    }
}

# --- Done ---
Write-Host ""
Write-LogHeader "POLICYMESH SETUP COMPLETE"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Review generated .env files and set secrets"
Write-Host "  2. .\scripts\start.ps1"
Write-Host "  3. .\scripts\health-check.ps1"
Write-Host ""
exit 0
