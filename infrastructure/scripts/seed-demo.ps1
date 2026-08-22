$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'start.ps1') -Mode demo
Write-Host 'No backend seed endpoint exists in this repository, so no data was fabricated.'
Write-Host 'Integration point: call the authenticated backend demo-seed endpoint here after it is implemented and healthy.'
