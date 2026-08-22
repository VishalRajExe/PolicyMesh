# ============================================
# PolicyMesh Examples Demo Script (PowerShell)
# ============================================
# Runs all example scenarios against the CI checker
# and reports PASS/FAIL for each.
#
# Usage:
#   .\run-demo.ps1 [options]
#
# Options:
#   -JarPath PATH       Path to the CI checker JAR
#   -PolicyDir DIR      Path to policies directory
#   -ServicesFile FILE  Path to services JSON
#   -BackendUrl URL     Backend URL for runtime scenarios
#   -SkipBuild          Skip the Maven build step
#   -RuntimeOnly        Only run runtime scenarios
#   -CiOnly             Only run CI scenarios

param(
    [string]$JarPath = "",
    [string]$PolicyDir = "",
    [string]$ServicesFile = "",
    [string]$BackendUrl = "http://localhost:8080",
    [switch]$SkipBuild,
    [switch]$RuntimeOnly,
    [switch]$CiOnly
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir

# Defaults
if ($PolicyDir -eq "") { $PolicyDir = "$ProjectDir\policies" }
if ($ServicesFile -eq "") { $ServicesFile = "$ScriptDir\services\services.json" }

# Auto-detect JAR
if ($JarPath -eq "") {
    if (Test-Path "$ProjectDir\target\policymesh-ci.jar") {
        $JarPath = "$ProjectDir\target\policymesh-ci.jar"
    } elseif (Test-Path "$ProjectDir\ci-checker\target\policymesh-ci.jar") {
        $JarPath = "$ProjectDir\ci-checker\target\policymesh-ci.jar"
    }
}

$PassCount = 0
$FailCount = 0
$SkipCount = 0
$TotalCount = 0

function Print-Result {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detail = ""
    )
    $script:TotalCount++

    switch ($Status) {
        "PASS" {
            Write-Host "  ✅ PASS  — $Name" -ForegroundColor Green
            $script:PassCount++
        }
        "FAIL" {
            Write-Host "  ❌ FAIL  — $Name" -ForegroundColor Red
            if ($Detail) { Write-Host "           $Detail" -ForegroundColor Gray }
            $script:FailCount++
        }
        "SKIP" {
            Write-Host "  ⏭️  SKIP  — $Name" -ForegroundColor Yellow
            if ($Detail) { Write-Host "           $Detail" -ForegroundColor Gray }
            $script:SkipCount++
        }
    }
}

function Print-Summary {
    Write-Host ""
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host "  Summary" -ForegroundColor Cyan
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host "  Total:   $TotalCount"
    Write-Host "  Passed:  $PassCount" -ForegroundColor Green
    Write-Host "  Failed:  $FailCount" -ForegroundColor $(if ($FailCount -gt 0) { "Red" } else { "Green" })
    Write-Host "  Skipped: $SkipCount" -ForegroundColor Yellow
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host ""
}

# Build if needed
if (-not $SkipBuild -and -not $RuntimeOnly -and $JarPath -eq "") {
    Write-Host "Building CI Checker..." -ForegroundColor Yellow
    Push-Location $ProjectDir
    mvn clean package -DskipTests -q 2>$null
    Pop-Location
    $JarPath = "$ProjectDir\ci-checker\target\policymesh-ci.jar"
    Write-Host ""
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  PolicyMesh Examples Demo" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================
# CI Scenarios
# ============================================
if (-not $RuntimeOnly) {
    Write-Host "--- CI Compliance Scenarios ---" -ForegroundColor Cyan
    Write-Host ""

    # Scenario 1: Valid flow
    Write-Host "Running Scenario: Valid EU-to-EU flow..."
    if ($JarPath -ne "" -and (Test-Path $JarPath)) {
        & java -jar $JarPath check `
            --policy-dir $PolicyDir `
            --services $ServicesFile `
            --dataflows "$ScriptDir\dataflows\valid-flow.json" `
            --output console 2>$null
        if ($LASTEXITCODE -eq 0) {
            Print-Result "Valid EU-to-EU flow" "PASS"
        } else {
            Print-Result "Valid EU-to-EU flow" "FAIL" "Exit code: $LASTEXITCODE"
        }
    } else {
        Print-Result "Valid EU-to-EU flow" "SKIP" "CI checker JAR not found"
    }
    Write-Host ""

    # Scenario 2: Blocked flow
    Write-Host "Running Scenario: Blocked EU-to-US PII flow..."
    if ($JarPath -ne "" -and (Test-Path $JarPath)) {
        & java -jar $JarPath check `
            --policy-dir $PolicyDir `
            --services $ServicesFile `
            --dataflows "$ScriptDir\dataflows\blocked-flow.json" `
            --output console 2>$null
        if ($LASTEXITCODE -eq 1) {
            Print-Result "Blocked EU-to-US PII flow" "PASS" "(Expected failure)"
        } else {
            Print-Result "Blocked EU-to-US PII flow" "FAIL" "Expected exit code 1, got $LASTEXITCODE"
        }
    } else {
        Print-Result "Blocked EU-to-US PII flow" "SKIP" "CI checker JAR not found"
    }
    Write-Host ""

    # Scenario 3: Mixed flow
    Write-Host "Running Scenario: Mixed compliance flows..."
    if ($JarPath -ne "" -and (Test-Path $JarPath)) {
        & java -jar $JarPath check `
            --policy-dir $PolicyDir `
            --services $ServicesFile `
            --dataflows "$ScriptDir\dataflows\mixed-flow.json" `
            --output console 2>$null
        if ($LASTEXITCODE -eq 1) {
            Print-Result "Mixed compliance flows" "PASS" "(Expected failure with 1 violation)"
        } else {
            Print-Result "Mixed compliance flows" "FAIL" "Expected exit code 1, got $LASTEXITCODE"
        }
    } else {
        Print-Result "Mixed compliance flows" "SKIP" "CI checker JAR not found"
    }
    Write-Host ""
}

# ============================================
# Runtime Scenarios
# ============================================
if (-not $CiOnly) {
    Write-Host "--- Runtime Enforcement Scenarios ---" -ForegroundColor Cyan
    Write-Host ""

    $BackendAvailable = $false
    try {
        $null = Invoke-WebRequest -Uri "$BackendUrl/api/v1/actuator/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        $BackendAvailable = $true
    } catch {
        try {
            $null = Invoke-WebRequest -Uri "$BackendUrl/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
            $BackendAvailable = $true
        } catch { }
    }

    if ($BackendAvailable) {
        # Runtime Allow: EU PII to EU
        Write-Host "Running Scenario: Runtime ALLOW (EU PII → EU)..."
        try {
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/enforce/check" `
                -Method Post `
                -ContentType "application/json" `
                -InFile "$ScriptDir\runtime\allow-eu-pii.json"
            if ($response.decision -eq "ALLOW") {
                Print-Result "Runtime ALLOW: EU PII → EU" "PASS"
            } else {
                Print-Result "Runtime ALLOW: EU PII → EU" "FAIL" "Got $($response.decision)"
            }
        } catch {
            Print-Result "Runtime ALLOW: EU PII → EU" "FAIL" $_.Exception.Message
        }
        Write-Host ""

        # Runtime Deny: EU PII to US
        Write-Host "Running Scenario: Runtime DENY (EU PII → US)..."
        try {
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/enforce/check" `
                -Method Post `
                -ContentType "application/json" `
                -InFile "$ScriptDir\runtime\deny-eu-pii-us.json"
            if ($response.decision -eq "DENY") {
                Print-Result "Runtime DENY: EU PII → US" "PASS"
            } else {
                Print-Result "Runtime DENY: EU PII → US" "FAIL" "Got $($response.decision)"
            }
        } catch {
            Print-Result "Runtime DENY: EU PII → US" "FAIL" $_.Exception.Message
        }
        Write-Host ""

        # Runtime Deny: EU PII to CN
        Write-Host "Running Scenario: Runtime DENY (EU PII → CN)..."
        try {
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/enforce/check" `
                -Method Post `
                -ContentType "application/json" `
                -InFile "$ScriptDir\runtime\deny-eu-pii-cn.json"
            if ($response.decision -eq "DENY") {
                Print-Result "Runtime DENY: EU PII → CN" "PASS"
            } else {
                Print-Result "Runtime DENY: EU PII → CN" "FAIL" "Got $($response.decision)"
            }
        } catch {
            Print-Result "Runtime DENY: EU PII → CN" "FAIL" $_.Exception.Message
        }
        Write-Host ""

        # Runtime Deny: India PII to US
        Write-Host "Running Scenario: Runtime DENY (India PII → US)..."
        try {
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/enforce/check" `
                -Method Post `
                -ContentType "application/json" `
                -InFile "$ScriptDir\runtime\deny-india-pii-us.json"
            if ($response.decision -eq "DENY") {
                Print-Result "Runtime DENY: India PII → US" "PASS"
            } else {
                Print-Result "Runtime DENY: India PII → US" "FAIL" "Got $($response.decision)"
            }
        } catch {
            Print-Result "Runtime DENY: India PII → US" "FAIL" $_.Exception.Message
        }
        Write-Host ""

        # Runtime Allow: EU PCI to EU
        Write-Host "Running Scenario: Runtime ALLOW (EU PCI → EU)..."
        try {
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/enforce/check" `
                -Method Post `
                -ContentType "application/json" `
                -InFile "$ScriptDir\runtime\allow-eu-pci-eu.json"
            if ($response.decision -eq "ALLOW") {
                Print-Result "Runtime ALLOW: EU PCI → EU" "PASS"
            } else {
                Print-Result "Runtime ALLOW: EU PCI → EU" "FAIL" "Got $($response.decision)"
            }
        } catch {
            Print-Result "Runtime ALLOW: EU PCI → EU" "FAIL" $_.Exception.Message
        }
        Write-Host ""
    } else {
        Print-Result "Runtime scenarios" "SKIP" "Backend not available at $BackendUrl"
        Write-Host ""
    }
}

Print-Summary
