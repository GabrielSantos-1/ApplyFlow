param(
    [int]$TotalRequests = 200,
    [int]$Concurrency = 30,
    [switch]$KeepEnvironment
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot "infra\staging\docker-compose.yml"

function Invoke-Compose {
    param([string[]]$ComposeArgs)
    Write-Host ("docker compose -f `"{0}`" {1}" -f $composeFile, ($ComposeArgs -join " "))
    & docker compose -f $composeFile @ComposeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar docker compose: $($ComposeArgs -join ' ')"
    }
}

function Invoke-HttpStatus {
    param(
        [string]$Method = "GET",
        [string]$Uri,
        [string]$ContentType,
        [string]$Body
    )

    try {
        if ($Method -eq "GET") {
            $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Method Get -Uri $Uri
        } else {
            $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Method $Method -Uri $Uri -ContentType $ContentType -Body $Body
        }
        return @{
            StatusCode = [int]$response.StatusCode
            Headers = $response.Headers
        }
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $rawStatus = $_.Exception.Response.StatusCode
            $statusCode = [int]$rawStatus
            $headers = @{}
            try {
                foreach ($name in $_.Exception.Response.Headers.AllKeys) {
                    $headers[$name] = $_.Exception.Response.Headers[$name]
                }
            } catch {}
            return @{
                StatusCode = $statusCode
                Headers = $headers
            }
        }
        return @{
            StatusCode = 0
            Headers = @{}
        }
    }
}

Write-Host "Subindo stack de staging..."
Invoke-Compose @("up", "-d", "--build")

try {
    Write-Host "Aguardando backend-1 e backend-2 ficarem saudaveis..."
    $maxAttempts = 60
    for ($i = 1; $i -le $maxAttempts; $i++) {
        $r1 = Invoke-HttpStatus -Method GET -Uri "http://localhost:8081/actuator/health"
        $r2 = Invoke-HttpStatus -Method GET -Uri "http://localhost:8082/actuator/health"
        $ok1 = ($r1.StatusCode -eq 200)
        $ok2 = ($r2.StatusCode -eq 200)
        if ($ok1 -and $ok2) {
            break
        }
        if ($i -eq $maxAttempts) {
            throw "Timeout aguardando staging pronto"
        }
        Start-Sleep -Seconds 2
    }

    Write-Host "Executando carga controlada multi-instancia..."
    & (Join-Path $repoRoot "mvnw.cmd") -B `
      "-Dstaging.load.enabled=true" `
      "-Dstaging.load.totalRequests=$TotalRequests" `
      "-Dstaging.load.concurrency=$Concurrency" `
      "-Dtest=StagingOperationalLoadTest" `
      test

    Write-Host "Validando comportamento explicito com Redis indisponivel..."
    Invoke-Compose @("stop", "redis")
    Start-Sleep -Seconds 2
    $unavailable = Invoke-HttpStatus `
        -Method POST `
        -Uri "http://localhost:8081/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body '{"email":"load@test.local","password":"WrongPassword123!"}'
    Write-Host ("REDIS_DOWN_STATUS={0}" -f $unavailable.StatusCode)
    Write-Host ("REDIS_DOWN_RATE_LIMIT_MODE={0}" -f $unavailable.Headers["X-RateLimit-Mode"])
    Invoke-Compose @("start", "redis")
}
finally {
    if (-not $KeepEnvironment) {
        Write-Host "Derrubando stack de staging..."
        try {
            Invoke-Compose @("down", "-v")
        } catch {
            Write-Host "Falha no down de compose: $($_.Exception.Message)"
        }
    }
}
