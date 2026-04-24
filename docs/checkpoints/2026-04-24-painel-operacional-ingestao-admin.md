# Checkpoint Tecnico - Painel Operacional de Ingestao Admin
Data: 2026-04-24

## Objetivo do bloco
Criar uma visao administrativa agregada para monitorar a ingestao de vagas no ApplyFlow, cobrindo providers, execucoes, qualidade, dedupe e volume recente sem expor dados sensiveis ou alterar contratos existentes.

O bloco parte do estado consolidado de ingestao expandida, com `REMOTIVE` e `GREENHOUSE` ativos via fontes curadas.

## Endpoint criado
```text
GET /api/v1/admin/ingestion/overview
```

Caracteristicas:
- endpoint administrativo novo;
- protegido por `ADMIN`;
- resposta agregada;
- sem exposicao de `raw_payload`;
- sem listagem massiva de vagas;
- sem alteracao de endpoints publicos existentes.

## Design tecnico
O desenho manteve a arquitetura modular existente:

```text
interfaces/http -> application/usecase -> application/service -> application/port -> infrastructure/persistence
```

Responsabilidades:
- Controller: expor rota HTTP admin e delegar para use case.
- Use case: contrato de leitura do overview.
- Service: agregacao, calculos e normalizacao de status.
- Port: contrato interno para dados agregados.
- Adapter JPA/Postgres: queries agregadas e mapeamento seguro.

Nao houve mudanca de arquitetura, DTO publico existente ou endpoint existente.

## DTO agregado
DTO criado:

```text
AdminIngestionOverviewResponse
```

Estrutura de resposta:
- `providers`: visao por provider/fonte.
- `totals`: totais globais de providers, coletadas, persistidas, totais e visiveis.
- `quality`: media de `qualityScore` e principais flags.
- `dedupe`: total duplicado e taxa percentual.
- `recent`: volume nas ultimas 24h e 7 dias.

Campos por provider:
- `sourceConfigId`
- `name`
- `sourceType`
- `tenant`
- `active`
- `vacanciesCollected`
- `vacanciesPersisted`
- `duplicateVacancies`
- `averageQualityScore`
- `lastExecution`

Campos da ultima execucao:
- `status`
- `durationMs`
- `startedAt`
- `finishedAt`
- `fetchedCount`
- `persistedCount`
- `skippedCount`
- `failedCount`

## Service / usecase / repository
Arquivos principais:
- `AdminIngestionOverviewController`
- `AdminIngestionOverviewUseCase`
- `AdminIngestionOverviewService`
- `AdminIngestionOverviewRepository`
- `JpaAdminIngestionOverviewRepository`

Decisoes:
- agregacao feita no backend, nao no frontend;
- calculos de percentuais arredondados no service;
- status `FAILED` normalizado para `FAIL` na resposta operacional;
- limite fixo de top quality flags para evitar alta cardinalidade;
- mapeamento de datas tolerante a tipos temporais retornados pelo driver/Hibernate.

## Queries usadas
### Providers
Leitura de `vacancy_sources` ordenada por tipo e nome:

```text
findAllByOrderBySourceTypeAscDisplayNameAsc()
```

### Totais de execucao
Agregacao em `vacancy_ingestion_runs`:

```sql
select source_config_id,
       coalesce(sum(fetched_count), 0),
       coalesce(sum(inserted_count + updated_count), 0),
       coalesce(sum(skipped_count), 0),
       coalesce(sum(failed_count), 0)
from vacancy_ingestion_runs
where source_config_id is not null
group by source_config_id
```

### Ultima execucao por provider
Uso de `distinct on` em Postgres:

```sql
select distinct on (source_config_id)
       source_config_id,
       status,
       coalesce((extract(epoch from (coalesce(finished_at, started_at) - started_at)) * 1000)::bigint, 0),
       started_at,
       finished_at,
       fetched_count,
       inserted_count,
       updated_count,
       skipped_count,
       failed_count
from vacancy_ingestion_runs
where source_config_id is not null
order by source_config_id, started_at desc
```

### Vagas, dedupe, qualidade e volume
Agregacao em `VacancyJpaEntity` por `source` e `sourceTenant`:

```text
count(v)
sum(duplicateRecord)
avg(qualityScore)
sum(createdAt >= since24h)
sum(createdAt >= since7d)
```

### Quality flags
Top flags por `jsonb_array_elements_text`, com limite fixo:

```sql
select flag, count(*)
from vacancies v
cross join lateral jsonb_array_elements_text(v.quality_flags) flag
group by flag
order by count(*) desc, flag asc
limit :limit
```

## Dados retornados
Validacao runtime retornou:

```text
providers: 6
activeProviders: 3
vacanciesCollected: 1432
vacanciesPersisted: 244
vacanciesTotal: 222
vacanciesVisible: 219
duplicateVacancies: 3
duplicateRatePercent: 1.35
recent.last24h: 200
recent.last7d: 222
```

Providers ativos:
- `Remotive Public`
- `Greenhouse Figma`
- `Greenhouse Stripe`

Top quality flags:
- `MISSING_SKILLS`: 200
- `MISSING_OR_UNMAPPED_SENIORITY`: 65

## Validacao runtime
Backend tests:

```text
.\mvnw.cmd -B test -DskipITs
Tests run: 76
Failures: 0
Errors: 0
Skipped: 2
BUILD SUCCESS
```

Staging rebuild:

```text
docker compose -f apps\backend\infra\staging\docker-compose.yml up -d --build
```

Endpoint admin:

```text
GET /api/v1/admin/ingestion/overview
role: ADMIN
status: 200
```

Controle de acesso:

```text
GET /api/v1/admin/ingestion/overview
sem token -> 401
role USER -> 403
```

Falha runtime corrigida:
- O endpoint retornava `500` por tipo temporal inesperado no mapeamento manual de `started_at`/`finished_at`.
- Correcao minima: aceitar `OffsetDateTime`, `Instant`, `LocalDateTime` e `Timestamp` no adapter.
- Revalidacao final: endpoint retornou `200`.

## Seguranca aplicada
- Endpoint restrito a `ADMIN` via `@PreAuthorize("hasRole('ADMIN')")`.
- Resposta agregada, sem dados individuais de vagas.
- Sem `raw_payload`, HTML de origem ou campos ricos do provider.
- Sem PII.
- Sem token, cookie, userId sensivel ou payload completo em logs.
- Sem cardinalidade alta: top quality flags limitado.
- Sem endpoint de busca massiva administrativa.
- Erros inesperados continuam retornando mensagem generica ao cliente.
- Observabilidade segura adicionada para erro inesperado: loga path, correlationId, classe e mensagem da excecao, sem body/payload.

## OWASP mitigado
- API1 Broken Object Level Authorization:
  - rota protegida por role `ADMIN`;
  - anonimo recebe `401`;
  - usuario comum recebe `403`.
- API3 Broken Object Property Level Authorization:
  - DTO nao expoe `raw_payload`, dados individuais, payload externo ou propriedades internas.
- API4 Unrestricted Resource Consumption:
  - resposta agregada;
  - sem paginacao massiva;
  - queries por agregacao;
  - limite fixo de quality flags.
- A05 Security Misconfiguration:
  - erro inesperado nao vaza stack trace ao cliente;
  - log operacional seguro preserva diagnostico no servidor.

## Arquivos alterados
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/interfaces/http/AdminIngestionOverviewController.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/application/usecase/AdminIngestionOverviewUseCase.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/application/service/AdminIngestionOverviewService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/application/ingestion/port/AdminIngestionOverviewRepository.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/application/dto/response/AdminIngestionOverviewResponse.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/persistence/JpaAdminIngestionOverviewRepository.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/persistence/repository/VacancySourceJpaRepository.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/interfaces/http/GlobalExceptionHandler.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/SecurityAuthorizationIntegrationTest.java`

## Riscos residuais
- Query de quality flags usa recurso especifico de Postgres (`jsonb_array_elements_text`); por isso o teste de happy path runtime foi validado em staging/Postgres, nao em H2.
- `SKIPPED_LOCKED` pode aparecer como ultima execucao em ambiente multi-instancia; isso e correto operacionalmente, mas pode exigir UX/semantica melhor no painel futuro.
- O painel ainda e somente API; nao ha tela frontend administrativa.
- Nao ha indice especifico novo para todas as agregacoes; volume atual e aceitavel, mas crescimento de vagas pode exigir analise de plano e indices dedicados.

## Proximo passo recomendado
Criar a tela administrativa do painel operacional consumindo `GET /api/v1/admin/ingestion/overview`, com:

1. cards agregados de volume, dedupe e qualidade;
2. tabela por provider;
3. destaque para `FAIL`/`PARTIAL`;
4. aviso visual para `SKIPPED_LOCKED` em staging multi-instancia;
5. refresh manual sem polling agressivo;
6. smoke test versionado para o endpoint admin em Postgres.
