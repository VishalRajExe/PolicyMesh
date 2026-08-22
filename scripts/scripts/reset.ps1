# ==============================================================================
# PolicyMesh — Reset (PowerShell)
# ==============================================================================
# Destructive reset: stop services, remove containers, optionally remove volumes.
# Usage: .\scripts\reset.ps1 -Force [-Volumes] [-Help]
# ==============================================================================

param(
    [switch]$Force,
    [switch]$Volumes,
    [switch]$NoColor,
    [switch]$Verbose,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH RESET"
    Write-Host ""
    Write-Host "Usage: .\scripts\reset.ps1 [options]"
    Write-Host ""
    Write-Host "Destructive reset: stop services and remove containers."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Force       Required to perform the reset"
    Write-Host "  -Volumes     Also remove data volumes (destroys all data)"
    Write-Host "  -NoColor     Disable colored output"
    Write-Host "  -Verbose     Enable verbose output"
    Write-Host "  -Help        Show this help message"
    Write-Host ""
    Write-Host "WARNING: -Volumes will destroy all PostgreSQL, Redis, and Kafka data."
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

Write-LogHeader "POLICYMESH RESET"
Write-Host ""

# --- Safety check ---
if (-not $Force) {
    Write-LogWarning "This will stop all PolicyMesh services and remove development containers."
    if ($Volumes) {
        Write-LogWarning "⚠  Data volumes WILL be removed (all data will be lost)."
    }
    Write-Host ""
    Write-Host "Run with -Force to execute:"
    Write-Host "  .\scripts\reset.ps1 -Force"
    Write-Host ""
    Write-LogInfo "Dry run — nothing was changed."
    exit 0
}

# --- Load environment ---
$envFiles = @(
    (Join-Path $script:RepoRoot ".env"),
    (Join-Path $script:RepoRoot "backend\.env"),
    (Join-Path $script:RepoRoot "infrastructure\env\.env.dev")
)
foreach ($f in $envFiles) { Import-EnvFile $f }

# --- Step 1: Stop application processes ---
Write-Host ""
Write-LogInfo "Step 1: Stopping application processes..."
& "$PSScriptDir\stop.ps1" -NoColor 2>$null
Write-Host ""

# --- Step 2: Docker Compose down ---
$composeFile = Find-ComposeFile
$composeCmd = Find-ComposeCommand

if ($composeFile -and $composeCmd) {
    Write-LogInfo "Step 2: Removing Docker containers..."
    Write-Host ""

    $downArgs = "down --remove-orphans"
    if ($Volumes) {
        $downArgs += " -v"
        Write-LogWarning "Removing data volumes..."
    }

    Push-Location $script:RepoRoot
    try {
        Invoke-Expression "$composeCmd -f `"$composeFile`" $downArgs" 2>$null
        Write-LogSuccess "Docker containers removed"
    } catch {
        Write-LogWarning "Some containers may not have been removed cleanly"
    } finally {
        Pop-Location
    }
} else {
    Write-LogWarning "No Docker Compose found — skipping container cleanup"
}

Write-Host ""

# --- Step 3: Clean temp files ---
Write-LogInfo "Step 3: Cleaning temporary files..."
$tempFiles = @("pm-backend.log", "pm-backend-err.log", "pm-ai.log", "pm-frontend.log")
foreach ($f in $tempFiles) {
    $path = Join-Path "C:\temp" $f
    if (Test-Path $path) { Remove-Item $path -Force -ErrorAction SilentlyContinue }
}
Write-LogSuccess "Temp files cleaned"

Write-Host ""
Write-LogHeader "POLICYMESH RESET COMPLETE"
Write-Host ""
if ($Volumes) {
    Write-Host "All containers and data volumes have been removed."
} else {
    Write-Host "Containers removed. Data volumes preserved."
    Write-Host "Use -Volumes to also remove data."
}
Write-Host ""
Write-Host "Next: .\scripts\start.ps1"
Write-Host ""
exit 0
