param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$Email = "",
    [string]$Password = "",
    [string]$AccessToken = "",
    [int]$TimeoutSec = 20
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function ConvertTo-CanonicalBaseUrl {
    param([string]$Raw)
    if ([string]::IsNullOrWhiteSpace($Raw)) {
        throw "BaseUrl obrigatoria."
    }
    return $Raw.TrimEnd("/")
}

function New-JsonBody {
    param([object]$Data)
    return ($Data | ConvertTo-Json -Depth 10 -Compress)
}

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST", "PATCH")]
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null,
        [switch]$AllowFailure
    )

    $uri = "$script:ApiBase$Path"
    $headers = @{
        "X-Requested-With" = "applyflow-smoke"
    }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Method = $Method
        Uri = $uri
        Headers = $headers
        TimeoutSec = $TimeoutSec
    }

    if ($Body -ne $null) {
        $params["ContentType"] = "application/json"
        $params["Body"] = (New-JsonBody -Data $Body)
    }

    try {
        $response = Invoke-WebRequest @params
        $json = $null
        if ($response.Content) {
            try { $json = $response.Content | ConvertFrom-Json } catch { $json = $null }
        }
        return @{
            ok = $true
            status = [int]$response.StatusCode
            headers = $response.Headers
            body = $json
            raw = $response.Content
        }
    } catch {
        if (-not $_.Exception.Response) {
            if ($AllowFailure) {
                return @{
                    ok = $false
                    status = 0
                    headers = @{}
                    body = $null
                    raw = $_.Exception.Message
                }
            }
            throw
        }

        $status = [int]$_.Exception.Response.StatusCode
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $raw = $reader.ReadToEnd()
        $reader.Dispose()
        $stream.Dispose()

        $json = $null
        if ($raw) {
            try { $json = $raw | ConvertFrom-Json } catch { $json = $null }
        }

        if (-not $AllowFailure) {
            throw "Falha HTTP $status em $Method $Path. Body: $raw"
        }

        return @{
            ok = $false
            status = $status
            headers = $_.Exception.Response.Headers
            body = $json
            raw = $raw
        }
    }
}

function Assert-Status {
    param(
        [string]$Step,
        [int]$Expected,
        [hashtable]$Response
    )
    if ($Response.status -ne $Expected) {
        throw "[$Step] esperado HTTP $Expected, recebido $($Response.status)."
    }
    Write-Host ("[OK] {0} -> HTTP {1}" -f $Step, $Response.status)
}

function Assert-AnyStatus {
    param(
        [string]$Step,
        [int[]]$ExpectedAny,
        [hashtable]$Response
    )
    if (-not ($ExpectedAny -contains $Response.status)) {
        throw "[$Step] esperado HTTP em {$($ExpectedAny -join ',')}, recebido $($Response.status)."
    }
    Write-Host ("[OK] {0} -> HTTP {1}" -f $Step, $Response.status)
}

$script:ApiBase = ConvertTo-CanonicalBaseUrl -Raw $BaseUrl
$token = $AccessToken

Write-Host "=== ApplyFlow Runtime Smoke ==="
Write-Host ("BaseUrl: {0}" -f $script:ApiBase)

$health = Invoke-Api -Method GET -Path "/actuator/health" -Token "" -AllowFailure
Assert-Status -Step "health" -Expected 200 -Response $health

$unauthVacancies = Invoke-Api -Method GET -Path "/api/v1/vacancies?page=0&size=1" -Token "" -AllowFailure
Assert-Status -Step "vacancies_sem_token" -Expected 401 -Response $unauthVacancies

if ([string]::IsNullOrWhiteSpace($token)) {
    if ([string]::IsNullOrWhiteSpace($Email) -or [string]::IsNullOrWhiteSpace($Password)) {
        throw "Informe -AccessToken ou (-Email e -Password) para autenticar o smoke."
    }
    $login = Invoke-Api -Method POST -Path "/api/v1/auth/login" -Token "" -Body @{
        email = $Email
        password = $Password
    } -AllowFailure
    Assert-Status -Step "auth_login" -Expected 200 -Response $login
    if (-not $login.body -or -not $login.body.accessToken) {
        throw "[auth_login] accessToken ausente na resposta."
    }
    $token = [string]$login.body.accessToken
}

$me = Invoke-Api -Method GET -Path "/api/v1/auth/me" -Token $token -AllowFailure
Assert-Status -Step "auth_me" -Expected 200 -Response $me

$vacancies = Invoke-Api -Method GET -Path "/api/v1/vacancies?page=0&size=1&sortBy=createdAt&sortDirection=desc" -Token $token -AllowFailure
Assert-Status -Step "vacancies_auth" -Expected 200 -Response $vacancies

if (-not $vacancies.body -or -not $vacancies.body.items -or $vacancies.body.items.Count -lt 1) {
    throw "[vacancies_auth] nenhuma vaga retornada para o smoke."
}

$vacancyId = [string]$vacancies.body.items[0].id
if ([string]::IsNullOrWhiteSpace($vacancyId)) {
    throw "[vacancies_auth] id de vaga ausente."
}
Write-Host ("[OK] vacancy_id -> {0}" -f $vacancyId)

$rateLimitHeader = $vacancies.headers["X-RateLimit-Policy"]
if ([string]::IsNullOrWhiteSpace($rateLimitHeader)) {
    throw "[vacancies_auth] header X-RateLimit-Policy ausente."
}
Write-Host ("[OK] vacancies_rate_limit_policy -> {0}" -f $rateLimitHeader)

$resumeTitle = "Smoke Resume $(Get-Date -Format 'yyyyMMdd-HHmmss')"
$resume = Invoke-Api -Method POST -Path "/api/v1/resumes" -Token $token -Body @{
    title = $resumeTitle
    sourceFileName = "smoke-resume.txt"
} -AllowFailure
Assert-Status -Step "resume_create" -Expected 201 -Response $resume

$resumeId = [string]$resume.body.id
if ([string]::IsNullOrWhiteSpace($resumeId)) {
    throw "[resume_create] resumeId ausente."
}

$variant = Invoke-Api -Method POST -Path "/api/v1/resumes/$resumeId/variants" -Token $token -Body @{
    vacancyId = $vacancyId
    variantLabel = "Smoke Variant $(Get-Date -Format 'HHmmss')"
} -AllowFailure
Assert-Status -Step "variant_create" -Expected 201 -Response $variant

$variantId = [string]$variant.body.id
if ([string]::IsNullOrWhiteSpace($variantId)) {
    throw "[variant_create] resumeVariantId ausente."
}

$matchGenerate = Invoke-Api -Method POST -Path "/api/v1/matches" -Token $token -Body @{
    vacancyId = $vacancyId
    resumeId = $resumeId
    resumeVariantId = $variantId
    forceRegenerate = $true
} -AllowFailure
Assert-Status -Step "match_generate" -Expected 200 -Response $matchGenerate

$matchState = [string]$matchGenerate.body.state
Assert-AnyStatus -Step "match_state_http" -ExpectedAny @(200) -Response $matchGenerate
if ($matchState -ne "GENERATED") {
    throw "[match_generate] estado inesperado: $matchState (esperado GENERATED)."
}
Write-Host ("[OK] match_state -> {0}" -f $matchState)

$draft = Invoke-Api -Method POST -Path "/api/v1/applications/drafts" -Token $token -Body @{
    vacancyId = $vacancyId
    resumeVariantId = $variantId
    messageDraft = "Smoke draft validation"
} -AllowFailure
Assert-Status -Step "draft_create" -Expected 201 -Response $draft

$draftId = [string]$draft.body.id
if ([string]::IsNullOrWhiteSpace($draftId)) {
    throw "[draft_create] draftId ausente."
}

$invalidTransition = Invoke-Api -Method PATCH -Path "/api/v1/applications/$draftId/status" -Token $token -Body @{
    status = "APPLIED"
    notes = "invalid-transition-smoke"
} -AllowFailure
Assert-Status -Step "draft_invalid_transition" -Expected 400 -Response $invalidTransition

$toReview = Invoke-Api -Method PATCH -Path "/api/v1/applications/$draftId/status" -Token $token -Body @{
    status = "READY_FOR_REVIEW"
    notes = "smoke-ready"
} -AllowFailure
Assert-Status -Step "draft_to_ready_for_review" -Expected 200 -Response $toReview

$toApplied = Invoke-Api -Method PATCH -Path "/api/v1/applications/$draftId/status" -Token $token -Body @{
    status = "APPLIED"
    notes = "smoke-applied"
} -AllowFailure
Assert-Status -Step "draft_to_applied" -Expected 200 -Response $toApplied

$tracking = Invoke-Api -Method GET -Path "/api/v1/applications/$draftId/tracking" -Token $token -AllowFailure
Assert-Status -Step "draft_tracking" -Expected 200 -Response $tracking
if (-not $tracking.body -or $tracking.body.Count -lt 3) {
    throw "[draft_tracking] eventos insuficientes no tracking."
}

$trackingStages = @($tracking.body | ForEach-Object { [string]$_.stage })
if (-not ($trackingStages -contains "SUBMITTED")) {
    throw "[draft_tracking] stage SUBMITTED nao encontrado."
}
Write-Host ("[OK] tracking_stages -> {0}" -f ($trackingStages -join ","))

$matchByVacancy = Invoke-Api -Method GET -Path "/api/v1/matches/vacancy/$vacancyId" -Token $token -AllowFailure
Assert-Status -Step "match_by_vacancy" -Expected 200 -Response $matchByVacancy
if ([string]$matchByVacancy.body.state -ne "GENERATED") {
    throw "[match_by_vacancy] estado inesperado: $([string]$matchByVacancy.body.state)"
}

$matchSummary = Invoke-Api -Method GET -Path "/api/v1/matches/vacancy/$vacancyId/summary" -Token $token -AllowFailure
Assert-Status -Step "match_summary" -Expected 200 -Response $matchSummary
if ([string]$matchSummary.body.state -ne "GENERATED") {
    throw "[match_summary] estado inesperado: $([string]$matchSummary.body.state)"
}

$preferenceKeyword = "smoke-qa-$(Get-Date -Format 'yyyyMMddHHmmss')"
$jobPreference = Invoke-Api -Method POST -Path "/api/v1/job-search-preferences" -Token $token -Body @{
    keyword = $preferenceKeyword
    provider = "REMOTIVE"
    enabled = $false
} -AllowFailure
Assert-Status -Step "job_search_preference_create" -Expected 201 -Response $jobPreference

$listPreferences = Invoke-Api -Method GET -Path "/api/v1/job-search-preferences" -Token $token -AllowFailure
Assert-Status -Step "job_search_preference_list" -Expected 200 -Response $listPreferences

Write-Host "=== SMOKE_RUNTIME_RESULT=PASS ==="
