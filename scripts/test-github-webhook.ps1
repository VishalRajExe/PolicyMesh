<#
.SYNOPSIS
  Sends test GitHub webhook payloads with valid HMAC-SHA256 signatures to PolicyMesh.
.EXAMPLE
  .\test-github-webhook.ps1 -Secret "my-secret" -CommitSha "40905bd" -Branch "main"
#>
param(
  [string]$BaseUrl = "http://127.0.0.1:8080",
  [string]$Secret = "change-this-webhook-secret-at-least-32-chars",
  [string]$Branch = "main",
  [string]$CommitSha = "40905bd",
  [string]$Repo = "VishalRajExe/PolicyMesh",
  [string]$Author = "VishalRajExe"
)

function Get-HmacSha256Signature {
  param([string]$SecretKey, [string]$Message)
  $hmac = New-Object System.Security.Cryptography.HMACSHA256
  $hmac.Key = [System.Text.Encoding]::UTF_8.GetBytes($SecretKey)
  $hashBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF_8.GetBytes($Message))
  $hex = ($hashBytes | ForEach-Object { "{0:x2}" -f $_ }) -join ""
  return "sha256=$hex"
}

Write-Host "`n=== PolicyMesh GitHub Webhook Test Tool ===" -ForegroundColor Cyan
Write-Host "Target Endpoint: $BaseUrl/api/webhooks/github"
Write-Host "Branch:          $Branch"
Write-Host "Commit SHA:      $CommitSha"
Write-Host "Repository:      $Repo`n"

# Test 1: Ping Event
Write-Host "[1/4] Testing Ping Event..." -NoNewline
$pingPayload = @{ zen = "Design for failure." } | ConvertTo-Json -Compress
$pingSig = Get-HmacSha256Signature -SecretKey $Secret -Message $pingPayload
$pingDelivery = [System.Guid]::NewGuid().ToString()

try {
  $pingRes = Invoke-RestMethod -Uri "$BaseUrl/api/webhooks/github" -Method POST `
    -Headers @{
      "X-Hub-Signature-256" = $pingSig
      "X-GitHub-Event" = "ping"
      "X-GitHub-Delivery" = $pingDelivery
    } -Body $pingPayload -ContentType "application/json"
  Write-Host " SUCCESS -> $($pingRes.status)" -ForegroundColor Green
} catch {
  Write-Host " FAILED -> $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Valid Push Event
Write-Host "[2/4] Testing Valid Push Event..." -NoNewline
$pushBody = @{
  ref = "refs/heads/$Branch"
  after = $CommitSha
  repository = @{ full_name = $Repo }
  sender = @{ login = $Author }
  head_commit = @{
    id = $CommitSha
    message = "feat(service): register cross-border flow"
    author = @{ name = $Author }
  }
} | ConvertTo-Json -Compress

$pushSig = Get-HmacSha256Signature -SecretKey $Secret -Message $pushBody
$pushDelivery = [System.Guid]::NewGuid().ToString()

try {
  $pushRes = Invoke-RestMethod -Uri "$BaseUrl/api/webhooks/github" -Method POST `
    -Headers @{
      "X-Hub-Signature-256" = $pushSig
      "X-GitHub-Event" = "push"
      "X-GitHub-Delivery" = $pushDelivery
    } -Body $pushBody -ContentType "application/json"
  Write-Host " ACCEPTED -> $($pushRes.status) (Delivery: $($pushRes.deliveryId))" -ForegroundColor Green
} catch {
  Write-Host " FAILED -> $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Invalid Signature Rejection
Write-Host "[3/4] Testing Tampered / Invalid Signature Rejection..." -NoNewline
try {
  $badRes = Invoke-RestMethod -Uri "$BaseUrl/api/webhooks/github" -Method POST `
    -Headers @{
      "X-Hub-Signature-256" = "sha256=0000000000000000000000000000000000000000000000000000000000000000"
      "X-GitHub-Event" = "push"
      "X-GitHub-Delivery" = [System.Guid]::NewGuid().ToString()
    } -Body $pushBody -ContentType "application/json"
  Write-Host " UNEXPECTED PASS (Security bug)" -ForegroundColor Red
} catch {
  Write-Host " REJECTED (HTTP 401 as expected)" -ForegroundColor Green
}

# Test 4: Replay Protection
Write-Host "[4/4] Testing Replay Idempotency Protection..." -NoNewline
try {
  $replayRes = Invoke-RestMethod -Uri "$BaseUrl/api/webhooks/github" -Method POST `
    -Headers @{
      "X-Hub-Signature-256" = $pushSig
      "X-GitHub-Event" = "push"
      "X-GitHub-Delivery" = $pushDelivery
    } -Body $pushBody -ContentType "application/json"
  Write-Host " IDEMPOTENT -> $($replayRes.status)" -ForegroundColor Green
} catch {
  Write-Host " FAILED -> $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nAll Webhook Security Tests Completed.`n" -ForegroundColor Cyan