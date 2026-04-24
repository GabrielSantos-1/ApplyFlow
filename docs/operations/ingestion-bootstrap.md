# Runbook - Ingestion Bootstrap Operacional

## Objetivo

Tirar o ambiente de estado "saudavel porem vazio" para estado operacional, com trilha auditavel.

## 1) Validar banco vazio

```powershell
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select count(*) as vacancies_total from vacancies;"
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select count(*) as runs_total from vacancy_ingestion_runs;"
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select source_type, display_name, enabled from vacancy_sources order by source_type;"
```

## 2) Validar ADMIN e RBAC

```powershell
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select email, role, is_active from users order by created_at;"
```

Regras esperadas:
- `USER` continua sem permissao para `/api/v1/admin/vacancies/ingestion/runs` (403).
- `ADMIN` executa ingestao manual com sucesso (200).

## 3) Executar ingestao manual (ADMIN)

Endpoint:
- `POST /api/v1/admin/vacancies/ingestion/runs`

Body minimo recomendado:

```json
{
  "sourceType": "REMOTIVE",
  "limit": 30
}
```

Validar no retorno:
- `requestedRuns`
- `successfulRuns`
- `failedRuns`
- `runs[].runId`
- `runs[].status`
- `runs[].fetchedCount`
- `runs[].insertedCount`
- `runs[].updatedCount`
- `runs[].skippedCount`
- `runs[].correlationId`

## 4) Confirmar populacao do banco e API

```powershell
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select count(*) as vacancies_total from vacancies;"
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select count(*) as runs_total from vacancy_ingestion_runs;"
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select source, count(*) as total from vacancies group by source order by total desc;"
docker exec staging-postgres-1 psql -U applyflow -d applyflow -c "select source_type, trigger_type, status, fetched_count, inserted_count, updated_count, skipped_count, started_at from vacancy_ingestion_runs order by started_at desc limit 10;"
```

```powershell
# com token valido de USER autenticado
GET http://localhost:8081/api/v1/vacancies?limit=5
```

## 5) Scheduler controlado

Flags:
- `INGESTION_ENABLED=true`
- `INGESTION_SCHEDULER_ENABLED=true|false`

Valide:
- existem runs `trigger_type=SCHEDULED`
- lock evita concorrencia (`status=SKIPPED_LOCKED` quando aplicavel)
- nao ha loop agressivo (delay por configuracao)

## 6) Bootstrap minimo para ambiente vazio

Flags:
- `INGESTION_BOOTSTRAP_ENABLED=true|false`
- `INGESTION_BOOTSTRAP_SOURCE=REMOTIVE`
- `INGESTION_BOOTSTRAP_MAX_JOBS_PER_RUN=120`
- `INGESTION_BOOTSTRAP_ONLY_WHEN_VACANCIES_EMPTY=true`
- `INGESTION_BOOTSTRAP_REQUIRE_NO_RUNS=true`

Comportamento esperado:
- so roda quando habilitado.
- so roda quando ambiente esta vazio (conforme flags).
- registra inicio/fim/resultado.
- nao executa em loop no startup.

## 7) Bootstrap de ADMIN (uso emergencial controlado)

Somente para ambiente controlado:
- `BOOTSTRAP_ADMIN_ENABLED=true`
- `BOOTSTRAP_ADMIN_EMAIL=<admin>`
- `BOOTSTRAP_ADMIN_PASSWORD=<senha>`
- `BOOTSTRAP_ADMIN_FORCE_PASSWORD_RESET=true|false`

Apos provisionar/validar:
- voltar `BOOTSTRAP_ADMIN_ENABLED=false` (hardening de menor privilegio).

## 8) Recuperacao rapida apos recriacao de ambiente

1. Subir stack.
2. Confirmar migrations aplicadas (`Flyway`).
3. Confirmar fontes (`vacancy_sources`).
4. Se ambiente vazio:
   - executar run manual com ADMIN ou
   - usar bootstrap de ingestao (flag) para primeira carga.
5. Validar `vacancies > 0` e `runs > 0`.

## Riscos operacionais conhecidos

- Recriacao de ambiente sem volume persistente pode zerar dados.
- Se scheduler estiver desligado e ninguem rodar manualmente, banco volta a ficar vazio.
- Bootstrap de ADMIN ligado permanentemente aumenta superficie operacional; manter desligado por padrao.
