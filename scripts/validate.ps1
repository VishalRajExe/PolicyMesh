# ==============================================================================
# PolicyMesh — Validate (PowerShell)
# ==============================================================================
# Validate repository configuration without starting services.
# Usage: .\scripts\validate.ps1 [-Verbose] [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Verbose_,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH VALIDATE"
    Write-Host ""
    Write-Host "Usage: .\scripts\validate.ps1 [options]"
    Write-Host ""
    Write-Host "Validate repository configuration."
    Write-Host ""
    Write-CommonHelp
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

$total = 0; $passed = 0; $failed = 0

function Confirm-Validation {
    param([string]$Label)
    $script:total++; $script:passed++
    Write-CheckPass $Label
}

function Confirm-Fail {
    param([string]$Label)
    $script:total++; $script:failed++
    Write-CheckFail $Label
}

function Confirm-Skip {
    param([string]$Label)
    Write-Host ("  ⏭  {0,-24} (not found)" -f $Label) -ForegroundColor Yellow
}

Write-LogHeader "POLICYMESH VALIDATE"
Write-Host ""

# --- Docker Compose ---
Write-LogInfo "Docker Compose..."
$composeFile = Find-ComposeFile
if ($composeFile) {
    Confirm-Validation "docker-compose.yml found"
} else {
    Confirm-Skip "docker-compose.yml"
}

# --- JSON files ---
Write-LogInfo "JSON example files..."
$jsonCount = 0
$jsonErrors = 0

$examplesDir = Join-Path $script:RepoRoot "examples"
$policiesDir = Join-Path $script:RepoRoot "policies"

foreach ($dir in @($examplesDir, $policiesDir)) {
    if (Test-Path $dir) {
        Get-ChildItem -Path $dir -Filter "*.json" -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
            $jsonCount++
            try {
                Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null
            } catch {
                $jsonErrors++
            }
        }
    }
}

if ($jsonCount -eq 0) {
    Confirm-Skip "JSON files (none found)"
} elseif ($jsonErrors -eq 0) {
    Confirm-Validation "JSON files ($jsonCount valid)"
} else {
    Confirm-Fail "JSON files ($jsonErrors of $jsonCount invalid)"
}

# --- Environment ---
Write-LogInfo "Environment files..."
$rootEnvEx = Join-Path $script:RepoRoot ".env.example"
$rootEnv = Join-Path $script:RepoRoot ".env"
if (Test-Path $rootEnvEx) {
    if (Test-Path $rootEnv) {
        Confirm-Validation ".env exists"
    } else {
        Confirm-Fail ".env (copy from .env.example)"
    }
} else {
    Confirm-Skip ".env.example"
}

# --- Backend ---
Write-LogInfo "Backend..."
$backendDir = Join-Path $script:RepoRoot "backend"
if (Test-Path $backendDir) {
    if (Test-Path (Join-Path $backendDir "pom.xml")) {
        Confirm-Validation "Backend config found (Maven)"
    } elseif (Test-Path (Join-Path $backendDir "package.json")) {
        Confirm-Validation "Backend config found (Node)"
    } else {
        Confirm-Skip "Backend (no build file)"
    }
} else {
    Confirm-Skip "Backend"
}

# --- Frontend ---
Write-LogInfo "Frontend..."
$frontendDir = Join-Path $script:RepoRoot "frontend"
if ((Test-Path $frontendDir) -and (Test-Path (Join-Path $frontendDir "package.json"))) {
    Confirm-Validation "Frontend config found"
} else {
    Confirm-Skip "Frontend"
}

# --- AI Service ---
Write-LogInfo "AI Service..."
$aiDir = Join-Path $script:RepoRoot "ai-service"
if (Test-Path $aiDir) {
    if ((Test-Path (Join-Path $aiDir "requirements.txt")) -or (Test-Path (Join-Path $aiDir "pyproject.toml"))) {
        Confirm-Validation "AI service config found"
    } else {
        Confirm-Skip "AI service (no requirements.txt)"
    }
} else {
    Confirm-Skip "AI Service"
}

# --- CI Checker ---
Write-LogInfo "CI Checker..."
$ciDir = Join-Path $script:RepoRoot "ci-checker"
if (Test-Path $ciDir) {
    Confirm-Validation "CI checker directory found"
} else {
    Confirm-Skip "CI Checker"
}

# --- Summary ---
Write-Host ""
if ($failed -eq 0) {
    Write-LogSuccess "All validations passed ($total checks)."
} else {
    Write-LogWarning "$passed/$total passed. ($failed failed)"
}
Write-Host ""

if ($failed -gt 0) { exit 1 }
exit 0
