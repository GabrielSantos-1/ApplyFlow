# Checkpoint Tecnico - CI/CD & Repository Protection
Data: 2026-04-28

## Versao atual do sistema
O ApplyFlow permanece no estado consolidado pos-runtime validation, UX de candidatura, sessao frontend segura, ingestao multi-source, painel admin operacional, priorizacao segura de vagas e ingestao query-driven por preferencias do usuario.

Nesta data foi executado bloco de CI/CD minimo, protecao de repositorio e prevencao de regressao, sem novas features e sem mudanca arquitetural.

Referencia oficial desta versao:
- `docs/checkpoints/2026-04-28-ci-cd-repository-protection.md`

## Fluxo principal consolidado
```text
vacancy -> match -> draft -> status -> tracking
```

## Estado consolidado
- Backend Java/Spring Boot e PostgreSQL seguem como fonte de verdade.
- Frontend Next.js atua como apresentacao e nao decide regra de negocio.
- Matching segue deterministico.
- Ownership permanece obrigatorio.
- Dedupe e qualityScore seguem ativos.
- Rate limit segue aplicado a fluxos sensiveis.
- Painel admin de ingestao esta operacional.
- `/vagas` prioriza oportunidades sem recalcular score no frontend.

## Query-driven ingestion
Foi criado suporte a pesquisas controladas por usuario em `UserJobSearchPreference`.

Endpoints:
- `GET /api/v1/job-search-preferences`
- `POST /api/v1/job-search-preferences`
- `PATCH /api/v1/job-search-preferences/{id}`

Providers:
- `REMOTIVE`: busca por keyword.
- `ADZUNA`: busca por keyword/location quando credenciais existirem.
- `GREENHOUSE`: mantido como board-curated.

## Seguranca aplicada
- Ownership por `userId`.
- Provider allowlist.
- Normalizacao Unicode e bloqueio de caracteres de controle.
- Limite de preferencias por usuario.
- Rate limit em leitura/escrita.
- Sem URL arbitraria do usuario.
- Logs com hash de keyword.
- Dedupe/qualityScore preservados no pipeline existente.
- Outro usuario nao acessa preferencia alheia.

## Evidencias recentes
- Backend tests (2026-04-28):
  - `.\mvnw.cmd -B test -DskipITs`;
  - 76 testes, 0 falhas, 0 erros, 2 skipped.
- Frontend build (2026-04-28):
  - `npm run build` em `apps/frontend` -> sucesso com TypeScript.
- Frontend quality gate (2026-04-28):
  - `npm run lint` -> OK (`tsc --noEmit`);
  - `npm run typecheck` -> OK;
  - `npm run build` -> OK.
- Runtime smoke E2E (2026-04-28):
  - script implementado em `apps/backend/ops/smoke`;
  - runtime staging reportado como validado com `SMOKE_RUNTIME_RESULT=PASS`.
- CI/CD (2026-04-28):
  - `.github/workflows/ci.yml` criado com `backend-test`, `frontend-quality`, `repository-hygiene`;
  - `.github/workflows/runtime-smoke.yml` criado como workflow manual;
  - `.github/dependabot.yml` criado;
  - documentacao de branch protection e secret protection criada.

## Limitacoes conhecidas
- Adzuna depende de credenciais externas.
- Greenhouse nao suporta busca global no conector atual.
- Aumento real de volume depende de resultados novos nas APIs externas.
- Repositorio Git/GitHub deve ser inicializado com varredura de segredos e `.gitignore` abrangente.

## Proxima retomada segura
1. Fazer push/PR e validar GitHub Actions remoto.
2. Configurar branch protection manualmente para `main` exigindo `backend-test`, `frontend-quality` e `repository-hygiene`.
3. Ativar secret scanning/push protection no GitHub quando disponivel.
