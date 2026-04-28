# PROJECT_STATE.md

## Snapshot Atualizado - 2026-04-28

| Campo | Valor |
|---|---|
| Projeto | `ApplyFlow / Job Copilot` |
| Data | `2026-04-28` |
| Fase | `CI/CD minimo + repository protection` |
| Status | `CI minimo criado, Dependabot configurado, workflow manual de smoke runtime criado, gate de higiene de repositorio implementado, validacoes locais passando; execucao remota do GitHub Actions e branch protection ainda pendentes de configuracao no GitHub` |

## Estado do produto
- Fluxo principal validado: `vacancy -> match -> draft -> status -> tracking`.
- Backend segue fonte de verdade para matching, score, recomendacao, dedupe, qualityScore, ownership e transicoes.
- Frontend segue camada de apresentacao, sem autorizacao real local e sem decisao de negocio.
- Sessao frontend usa token em memoria com reidratacao via refresh HttpOnly cookie.
- Ingestao multi-source esta ativa com Remotive e Greenhouse curados.
- Adzuna esta preparado para query quando credenciais externas forem configuradas.
- Painel admin de ingestao exposto via `GET /api/v1/admin/ingestion/overview`.
- `/vagas` prioriza com base em dados do backend.
- Preferencias de busca por usuario foram implementadas.
- Diagnostico tecnico de retomada concluido com checkpoint dedicado.
- Smoke runtime operacional versionado em `apps/backend/ops/smoke`.
- CI principal versionado em `.github/workflows/ci.yml`.
- Dependabot versionado em `.github/dependabot.yml`.

## Query-driven ingestion
- Modelo: `UserJobSearchPreference`.
- Endpoints:
  - `GET /api/v1/job-search-preferences`;
  - `POST /api/v1/job-search-preferences`;
  - `PATCH /api/v1/job-search-preferences/{id}`.
- Scheduler executa preferencias ativas.
- Pipeline reutilizado:
  - connector -> normalizer -> dedupe -> qualityScore -> upsert.

## Seguranca consolidada
- Ownership por usuario.
- Provider allowlist.
- Rate limit preservado.
- Sem logs de token, payload bruto ou termo completo de pesquisa.
- Sem SSRF por URL arbitraria de usuario.
- Sem remocao de dedupe ou qualityScore.
- Endpoints novos retornam erros controlados, sem 500 em cenarios de validacao.

## Evidencias
- Backend:
  - `.\mvnw.cmd -B test -DskipITs`;
  - 76 testes, 0 falhas, 0 erros, 2 skipped.
- Frontend:
  - `npm run build` em `apps/frontend`: sucesso;
  - `npm run lint`: sucesso (`tsc --noEmit`);
  - `npm run typecheck`: sucesso.
- Runtime:
  - script de smoke E2E implementado;
  - runtime staging reportado como validado com `SMOKE_RUNTIME_RESULT=PASS`.
- CI/CD:
  - `backend-test`, `frontend-quality` e `repository-hygiene` criados;
  - CI remoto ainda nao validado no GitHub.

## Referencias oficiais
- `docs/checkpoints/2026-04-24-runtime-validation-flow-consistency.md`
- `docs/checkpoints/2026-04-24-ux-candidatura-sessao-segura.md`
- `docs/checkpoints/2026-04-24-expansao-segura-ingestao-vagas.md`
- `docs/checkpoints/2026-04-24-painel-operacional-ingestao-admin.md`
- `docs/checkpoints/2026-04-24-priorizacao-segura-vagas.md`
- `docs/checkpoints/2026-04-24-query-driven-ingestion-and-repository-bootstrap.md`
- `docs/checkpoints/2026-04-28-retomada-codex-5-3.md`
- `docs/checkpoints/2026-04-28-runtime-validation-operational-hardening.md`
- `docs/checkpoints/2026-04-28-ci-cd-repository-protection.md`
- `docs/operations/repository-protection.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`

## Proxima retomada segura
1. Fazer push/PR e validar GitHub Actions remoto.
2. Configurar branch protection manual da `main`.
3. Ativar secret scanning/push protection quando disponivel no GitHub.
