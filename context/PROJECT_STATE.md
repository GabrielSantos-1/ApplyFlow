# PROJECT_STATE.md

## Snapshot Atualizado - 2026-04-29

| Campo | Valor |
|---|---|
| Projeto | `ApplyFlow / Job Copilot` |
| Data | `2026-04-29` |
| Fase | `README + LICENSE public readiness fix` |
| Status | `README publico, SECURITY.md, CONTRIBUTING.md, env examples e estrutura do repositorio documentados; backend tests e frontend lint/typecheck/build passando; GitHub Actions remoto, branch protection e secret scanning ainda pendentes antes de abertura publica; LICENSE MIT definido` |

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
- README raiz corrigido em portugues pt-BR com Markdown valido.
- `SECURITY.md` criado.
- `CONTRIBUTING.md` criado.
- `.env.example` raiz, backend e frontend padronizados sem secrets reais.
- Estrutura do repositorio documentada em `docs/architecture/repository-structure.md`.
- `LICENSE` MIT criado.

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
- Publicacao:
  - README renderiza corretamente;
  - LICENSE MIT definido;
  - `SECURITY.md` alinhado com MIT;
  - validacoes locais deste bloco passaram.

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
- `docs/checkpoints/2026-04-29-public-release-hardening-portfolio-polish.md`
- `docs/checkpoints/2026-04-29-readme-license-public-readiness-fix.md`
- `docs/operations/repository-protection.md`
- `docs/architecture/repository-structure.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`

## Proxima retomada segura
1. Reexecutar validacoes finais deste bloco e revisar scanner de secrets.
2. Fazer push/PR e validar GitHub Actions remoto.
3. Configurar branch protection manual da `main`.
4. Ativar secret scanning/push protection quando disponivel no GitHub.
5. Confirmar publicacao somente apos protecoes remotas do GitHub.
