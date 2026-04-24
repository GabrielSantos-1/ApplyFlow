# Checkpoint - Ativacao Operacional de Ingestao (Ambiente Vazio)

**Data:** 2026-04-22  
**Escopo:** ativar pipeline existente de ingestao sem refatoracao estrutural.

## Diagnostico inicial

- Ambiente recreado estava com banco vazio:
  - `vacancies = 0`
  - `vacancy_ingestion_runs = 0`
- Frontend vazio era efeito legitimo de ausencia de dados.
- RBAC estava correto: endpoint admin de ingestao nao podia ser aberto para `USER`.

## O que foi executado

1. Validacao de estado operacional (DB, fontes, usuarios, flags).
2. Provisionamento controlado de `ADMIN` para execucao manual (via bootstrap por flag).
3. Execucao manual real:
   - `POST /api/v1/admin/vacancies/ingestion/runs`
   - `sourceType=REMOTIVE`, `limit=30`.
4. Revalidacao de populacao em banco e API.
5. Hardening apos validacao:
   - `BOOTSTRAP_ADMIN_ENABLED=false` em runtime.
6. Confirmacao de scheduler e lock funcionando.

## Evidencias objetivas (resumo)

- RBAC:
  - `USER` -> `403` em endpoint admin de ingestao.
  - `ADMIN` -> `200` com run manual `SUCCESS`.
- Banco apos execucao:
  - `vacancies_total = 22`
  - `runs_total = 7`
  - source: `REMOTIVE=22`
- Runs recentes:
  - `MANUAL SUCCESS` com `fetched=22`, `inserted=0`, `updated=0`, `skipped=22`
  - `SCHEDULED SUCCESS` e `SCHEDULED SKIPPED_LOCKED` (lock concorrente funcional).
- API:
  - `GET /api/v1/vacancies?limit=5` -> `200` com items.
- Runtime flags:
  - `INGESTION_ENABLED=true`
  - `INGESTION_SCHEDULER_ENABLED=true`
  - `INGESTION_BOOTSTRAP_ENABLED=true`
  - `BOOTSTRAP_ADMIN_ENABLED=false` (pos-hardening)

## Artefatos operacionais

- Runbook criado:
  - `docs/operations/ingestion-bootstrap.md`
- Contexto atualizado:
  - `context/PROJECT_STATE.md`
  - `context/TASKS.md`
  - `context/DECISIONS.md`

## Riscos remanescentes

1. Qualidade de dados ainda depende da fonte externa (seniority/location/remoteType incompletos).
2. Deduplicacao cross-source ainda nao exercitada (apenas REMOTIVE ativa no ambiente atual).
3. Ambientes sem persistencia de volume podem voltar ao estado vazio apos recriacao sem runbook.

## Proximo bloco recomendado

**Data Quality & Deduplication**
- normalizacao de `seniority` e `remoteType`;
- tratamento de `location` ambigua;
- deduplicacao cross-source com checksum/identidade forte;
- regras de qualidade para evitar poluicao do ranking/matching.
