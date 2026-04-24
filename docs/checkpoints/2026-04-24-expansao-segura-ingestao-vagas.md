# Checkpoint Tecnico - Expansao Segura de Ingestao de Vagas
Data: 2026-04-24

## Contexto
O ApplyFlow estava no estado consolidado de 2026-04-24, com runtime validado, UX de candidaturas consolidada e fluxo principal funcionando:

```text
ingestao -> normalizacao -> dedupe -> persistencia -> vacancies API
```

O problema investigado foi o baixo volume de vagas retornado por `GET /api/v1/vacancies`, com suspeita de limite ou falha parcial no pipeline de ingestao.

## Causa raiz do baixo volume
A causa raiz estava na cobertura operacional de providers, nao no contrato publico da API de vagas.

- Antes da correcao, apenas `REMOTIVE` estava ativo.
- O provider Remotive retornava uma janela efetiva de aproximadamente 20 vagas por execucao.
- Execucoes seguintes reencontravam as mesmas vagas e eram corretamente filtradas pelo dedupe.
- Em staging multi-instancia, o scheduler estava ativo em mais de um backend; o lock evitava concorrencia real, mas gerava execucoes `SKIPPED_LOCKED` e ruido operacional.
- Fontes Greenhouse existiam como estrutura, mas nao havia fonte real curada ativa para ampliar o volume.

## Providers antes/depois

### Antes
| Provider | Fonte | Estado | Observacao |
|---|---|---|---|
| REMOTIVE | Remotive Public | Ativo | Unica fonte efetiva, ~20 vagas por execucao |
| ADZUNA | Adzuna Default | Inativo | Mantido desativado |
| GREENHOUSE | Greenhouse Default | Inativo | Configuracao exemplo |
| LEVER | Lever Default | Inativo | Configuracao exemplo |

### Depois
| Provider | Fonte | Estado | Limite |
|---|---|---|---|
| REMOTIVE | Remotive Public | Ativo | `maxJobsPerRun=200` |
| GREENHOUSE | Greenhouse Stripe | Ativo | `maxJobsPerRun=120` |
| GREENHOUSE | Greenhouse Figma | Ativo | `maxJobsPerRun=80` |
| ADZUNA | Adzuna Default | Inativo | Mantido desativado |
| GREENHOUSE | Greenhouse Default | Inativo | Mantido desativado |
| LEVER | Lever Default | Inativo | Mantido desativado |

## Mudancas em Remotive
- Envio explicito de `limit` para a API Remotive.
- Suporte a categorias controladas via configuracao.
- Limite interno de categorias por execucao.
- Backoff entre grupos de requisicoes para evitar consumo agressivo.
- Dedupe em memoria por `externalJobId` ao combinar categorias.
- Validacao preservada de HTTPS e allowlist de host.
- `raw_payload` passou a omitir `description` para evitar persistencia de HTML bruto de origem.

## Ativacao curada de Greenhouse Stripe/Figma
Foram ativadas duas fontes confiaveis e explicitas:

- `Greenhouse Stripe`
  - `boardToken=stripe`
  - `tenant=stripe`
  - `maxJobsPerRun=120`
- `Greenhouse Figma`
  - `boardToken=figma`
  - `tenant=figma`
  - `maxJobsPerRun=80`

A ativacao foi feita por migration, sem endpoint novo e sem permitir fonte arbitraria definida pelo cliente.

Ajustes de seguranca e robustez no connector Greenhouse:
- validacao de `boardToken` por regex;
- fallback de `company` para `tenant` quando o provider nao envia `company_name`;
- aumento controlado de `maxPayloadBytes`;
- `raw_payload` omitindo `content` para nao persistir HTML bruto de origem.

## Migrations V8, V9 e V10
- `V8__remotive_controlled_category_ingestion.sql`
  - Atualiza Remotive com `maxJobsPerRun=200`.
  - Adiciona categorias controladas `software-dev` e `devops`.
- `V9__enable_curated_greenhouse_sources.sql`
  - Insere/ativa `Greenhouse Stripe`.
  - Insere/ativa `Greenhouse Figma`.
  - Mantem ativacao curada e auditavel por banco/migration.
- `V10__sanitize_legacy_remotive_raw_payload.sql`
  - Remove `description` de `raw_payload` legado Remotive.
  - Marca os registros com `descriptionOmitted=true`.

## Evidencia antes/depois

### Antes
Banco:

```text
total vacancies: 22
REMOTIVE: 22
duplicates: 0
recent_3d: 22
```

### Execucao manual validada
`POST /api/v1/admin/vacancies/ingestion/runs` autenticado como admin:

```text
requestedRuns: 3
successfulRuns: 3
failedRuns: 0

REMOTIVE:
  fetched=20
  normalized=20
  inserted=0
  skipped=20
  failed=0

GREENHOUSE / stripe:
  fetched=120
  normalized=120
  inserted=120
  skipped=0
  failed=0

GREENHOUSE / figma:
  fetched=80
  normalized=80
  inserted=80
  skipped=0
  failed=0
```

### Depois
Banco:

```text
total vacancies: 222
visible vacancies: 219
duplicate vacancies: 3

GREENHOUSE / figma: 80
GREENHOUSE / stripe: 120
REMOTIVE / remotive.com: 22
```

API:

```text
GET /api/v1/vacancies?page=0&size=5
status: 200
totalElements: 219
```

Chamada sem credencial:

```text
GET /api/v1/vacancies?page=0&size=5
status: 401
resultado esperado: endpoint exige autenticacao
```

## Dedupe validado
- Dedupe permaneceu ativo.
- Foram encontrados `3` grupos duplicados.
- Os duplicados identificados estavam em `stripe` e tinham titulo, empresa e localidade equivalentes.
- A API retornou `219` vagas visiveis, excluindo as `3` marcadas como duplicadas.
- Nao foi identificado falso positivo evidente na amostra validada.

## Seguranca aplicada
- Sem alteracao de endpoint publico.
- Sem alteracao de DTO publico.
- Sem remocao de dedupe.
- Sem dependencia nova.
- Sem scraping agressivo.
- SSRF mitigado por HTTPS + allowlist de host.
- Greenhouse restringe `boardToken` por regex, evitando path injection.
- Providers ativados sao curados por migration.
- Limites operacionais preservados:
  - request admin com limite maximo ja existente;
  - `maxJobsPerRun` por fonte;
  - `maxPayloadBytes`;
  - backoff interno no Remotive;
  - scheduler Remotive com intervalo maior em staging.
- Logs de ingestao registram contadores (`fetched`, `normalized`, `inserted`, `skipped`, `failed`) sem payload bruto.
- `raw_payload` novo e legado nao retem campos ricos HTML (`description` Remotive, `content` Greenhouse).

## Validacoes executadas
- Backend tests:

```text
.\mvnw.cmd -B test -DskipITs
Tests run: 74
Failures: 0
Errors: 0
Skipped: 2
BUILD SUCCESS
```

- Rebuild de staging:

```text
docker compose -f apps\backend\infra\staging\docker-compose.yml up -d --build
```

- Flyway:

```text
V8  remotive controlled category ingestion  success=true
V9  enable curated greenhouse sources       success=true
V10 sanitize legacy remotive raw payload    success=true
```

- Banco:
  - total por provider;
  - duplicadas;
  - grupos por `dedupe_key`;
  - saneamento de `raw_payload`.
- API:
  - `GET /api/v1/vacancies` autenticado -> `200`;
  - `GET /api/v1/vacancies` sem autenticacao -> `401`.
- Logs:
  - runs de ingestao com contadores claros;
  - sem `500` no fluxo validado.

## Arquivos alterados
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/integration/remotive/RemotiveVacancySourceConnector.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/integration/remotive/RemotivePayloadMapper.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/integration/greenhouse/GreenhouseVacancySourceConnector.java`
- `apps/backend/src/main/resources/application.yml`
- `apps/backend/src/main/resources/application-staging.yml`
- `apps/backend/src/main/resources/db/migration/V8__remotive_controlled_category_ingestion.sql`
- `apps/backend/src/main/resources/db/migration/V9__enable_curated_greenhouse_sources.sql`
- `apps/backend/src/main/resources/db/migration/V10__sanitize_legacy_remotive_raw_payload.sql`
- `apps/backend/infra/staging/docker-compose.yml`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/vacancies/infrastructure/integration/remotive/RemotiveVacancySourceConnectorTest.java`

## Riscos residuais
- Remotive continua limitado pela janela efetiva do provider; aumentar `limit` nao garante maior retorno upstream.
- Greenhouse Stripe/Figma ampliam volume, mas sao fontes curadas especificas; crescimento futuro exige curadoria de novos boards/providers.
- Dedupe validado por amostra e contagem; uma campanha maior cross-source ainda pode revelar falso positivo/negativo em casos extremos.
- Scheduler em ambiente multi-instancia depende do lock para evitar concorrencia; recomendavel evoluir politica operacional para agendamento unico ou lideranca explicita.
- A qualidade de metadados varia por provider, especialmente localidade, senioridade e remoto/hibrido.

## Proximo passo recomendado
Criar uma lista curada adicional de providers/boards confiaveis e parametrizar limites por ambiente, mantendo:

1. allowlist estrita;
2. caps de payload e itens por execucao;
3. observabilidade por provider;
4. campanha de dedupe cross-source;
5. smoke test versionado de ingestao (`admin run -> banco -> GET /vacancies`).
