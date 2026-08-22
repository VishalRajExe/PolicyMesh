# ==============================================================================
# PolicyMesh — Common PowerShell Utilities
# ==============================================================================
# Shared helpers used by all PolicyMesh scripts.
# . "$PSScriptRoot\utils\common.ps1"
# ==============================================================================

# ------------------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------------------
$script:ScriptRoot = $PSScriptRoot
$script:RepoRoot = (Resolve-Path "$PSScriptRoot\..").Path
$script:WaitTimeoutSeconds = if ($env:WAIT_TIMEOUT_SECONDS) { [int]$env:WAIT_TIMEOUT_SECONDS } else { 120 }
$script:UseColor = $true

# ------------------------------------------------------------------------------
# Color support
# ------------------------------------------------------------------------------
function Initialize-Colors {
    if ($env:NO_COLOR -eq "true" -or $env:NO_COLOR -eq "1" -or -not $Host.UI.SupportsVirtualTerminal) {
        $script:UseColor = $false
    }
    if ($args -contains "--no-color") {
        $script:UseColor = $false
    }
}

function Write-Color {
    param(
        [string]$Text,
        [string]$Color = "White"
    )
    if ($script:UseColor) {
        Write-Host $Text -ForegroundColor $Color
    } else {
        Write-Host $Text
    }
}

# ------------------------------------------------------------------------------
# Logging helpers
# ------------------------------------------------------------------------------
function Write-LogInfo    { param([string]$Message) Write-Color "[INFO]  $Message" "Cyan" }
function Write-LogSuccess { param([string]$Message) Write-Color "[OK]    $Message" "Green" }
function Write-LogWarning { param([string]$Message) Write-Color "[WARN]  $Message" "Yellow" }
function Write-LogError   { param([string]$Message) Write-Color "[ERROR] $Message" "Red" }

function Write-LogHeader {
    param([string]$Title)
    Write-Host ""
    Write-Color "====================================" "White"
    Write-Color ("       {0}" -f $Title) "White"
    Write-Color "====================================" "White"
}

function Write-CheckPass { param([string]$Label) Write-Host ("  ✅ {0,-20}" -f $Label) -ForegroundColor Green }
function Write-CheckFail { param([string]$Label) Write-Host ("  ✗  {0,-20}" -f $Label) -ForegroundColor Red }
function Write-CheckSkip { param([string]$Label) Write-Host ("  ⏭  {0,-20} (not installed)" -f $Label) -ForegroundColor Yellow }

# ------------------------------------------------------------------------------
# Command detection
# ------------------------------------------------------------------------------
function Test-CommandExists {
    param([string]$Command)
    $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

# ------------------------------------------------------------------------------
# Port checking
# ------------------------------------------------------------------------------
function Test-PortInUse {
    param(
        [string]$Host_ = "localhost",
        [int]$Port
    )
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($Host_, $Port)
        $tcp.Close()
        return $true
    } catch {
        return $false
    }
}

# ------------------------------------------------------------------------------
# Wait helpers
# ------------------------------------------------------------------------------
function Wait-ForPort {
    param(
        [string]$Host_ = "localhost",
        [int]$Port,
        [string]$Label = "Port $Port",
        [int]$Timeout = $script:WaitTimeoutSeconds
    )
    $elapsed = 0
    Write-Host "  Waiting for $Label ($Host_:$Port)..." -NoNewline
    while (-not (Test-PortInUse -Host_ $Host_ -Port $Port)) {
        Start-Sleep -Seconds 2
        $elapsed += 2
        if ($elapsed -ge $Timeout) {
            Write-Host " TIMEOUT" -ForegroundColor Red
            Write-LogError "$Label did not become ready within ${Timeout}s."
            return $false
        }
    }
    Write-Host " UP (${elapsed}s)" -ForegroundColor Green
    return $true
}

function Wait-ForHttp {
    param(
        [string]$Url,
        [string]$Label = $Url,
        [int]$Timeout = $script:WaitTimeoutSeconds
    )
    $elapsed = 0
    Write-Host "  Waiting for $Label..." -NoNewline
    while ($true) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -in @(200, 201, 204)) {
                Write-Host " UP (${elapsed}s)" -ForegroundColor Green
                return $true
            }
        } catch {}
        Start-Sleep -Seconds 2
        $elapsed += 2
        if ($elapsed -ge $Timeout) {
            Write-Host " TIMEOUT" -ForegroundColor Red
            Write-LogError "$Label did not become ready within ${Timeout}s."
            return $false
        }
    }
}

function Wait-ForPostgres {
    param(
        [string]$Host_ = "localhost",
        [int]$Port = 5432,
        [int]$Timeout = $script:WaitTimeoutSeconds
    )
    return Wait-ForPort -Host_ $Host_ -Port $Port -Label "PostgreSQL" -Timeout $Timeout
}

function Wait-ForRedis {
    param(
        [string]$Host_ = "localhost",
        [int]$Port = 6379,
        [int]$Timeout = $script:WaitTimeoutSeconds
    )
    return Wait-ForPort -Host_ $Host_ -Port $Port -Label "Redis" -Timeout $Timeout
}

function Wait-ForKafka {
    param(
        [string]$Host_ = "localhost",
        [int]$Port = 9092,
        [int]$Timeout = $script:WaitTimeoutSeconds
    )
    return Wait-ForPort -Host_ $Host_ -Port $Port -Label "Kafka" -Timeout $Timeout
}

# ------------------------------------------------------------------------------
# Docker Compose detection
# ------------------------------------------------------------------------------
function Find-ComposeFile {
    $candidates = @(
        "infrastructure\compose\docker-compose.yml",
        "infrastructure\docker-compose.yml",
        "docker-compose.yml"
    )
    foreach ($candidate in $candidates) {
        $fullPath = Join-Path $script:RepoRoot $candidate
        if (Test-Path $fullPath) {
            return $candidate
        }
    }
    return $null
}

function Find-ComposeCommand {
    if (Test-CommandExists "docker") {
        try {
            $ver = docker compose version 2>$null
            if ($ver) { return "docker compose" }
        } catch {}
    }
    if (Test-CommandExists "docker-compose") {
        return "docker-compose"
    }
    return $null
}

# ------------------------------------------------------------------------------
# Environment loading
# ------------------------------------------------------------------------------
function Import-EnvFile {
    param([string]$EnvFilePath)
    if (Test-Path $EnvFilePath) {
        Write-LogInfo "Loading $EnvFilePath"
        Get-Content $EnvFilePath | ForEach-Object {
            if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
}

# ------------------------------------------------------------------------------
# CI detection
# ------------------------------------------------------------------------------
function Test-IsCI {
    return ($env:CI -eq "true") -or ($env:GITHUB_ACTIONS -eq "true") -or ($env:JENKINS -eq "true")
}

# ------------------------------------------------------------------------------
# Progress helpers
# ------------------------------------------------------------------------------
function Write-Step {
    param(
        [int]$StepNum,
        [int]$Total,
        [string]$Label
    )
    Write-Host ""
    Write-Color ("[{0}/{1}] {2}" -f $StepNum, $Total, $Label) "White"
}

# ------------------------------------------------------------------------------
# Help text helpers
# ------------------------------------------------------------------------------
function Write-CommonHelp {
    Write-Host "Options:"
    Write-Host "  -Help          Show this help message"
    Write-Host "  -NoColor       Disable colored output"
    Write-Host "  -Verbose       Enable verbose output"
}

# Initialize colors on import
Initialize-Colors
