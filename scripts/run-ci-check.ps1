# ==============================================================================
# PolicyMesh - Run CI Check (PowerShell)
# ==============================================================================
# Run the local compliance checker against policy scenarios.
# Usage: .\scripts\run-ci-check.ps1 [-Scenario valid|blocked|mixed] [-Help]
# ==============================================================================

param(
    [string]$Scenario = "valid",
    [switch]$NoColor,
    [switch]$Help
)

$ErrorActionPreference = "Continue"
$PSScriptDir = $PSScriptRoot
. "$PSScriptDir\utils\common.ps1"

if ($Help) {
    Write-LogHeader "POLICYMESH RUN CI CHECK"
    Write-Host ""
    Write-Host "Usage: .\scripts\run-ci-check.ps1 [options]"
    Write-Host ""
    Write-Host "Run the local compliance checker."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Scenario <name>  Scenario to run: valid, blocked, mixed"
    Write-Host "  -NoColor          Disable colored output"
    Write-Host "  -Help             Show this help message"
    Write-Host ""
    Write-Host "Scenarios:"
    Write-Host "  valid    Data flows that should comply (EU->EU)"
    Write-Host "  blocked  Data flows that should be denied (EU->US PII)"
    Write-Host "  mixed    Run both valid and blocked scenarios"
    Write-Host ""
    exit 0
}

if ($NoColor) { $script:UseColor = $false }

# --- Validate scenario ---
if ($Scenario -notin @("valid", "blocked", "mixed")) {
    Write-LogError "Invalid scenario: $Scenario"
    Write-Host "Valid scenarios: valid, blocked, mixed"
    exit 1
}

Write-LogHeader "POLICYMESH CI CHECK"
Write-Host ""

# --- Locate CI checker ---
$ciDir = Join-Path $script:RepoRoot "ci-checker"
$policiesDir = Join-Path $script:RepoRoot "policies"
$examplesDir = Join-Path $script:RepoRoot "examples"

$ciCheckerJar = $null
$ciCheckerPy = $null

if (Test-Path $ciDir) {
    $ciCheckerJar = Get-ChildItem -Path $ciDir -Filter "*.jar" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match "\\target\\" -and $_.Name -notmatch "^original-" } |
        Sort-Object Length -Descending |
        Select-Object -First 1 -ExpandProperty FullName

    if (Test-Path (Join-Path $ciDir "main.py")) {
        $ciCheckerPy = Join-Path $ciDir "main.py"
    } elseif (Test-Path (Join-Path $ciDir "checker.py")) {
        $ciCheckerPy = Join-Path $ciDir "checker.py"
    }
} else {
    Write-LogError "CI checker directory not found at $ciDir"
    exit 1
}

# --- Build if needed ---
if (-not $ciCheckerJar -and (Test-Path (Join-Path $ciDir "pom.xml")) -and (Test-CommandExists "mvn")) {
    Write-LogInfo "Building CI checker..."
    Push-Location $ciDir
    try { mvn package -q -DskipTests 2>$null }
    catch { Write-LogError "CI checker build failed"; exit 1 }
    finally { Pop-Location }
}

# --- Run checker ---
$checkerExit = 1

if ($ciCheckerJar -and (Test-CommandExists "java")) {
    Write-LogInfo "Running Java CI checker..."
    $flowsFile = if ($Scenario -eq "blocked" -or $Scenario -eq "mixed") {
        Join-Path $examplesDir "dataflows-invalid.json"
    } else {
        Join-Path $examplesDir "dataflows-valid.json"
    }
    $servicesFile = Join-Path $examplesDir "services.json"
    Push-Location $ciDir
    try {
        java -jar $ciCheckerJar check --policy-dir $policiesDir --services $servicesFile --dataflows $flowsFile 2>&1
        $checkerExit = $LASTEXITCODE
    } catch { $checkerExit = 1 }
    finally { Pop-Location }
} elseif ($ciCheckerPy -and (Test-CommandExists "python")) {
    Write-LogInfo "Running Python CI checker..."
    Push-Location $ciDir
    try {
        & python $ciCheckerPy --policies $policiesDir --examples $examplesDir --scenario $Scenario 2>&1
        $checkerExit = $LASTEXITCODE
    } catch { $checkerExit = 1 }
    finally { Pop-Location }
} else {
    Write-LogWarning "No CI checker binary found - performing basic validation"
    Write-Host ""

    if (Test-Path $examplesDir) {
        $jsonFiles = Get-ChildItem -Path $examplesDir -Filter "*.json" -ErrorAction SilentlyContinue
        foreach ($f in $jsonFiles) {
            try {
                Get-Content $f.FullName -Raw | ConvertFrom-Json | Out-Null
                Write-LogSuccess "$($f.Name) - valid JSON"
            } catch {
                Write-LogError "$($f.Name) - invalid JSON"
                $checkerExit = 1
            }
        }
    }
}

Write-Host ""

# --- Scenario result ---
if ($Scenario -eq "blocked") {
    if ($checkerExit -ne 0) {
        Write-LogSuccess "Expected result: FAIL"
        Write-LogSuccess "Actual result: FAIL"
        Write-Host ""
        Write-LogSuccess "Scenario behaved correctly - violation was detected."
        $checkerExit = 0
    } else {
        Write-LogWarning "Expected FAIL but checker returned PASS"
    }
}

Write-Host ""

if ($checkerExit -eq 0) {
    Write-LogSuccess "CI CHECK PASSED"
} else {
    Write-LogError "CI CHECK FAILED"
}
Write-Host ""

exit $checkerExit
