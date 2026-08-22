# PolicyMesh CI Checker - Demo Script (PowerShell)
# This script demonstrates the compliance checker with valid and invalid data flows.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  PolicyMesh CI Checker Demo" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# Build if JAR doesn't exist
if (-not (Test-Path "$ProjectDir\target\policymesh-ci.jar")) {
    Write-Host "Building CI Checker..." -ForegroundColor Yellow
    Push-Location $ProjectDir
    mvn clean package -DskipTests -q
    Pop-Location
    Write-Host ""
}

# Test 1: Valid data flows (should PASS)
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  TEST 1: Valid data flows (EU -> EU)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
java -jar "$ProjectDir\target\policymesh-ci.jar" check `
    --policy-dir "$ProjectDir\policies" `
    --services "$ProjectDir\examples\services.json" `
    --dataflows "$ProjectDir\examples\dataflows-valid.json" `
    --output console
$ExitCode = $LASTEXITCODE
Write-Host ""
Write-Host "Exit code: $ExitCode"
Write-Host ""

# Test 2: Invalid data flows (should FAIL)
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  TEST 2: Invalid data flows (EU PII -> US)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
java -jar "$ProjectDir\target\policymesh-ci.jar" check `
    --policy-dir "$ProjectDir\policies" `
    --services "$ProjectDir\examples\services.json" `
    --dataflows "$ProjectDir\examples\dataflows-invalid.json" `
    --output console
$ExitCode = $LASTEXITCODE
Write-Host ""
Write-Host "Exit code: $ExitCode"
Write-Host ""

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  Demo complete!" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
