param(
    [string]$AdminEmail = $env:SMOKE_ADMIN_EMAIL,
    [string]$AdminPassword = $env:SMOKE_ADMIN_PASSWORD,
    [string]$BaseUrl = "http://localhost:8081",
    [switch]$KeepEnvironment
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot "infra\staging\docker-compose.yml"
$smokeScript = Join-Path $PSScriptRoot "run-runtime-smoke.ps1"
$pwshCommand = (Get-Command pwsh -ErrorAction SilentlyContinue)
$powershellCommand = (Get-Command powershell -ErrorAction SilentlyContinue)
$shellExecutable = if ($pwshCommand) { $pwshCommand.Source } elseif ($powershellCommand) { $powershellCommand.Source } else { throw "PowerShell executable not found." }

if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
    throw "SMOKE_ADMIN_EMAIL required. Provide -AdminEmail or set SMOKE_ADMIN_EMAIL."
}

if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    throw "SMOKE_ADMIN_PASSWORD required. Provide -AdminPassword or set SMOKE_ADMIN_PASSWORD."
}

function Invoke-Compose {
    param([string[]]$ComposeArgs)
    & docker compose -f $composeFile @ComposeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar docker compose: $($ComposeArgs -join ' ')"
    }
}

function Wait-Health {
    param([string]$Url)
    for ($i = 1; $i -le 60; $i++) {
        try {
            $res = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Method GET -Uri "$Url/actuator/health"
            if ([int]$res.StatusCode -eq 200) {
                return
            }
        } catch {}
        Start-Sleep -Seconds 2
    }
    throw "Timeout aguardando health em $Url"
}

$originalBootstrapEnabled = $env:BOOTSTRAP_ADMIN_ENABLED
$originalBootstrapEmail = $env:BOOTSTRAP_ADMIN_EMAIL
$originalBootstrapPassword = $env:BOOTSTRAP_ADMIN_PASSWORD
$originalBootstrapForce = $env:BOOTSTRAP_ADMIN_FORCE_PASSWORD_RESET

try {
    $env:BOOTSTRAP_ADMIN_ENABLED = "true"
    $env:BOOTSTRAP_ADMIN_EMAIL = $AdminEmail
    $env:BOOTSTRAP_ADMIN_PASSWORD = $AdminPassword
    $env:BOOTSTRAP_ADMIN_FORCE_PASSWORD_RESET = "true"

    Write-Host "Subindo stack de staging para smoke runtime..."
    Invoke-Compose @("up", "-d", "--build")

    Write-Host "Aguardando backend saudavel..."
    Wait-Health -Url $BaseUrl

    Write-Host "Executando smoke E2E..."
    & $shellExecutable -ExecutionPolicy Bypass -File $smokeScript -BaseUrl $BaseUrl -Email $AdminEmail -Password $AdminPassword
    if ($LASTEXITCODE -ne 0) {
        throw "Smoke runtime retornou erro."
    }
}
finally {
    if (-not $KeepEnvironment) {
        Write-Host "Derrubando stack de staging..."
        try {
            Invoke-Compose @("down", "-v")
        } catch {
            Write-Host "Falha ao derrubar stack: $($_.Exception.Message)"
        }
    }

    $env:BOOTSTRAP_ADMIN_ENABLED = $originalBootstrapEnabled
    $env:BOOTSTRAP_ADMIN_EMAIL = $originalBootstrapEmail
    $env:BOOTSTRAP_ADMIN_PASSWORD = $originalBootstrapPassword
    $env:BOOTSTRAP_ADMIN_FORCE_PASSWORD_RESET = $originalBootstrapForce
}
