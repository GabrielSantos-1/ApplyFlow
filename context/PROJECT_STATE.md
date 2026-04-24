# PROJECT_STATE.md

## Snapshot Atualizado - 2026-04-24

| Campo | Valor |
|---|---|
| Projeto | `ApplyFlow / Job Copilot` |
| Data | `2026-04-24` |
| Fase | `Query-driven ingestion + repository bootstrap` |
| Status | `Runtime validado, UX consolidada, ingestao multi-source e query-driven ativas, painel admin operacional, priorizacao segura de vagas, preparacao para GitHub com varredura de segredos` |

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
- Runtime:
  - Flyway validou 11 migrations;
  - preferencias `QA` e `Java Developer` executadas com `SUCCESS`;
  - dedupe evitou duplicatas, mantendo 222 vagas.
- Frontend:
  - ultimo build validado em checkpoint anterior para `/vagas` e `/candidaturas`.

## Referencias oficiais
- `docs/checkpoints/2026-04-24-runtime-validation-flow-consistency.md`
- `docs/checkpoints/2026-04-24-ux-candidatura-sessao-segura.md`
- `docs/checkpoints/2026-04-24-expansao-segura-ingestao-vagas.md`
- `docs/checkpoints/2026-04-24-painel-operacional-ingestao-admin.md`
- `docs/checkpoints/2026-04-24-priorizacao-segura-vagas.md`
- `docs/checkpoints/2026-04-24-query-driven-ingestion-and-repository-bootstrap.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`

## Proxima retomada segura
1. Concluir bootstrap Git/GitHub com `.gitignore`, `.env.example`, varredura de segredos, testes e build.
2. Criar endpoint backend batch/agregador para `/vagas`.
3. Criar tela admin frontend para o overview de ingestao.
4. Criar testes de navegador para fluxos principais.
