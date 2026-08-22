# ==============================================================================
# PolicyMesh — Stop (PowerShell)
# ==============================================================================
# Stop PolicyMesh services. Non-destructive (does not remove volumes).
# Usage: .\scripts\stop.ps1 [-Help]
# ==============================================================================

param(
    [switch]$NoColor,
    [switch]$Verbose,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH STOP"
    Write-Host ""
    Write-Host "Usage: .\scripts\stop.ps1 [options]"
    Write-Host ""
    Write-Host "Stop all PolicyMesh services."
    Write-Host "This is non-destructive: data volumes are preserved."
    Write-Host ""
    Write-CommonHelp
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

Write-LogHeader "POLICYMESH STOP"
Write-Host ""

# --- Load environment ---
$envFiles = @(
    (Join-Path $script:RepoRoot ".env"),
    (Join-Path $script:RepoRoot "backend\.env"),
    (Join-Path $script:RepoRoot "infrastructure\env\.env.dev")
)
foreach ($f in $envFiles) { Import-EnvFile $f }

# --- Kill application processes ---
Write-LogInfo "Stopping application processes..."
Write-Host ""

$killed = 0

# Backend
$backendProcs = Get-Process -Name "java" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "spring-boot" }
if ($backendProcs) {
    $backendProcs | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-LogSuccess "Stopped backend (Java/Maven)"
    $killed++
}

# AI Service
$aiProcs = Get-Process -Name "python*","uvicorn" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "uvicorn" }
if ($aiProcs) {
    $aiProcs | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-LogSuccess "Stopped AI service"
    $killed++
}

# Frontend
$feProcs = Get-Process -Name "node" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match "vite|next|react-scripts" }
if ($feProcs) {
    $feProcs | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-LogSuccess "Stopped frontend"
    $killed++
}

if ($killed -eq 0) {
    Write-LogInfo "No application processes found running"
}

Write-Host ""

# --- Stop Docker Compose ---
$composeFile = Find-ComposeFile
$composeCmd = Find-ComposeCommand

if ($composeFile -and $composeCmd) {
    Write-LogInfo "Stopping Docker infrastructure..."
    Write-Host ""

    Push-Location $script:RepoRoot
    try {
        Invoke-Expression "$composeCmd -f `"$composeFile`" stop" 2>$null
        Write-LogSuccess "Docker services stopped"
    } catch {
        Write-LogWarning "Docker Compose stop encountered issues"
    } finally {
        Pop-Location
    }
} else {
    Write-LogWarning "No Docker Compose configuration found — skipping"
}

Write-Host ""
Write-LogSuccess "PolicyMesh stopped."
Write-Host ""
Write-Host "Volumes and data are preserved."
Write-Host "Use .\scripts\reset.ps1 -Force to remove data."
Write-Host ""
exit 0
