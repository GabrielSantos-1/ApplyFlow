# Checkpoint - 2026-04-21 - Bloco 6.1 Validacao Real da Ingestao

## Status
`validado com evidencia operacional real, com correcao de lock concorrente durante a campanha`

## Gate e divergencia inicial
1. Checkpoint do Bloco 6 e `PROJECT_STATE` lidos antes da execucao.
2. Divergencia real encontrada no ambiente:
   - `vacancy_ingestion_runs` inexistente no Postgres de staging em execucao.
3. Acao de alinhamento aplicada:
   - `docker compose up -d --build` na stack staging.
   - Flyway confirmou `V3 vacancy ingestion foundation` aplicado.

## Cenarios executados (evidencia real)

### 1) Execucao online real (fonte Remotive)
- Trigger manual autenticado (`ADMIN`) em `POST /api/v1/vacancies/ingestion/runs`.
- Resultado inicial:
  - `status=SUCCESS`
  - `fetched=19`
  - `normalized=19`
  - `persisted=19`
  - `duplicates=0`
  - duracao ~`0.76s` (startedAt -> finishedAt)

### 2) Reingestao para dedupe
- Runs repetidos sequenciais.
- Resultado:
  - `fetched=19`
  - `normalized=19`
  - `persisted=0`
  - `duplicates=19`
- Banco:
  - `total_vacancies=19`
  - `distinct_external_ids=19`
  - sem duplicata por `external_id`.

### 3) Concorrencia / lock
- Duas execucoes manuais simultaneas da mesma fonte.
- Achado:
  - lock inicial apresentou falha (500 + run orfao `RUNNING`) por detalhe transacional no adapter.
- Correcao aplicada no bloco:
  - lock voltou para `JdbcTemplate` com `insert` atomico e captura imediata de duplicate key.
- Revalidacao apos fix:
  - uma execucao `SUCCESS`
  - outra `SKIPPED_LOCKED`
  - sem lock orfao em `vacancy_ingestion_locks`.

### 4) Falha externa controlada
- Backend-1 com override temporario de fonte:
  - `INGESTION_REMOTIVE_BASE_URL=https://remotive.invalid`
  - `INGESTION_REMOTIVE_ALLOWED_HOSTS=remotive.invalid`
- Resultado do run:
  - `status=FAILED`
  - `fetched=0`
  - `persisted=0`
  - `failedCount=1`
  - `errorSummary` com erro de I/O/DNS.
- Comportamento esperado confirmado:
  - falha explicita
  - sem persistencia parcial
  - log em `stage=fetch`.

## Throughput observado
- Baseado em runs reais de sucesso (19 vagas):
  - run 1: ~`0.76s` -> ~`25 vagas/s`
  - run dedupe: ~`0.28s` -> ~`67 vagas/s`
  - run dedupe (com lock validado): ~`1.05s` -> ~`18 vagas/s`
- Leitura honesta:
  - volume pequeno da fonte no momento da coleta (19 itens);
  - throughput varia com latencia externa e custo de persistencia/dedupe.

## Observabilidade comprovada
- Actuator (`/actuator/prometheus`) com token tecnico:
  - `applyflow_ingestion_runs_started_total`
  - `applyflow_ingestion_runs_completed_total` (success e failed)
  - `applyflow_ingestion_stage_failures_total{stage="fetch"}`
  - `applyflow_ingestion_duplicates_total`
  - `applyflow_ingestion_run_duration_seconds`
- Prometheus:
  - targets `backend-1` e `backend-2` em `health=up`
  - series de ingestao retornadas apos run real.
- Logs estruturados:
  - `stage=run outcome=SUCCESS`
  - `stage=fetch outcome=failed`.

## Seguranca validada neste bloco
- A01 Broken Access Control:
  - trigger manual continua exigindo perfil ADMIN.
- A10 SSRF / API10 Unsafe Consumption:
  - sem URL arbitraria de usuario;
  - allowlist e HTTPS permanecem.
- API4 Resource Consumption:
  - limites de payload/jobs preservados.
- A09 Logging and Monitoring:
  - sinais de sucesso/falha/lock observados em metrica e log.

## Correcoes aplicadas durante a validacao
1. **Lock concorrente**:
   - corrigido adapter para SQL atomico com `JdbcTemplate`.
2. **Operacao staging**:
   - compose recebeu variaveis explicitas de ingestao para permitir testes controlados de falha externa.
3. **Higiene operacional**:
   - run legado `RUNNING` (pre-fix) marcado como `FAILED` para historico consistente.

## Testes executados
- `.\mvnw.cmd -B test`
- Resultado:
  - `Tests run: 45, Failures: 0, Errors: 0, Skipped: 2`

## Riscos remanescentes
1. Lock ainda nao tem lease/TTL automatico (risco de intervencao manual em crash extremo).
2. Ainda existe dependencia de disponibilidade/dns da fonte externa para sucesso do fetch.
3. Volume da fonte no momento da prova foi baixo (19), entao throughput representa baseline de baixa carga.

## Proximo passo recomendado
1. Bloco 6.2:
   - lease/TTL para lock
   - reconciliador de runs `RUNNING` antigos
   - alerta operacional para taxa de `FAILED`/`SKIPPED_LOCKED`.
