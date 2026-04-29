# Runtime Smoke - ApplyFlow

Validacao operacional automatizada do fluxo principal:

`vacancy -> match -> draft -> status -> tracking`

## Scripts

- Smoke HTTP do fluxo:
  - `apps/backend/ops/smoke/run-runtime-smoke.ps1`
- Orquestracao staging (compose + bootstrap admin + smoke):
  - `apps/backend/ops/smoke/run-staging-runtime-smoke.ps1`

## Pre-condicoes

- Docker Engine ativo no host.
- Variaveis obrigatorias do compose disponiveis no ambiente:
  - `JWT_SECRET_BASE64`
  - `ACTUATOR_METRICS_TOKEN`
  - `SMOKE_ADMIN_EMAIL`
  - `SMOKE_ADMIN_PASSWORD`

## Execucao recomendada

```powershell
$env:JWT_SECRET_BASE64="<base64-32-bytes-or-more>"
$env:ACTUATOR_METRICS_TOKEN="<metrics-token-local>"
$env:SMOKE_ADMIN_EMAIL="smoke-admin@applyflow.local"
$env:SMOKE_ADMIN_PASSWORD="<senha-local-nao-versionada>"
powershell -ExecutionPolicy Bypass -File apps/backend/ops/smoke/run-staging-runtime-smoke.ps1
```

O script de staging falha imediatamente se credenciais obrigatorias estiverem ausentes. Nao ha senha padrao versionada.

## Execucao direta contra backend ja em pe

```powershell
powershell -ExecutionPolicy Bypass -File apps/backend/ops/smoke/run-runtime-smoke.ps1 `
  -BaseUrl "http://localhost:8081" `
  -Email "admin@seu-dominio" `
  -Password "sua-senha"
```

## Criterios de sucesso

- Health `200`.
- `GET /api/v1/vacancies` sem token retorna `401`.
- Login e `GET /api/v1/auth/me` retornam `200`.
- Match gerado com estado `GENERATED`.
- Draft criado (`201`), transicao invalida bloqueada (`400`), transicoes validas aceitas (`200`).
- Tracking retorna `SUBMITTED`.
- Endpoints de `job-search-preferences` respondem `201/200` (sinal de schema/migration em runtime).
