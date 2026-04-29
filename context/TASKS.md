# TASKS.md

## Estado Atual de Execucao

- Em foco: public release hardening e portfolio polish para possivel exposicao publica.
- Resultado do bloco (2026-04-29):
  - [x] reescrever README raiz com apresentacao publica do ApplyFlow;
  - [x] criar `SECURITY.md`;
  - [x] padronizar `.env.example` raiz e `apps/backend/.env.example`;
  - [x] criar `apps/frontend/.env.example`;
  - [x] criar `docs/architecture/repository-structure.md`;
  - [x] criar `CONTRIBUTING.md`;
  - [x] registrar que LICENSE ainda nao foi decidido;
  - [x] reexecutar scanner final de secrets;
  - [x] reexecutar backend tests;
  - [x] reexecutar frontend lint/typecheck/build;
  - [ ] validar GitHub Actions remoto apos push/PR;
  - [ ] configurar branch protection manualmente no GitHub;
  - [ ] ativar secret scanning/push protection quando disponivel;
  - [ ] decidir LICENSE antes de tornar publico.

## CI/CD & Repository Protection (2026-04-28)- Resultado do bloco (2026-04-28):
  - [x] criar `.github/workflows/ci.yml`;
  - [x] criar job `backend-test`;
  - [x] criar job `frontend-quality`;
  - [x] criar job `repository-hygiene`;
  - [x] criar `.github/workflows/runtime-smoke.yml` manual;
  - [x] criar `.github/dependabot.yml`;
  - [x] documentar branch protection e secret protection em `docs/operations/repository-protection.md`;
  - [x] validar backend tests localmente (`76/0/0/2`);
  - [x] validar frontend lint/typecheck/build localmente;
  - [x] validar higiene de repositorio localmente;
  - [ ] validar GitHub Actions remoto apos push/PR;
  - [ ] configurar branch protection manualmente no GitHub.

## Runtime Validation & Operational Hardening (2026-04-28)

### Concluidas
  - [x] criar smoke E2E HTTP: `apps/backend/ops/smoke/run-runtime-smoke.ps1`;
  - [x] criar orchestrator staging: `apps/backend/ops/smoke/run-staging-runtime-smoke.ps1`;
  - [x] documentar operacao: `docs/operations/runtime-smoke.md`;
  - [x] remover artefatos sensiveis locais: `tmp_token.txt`, `tmp_token_runtime.txt`;
  - [x] criar docs obrigatorias ausentes:
    - [x] `apps/backend/README.md`;
    - [x] `apps/frontend/README.md`;
    - [x] `docs/checkpoints/README.md`;
  - [x] estabilizar gate frontend:
    - [x] `npm run lint` (via `tsc --noEmit`);
    - [x] `npm run typecheck`;
    - [x] `npm run build`;
  - [x] validar backend tests: `76/0/0/2`;
  - [x] validar smoke runtime real em staging:
    - runtime staging posteriormente reportado como `SMOKE_RUNTIME_RESULT=PASS`.
- Sem mudanca de arquitetura macro; fluxo principal backend e frontend consolidado permanecem compativeis.
- Prioridade imediata de retomada:
  1. fazer push/PR e validar GitHub Actions remoto;
  2. configurar branch protection da `main` exigindo `backend-test`, `frontend-quality` e `repository-hygiene`;
  3. ativar secret scanning/push protection no GitHub quando disponivel;
  4. manter endpoint batch/agregador para `/vagas` em ciclo posterior.

## Query-Driven Ingestion + Repository Bootstrap (2026-04-24)

### Concluidas
- [x] Implementar preferencias controladas de busca por usuario:
  - [x] `GET /api/v1/job-search-preferences`;
  - [x] `POST /api/v1/job-search-preferences`;
  - [x] `PATCH /api/v1/job-search-preferences/{id}`.
- [x] Criar migration:
  - [x] `V11__user_job_search_preferences.sql`.
- [x] Preservar dedupe e qualityScore no pipeline existente.
- [x] Manter Greenhouse como board-curated.
- [x] Adicionar busca por keyword em Remotive.
- [x] Preparar Adzuna para query/location quando credenciais existirem.
- [x] Aplicar seguranca:
  - [x] ownership por `userId`;
  - [x] provider allowlist;
  - [x] normalizacao Unicode;
  - [x] bloqueio de caracteres de controle;
  - [x] limite de preferencias por usuario;
  - [x] rate limit nos endpoints;
  - [x] logs com hash de keyword.
- [x] Validar runtime:
  - [x] `QA` executou com `SUCCESS`;
  - [x] `Java Developer` executou com `SUCCESS`;
  - [x] dedupe evitou duplicatas;
  - [x] anonimo recebeu `401`;
  - [x] acesso cruzado retornou `404`.
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-query-driven-ingestion-and-repository-bootstrap.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`
  - [x] `context/DECISIONS.md`

### Pendentes neste ciclo
- [ ] Finalizar varredura de segredos antes do commit.
- [ ] Executar backend tests.
- [ ] Executar frontend build.
- [ ] Inicializar Git local.
- [ ] Configurar remote GitHub.
- [ ] Criar commit inicial seguro.
- [ ] Enviar para `origin/main`.

### Pendentes / proximos ciclos
- [ ] Criar endpoint backend batch/agregador para retornar vaga + match ja ordenados pelo servidor.
- [ ] Criar tela frontend administrativa para o overview de ingestao.
- [ ] Criar teste de navegador para `/vagas` cobrindo login, prioridade, draft e erros HTTP.
- [ ] Avaliar exposicao oficial de `qualityScore` em `VacancyResponse`.

## Priorizacao Segura de Vagas (2026-04-24)

### Concluidas
- [x] Melhorar `/vagas` sem alterar backend, DTO publico, endpoints ou dependencias.
- [x] Preservar backend como fonte de verdade:
  - [x] frontend nao calcula score;
  - [x] frontend nao altera recommendation;
  - [x] frontend nao decide fluxo de negocio.
- [x] Implementar ordenacao por sinais do backend:
  - [x] `score DESC`;
  - [x] `publishedAt DESC`;
  - [x] `qualityScore DESC` quando disponivel.
- [x] Mapear recomendacao para UI:
  - [x] `APPLY` -> Alta prioridade;
  - [x] `REVIEW` -> Revisar;
  - [x] `IGNORE` -> Ignorar.
- [x] Implementar fallback seguro para divergencia:
  - [x] validar `match.vacancyId === vacancy.id`;
  - [x] validar `state === GENERATED`;
  - [x] validar score numerico;
  - [x] validar recommendation conhecida;
  - [x] validar vaga publicada.
- [x] Atualizar card de vaga:
  - [x] score;
  - [x] recommendation;
  - [x] ate 3 strengths;
  - [x] ate 3 gaps;
  - [x] empresa;
  - [x] titulo;
  - [x] data.
- [x] Implementar CTAs seguros:
  - [x] `Aplicar` via draft assistido existente;
  - [x] `Revisar` para detalhe;
  - [x] `Ignorar` local em memoria;
  - [x] bloquear submit concorrente;
  - [x] sucesso visual somente apos resposta HTTP.
- [x] Mapear erros HTTP:
  - [x] `400`;
  - [x] `401`;
  - [x] `403`;
  - [x] `404`;
  - [x] `500`.
- [x] Validar seguranca frontend:
  - [x] sem `dangerouslySetInnerHTML`;
  - [x] sem `innerHTML`;
  - [x] sem `console.`;
  - [x] sem `localStorage`;
  - [x] sem `sessionStorage`.
- [x] Executar build frontend:
  - [x] `cmd /c npm run build`;
  - [x] build passou;
  - [x] `/vagas` e `/candidaturas` compilaram.
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-priorizacao-segura-vagas.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`

### Pendentes / proximos ciclos
- [ ] Criar endpoint backend batch/agregador para retornar vaga + match ja ordenados pelo servidor.
- [ ] Criar teste de navegador para `/vagas` cobrindo login, prioridade, draft e erros HTTP.
- [ ] Avaliar exposicao oficial de `qualityScore` em `VacancyResponse`.
- [ ] Decidir se `Ignorar` deve virar estado persistido no backend.

## Painel Operacional de Ingestao Admin (2026-04-24)

### Concluidas
- [x] Criar endpoint administrativo:
  - [x] `GET /api/v1/admin/ingestion/overview`.
- [x] Proteger endpoint com role `ADMIN`.
- [x] Criar DTO agregado:
  - [x] `providers`;
  - [x] `totals`;
  - [x] `quality`;
  - [x] `dedupe`;
  - [x] `recent`.
- [x] Criar use case:
  - [x] `AdminIngestionOverviewUseCase`.
- [x] Criar service de agregacao:
  - [x] calculo de totais;
  - [x] media de `qualityScore`;
  - [x] taxa de dedupe;
  - [x] volume 24h/7d;
  - [x] normalizacao de status operacional.
- [x] Criar port/repository de leitura agregada:
  - [x] providers em `vacancy_sources`;
  - [x] totais de runs em `vacancy_ingestion_runs`;
  - [x] ultima execucao por provider;
  - [x] agregados de vagas em `vacancies`;
  - [x] top quality flags com limite fixo.
- [x] Garantir que a resposta nao expoe:
  - [x] `raw_payload`;
  - [x] PII;
  - [x] dados individuais de vagas;
  - [x] payload externo bruto.
- [x] Corrigir falha runtime por tipo temporal inesperado no adapter.
- [x] Adicionar log seguro para erro inesperado no handler global.
- [x] Validar runtime:
  - [x] `ADMIN` -> `200`;
  - [x] anonimo -> `401`;
  - [x] `USER` -> `403`.
- [x] Validar dados retornados:
  - [x] `6` providers;
  - [x] `3` ativos;
  - [x] `222` vagas totais;
  - [x] `219` vagas visiveis;
  - [x] `3` duplicadas;
  - [x] dedupe `1.35%`.
- [x] Executar backend tests:
  - [x] `.\mvnw.cmd -B test -DskipITs`;
  - [x] `76` testes, `0` falhas, `0` erros, `2` skipped.
- [x] Rebuild de staging:
  - [x] `docker compose -f apps\backend\infra\staging\docker-compose.yml up -d --build`.
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-painel-operacional-ingestao-admin.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`

### Pendentes / proximos ciclos
- [ ] Criar tela frontend administrativa para consumir `GET /api/v1/admin/ingestion/overview`.
- [ ] Criar smoke test versionado do endpoint admin em Postgres.
- [ ] Melhorar UX/semantica de `SKIPPED_LOCKED` no painel.
- [ ] Avaliar indices dedicados para agregacoes conforme crescimento do volume.

## Expansao Segura de Ingestao de Vagas (2026-04-24)

### Concluidas
- [x] Diagnosticar baixo volume de vagas.
- [x] Confirmar que apenas `REMOTIVE` estava ativo antes da expansao.
- [x] Confirmar gargalo: Remotive retornando janela efetiva de ~20 vagas por execucao.
- [x] Validar scheduler ativo e lock multi-instancia (`SKIPPED_LOCKED` quando concorrente).
- [x] Validar volume inicial no banco:
  - [x] `22` vagas totais;
  - [x] `22` Remotive;
  - [x] `0` duplicadas.
- [x] Ajustar Remotive para envio de `limit`.
- [x] Adicionar categorias controladas em Remotive.
- [x] Adicionar limite interno/backoff para evitar consumo agressivo.
- [x] Preservar dedupe em memoria por `externalJobId`.
- [x] Ativar fontes curadas:
  - [x] `Greenhouse Stripe`;
  - [x] `Greenhouse Figma`.
- [x] Validar `boardToken` Greenhouse por regex.
- [x] Aplicar fallback seguro de `company = tenant` quando provider nao envia `company_name`.
- [x] Omitir campos HTML ricos de `raw_payload`:
  - [x] `description` em Remotive;
  - [x] `content` em Greenhouse.
- [x] Sanear legado Remotive via migration `V10`.
- [x] Criar migrations:
  - [x] `V8__remotive_controlled_category_ingestion.sql`;
  - [x] `V9__enable_curated_greenhouse_sources.sql`;
  - [x] `V10__sanitize_legacy_remotive_raw_payload.sql`.
- [x] Executar ingestao manual autenticada:
  - [x] `REMOTIVE`: `fetched=20`, `inserted=0`, `skipped=20`;
  - [x] `GREENHOUSE/stripe`: `fetched=120`, `inserted=120`;
  - [x] `GREENHOUSE/figma`: `fetched=80`, `inserted=80`.
- [x] Validar estado final:
  - [x] `222` vagas totais;
  - [x] `219` vagas visiveis;
  - [x] `3` duplicadas.
- [x] Validar `GET /api/v1/vacancies?page=0&size=5` autenticado -> `200`, `totalElements=219`.
- [x] Validar chamada sem autenticacao -> `401`.
- [x] Executar backend tests:
  - [x] `.\mvnw.cmd -B test -DskipITs`;
  - [x] `74` testes, `0` falhas, `0` erros, `2` skipped.
- [x] Rebuild de staging com Flyway ate `V10`.
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-expansao-segura-ingestao-vagas.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`

### Pendentes / proximos ciclos
- [ ] Criar lista curada adicional de providers/boards confiaveis.
- [ ] Parametrizar limites por ambiente com allowlist, payload cap e max jobs por fonte.
- [ ] Criar smoke test versionado de ingestao (`admin run -> banco -> GET /vacancies`).
- [ ] Executar campanha prolongada de dedupe cross-source.
- [ ] Avaliar agendamento unico/lideranca para reduzir ruido em staging multi-instancia.

## UX de Candidatura + Sessao Segura Frontend (2026-04-24)

### Concluidas
- [x] Mapear status tecnico para labels amigaveis em candidaturas.
- [x] Implementar timeline com estado atual, historico e proximo passo.
- [x] Garantir status atual como visual (nao clicavel).
- [x] Exibir apenas proxima transicao valida no frontend.
- [x] Tratar `400/401/403/404/500` com mensagens seguras e amigaveis.
- [x] Remover persistencia de token em `localStorage`/`sessionStorage`.
- [x] Validar sessao em memoria com reidratacao via refresh HttpOnly cookie.
- [x] Ajustar client auth para enviar `{}` em `refresh/logout` quando sem token explicito.
- [x] Validar login, chamadas autenticadas, refresh sem cookie e logout.
- [x] Documentar comportamento de sessao e limitacao de cookie `Secure` em HTTP local.
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-ux-candidatura-sessao-segura.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`

### Pendentes / proximos ciclos
- [ ] Validar fluxo de sessao completo em staging HTTPS.
- [ ] Criar smoke test versionado para autenticacao frontend.
- [ ] Definir estrategia de medio prazo para BFF de autenticacao (sem alterar contratos atuais).

## Runtime Validation + Flow Consistency (2026-04-24)

### Concluidas
- [x] Mapear fluxo real `vacancy -> match -> draft -> status -> tracking`.
- [x] Validar `GET /api/v1/vacancies` em staging local.
- [x] Validar `POST /api/v1/matches` com `resumeVariantId` real.
- [x] Validar `GET /api/v1/matches/vacancy/{id}` retornando match stateful persistido.
- [x] Validar `POST /api/v1/applications/drafts`.
- [x] Validar transicao invalida `DRAFT -> APPLIED` retornando `400 BAD_REQUEST`.
- [x] Validar transicoes validas `DRAFT -> READY_FOR_REVIEW -> APPLIED`.
- [x] Validar `GET /api/v1/applications/{id}/tracking` retornando `CREATED,SCREENING,SUBMITTED`.
- [x] Confirmar ownership no tracking com usuario diferente recebendo `404 NOT_FOUND`.
- [x] Identificar drift de runtime/schema: backend rodava imagem antiga com Flyway em `v6`.
- [x] Realinhar staging via rebuild/restart:
  - [x] `docker compose -f apps\backend\infra\staging\docker-compose.yml up -d --build`
- [x] Confirmar Flyway `v7` aplicada:
  - [x] `vacancy data quality and deduplication`
- [x] Ajustar UI de candidaturas para desabilitar clique no status atual.
- [x] Executar backend tests:
  - [x] `.\mvnw.cmd -B test -DskipITs`
  - [x] `72` testes, `0` falhas, `0` erros, `2` skipped.
- [x] Executar frontend build:
  - [x] `cmd /c npm run build`
- [x] Criar checkpoint:
  - [x] `docs/checkpoints/2026-04-24-runtime-validation-flow-consistency.md`
- [x] Atualizar contexto:
  - [x] `context/CHECKPOINT_TECNICO_ATUAL.md`
  - [x] `context/PROJECT_STATE.md`
  - [x] `context/TASKS.md`

### Pendentes / proximos ciclos
- [ ] Criar smoke test operacional versionado para validar Flyway + fluxo E2E em staging.
- [ ] Definir se smoke test roda via script operacional, perfil Maven dedicado ou pipeline CI controlado.
- [ ] Expor endpoint backend de listagem de variantes por curriculo para remover inferencia via drafts.
- [ ] Avaliar BFF/httpOnly para reduzir risco de token em `sessionStorage`.

## Bloco Matching V1 - checkpoint parcial (2026-04-22)

### Concluidas (parcial)
- [x] Evoluir DTO de match para incluir estado/contexto/versao/keywords.
- [x] Criar `MatchGenerateRequest` e `MatchSummaryResponse`.
- [x] Adicionar enum `MatchState`.
- [x] Evoluir `MatchingUseCase` para separar leitura x geracao.
- [x] Evoluir `MatchController` com:
  - [x] `POST /api/v1/matches`
  - [x] `GET /api/v1/matches/vacancy/{vacancyId}`
  - [x] `GET /api/v1/matches/vacancy/{vacancyId}/summary`
  - [x] alias legado `GET /api/v1/matches/{vacancyId}` para leitura.
- [x] Reescrever fluxo de `MatchingUseCaseService` para:
  - [x] gerar explicitamente
  - [x] retornar estado ausente sem falha catastrofica no read
  - [x] persistir recommendation/strengths/gaps/keywords/algorithmVersion/generatedAt.
- [x] Criar migration `V6__matching_v1_deterministic_pipeline.sql`.
- [x] Atualizar rate-limit resolver e endpoint tags para novos paths.
- [x] Adaptar camada IA para novo contrato deterministico (`scoreBreakdown` + enum recommendation).
- [x] Ajustar teste de seguranca para contrato de estado em ausencia de variante.
- [x] Ajustar frontend `types` e client de matching para novos endpoints.

### Pendentes para fechamento do bloco
- [ ] Finalizar adaptacao do frontend para usar `state` vindo do backend como caminho primario.
- [ ] Revisar telas que ainda leem `breakdown` antigo e migrar para `scoreBreakdown`.
- [ ] Executar:
  - [ ] `.\mvnw.cmd -B test -DskipITs`
  - [ ] `npm run build` (frontend)
  - [ ] validacao runtime com geracao real de match.
- [ ] Atualizar checkpoint final de matching com evidencias de runtime.

## Bloco Operacional - Ativacao de ingestao em ambiente vazio

### Concluidas
- [x] Validar estado inicial (`vacancies=0`, `vacancy_ingestion_runs=0`) em ambiente recriado.
- [x] Confirmar fontes configuradas e habilitadas (`REMOTIVE` ativa; demais desabilitadas por padrao).
- [x] Garantir usuario `ADMIN` funcional para execucao manual auditavel.
- [x] Validar RBAC no endpoint admin de ingestao:
  - [x] `USER` bloqueado (`403`)
  - [x] `ADMIN` permitido (`200`).
- [x] Executar ingestao manual real via `POST /api/v1/admin/vacancies/ingestion/runs`.
- [x] Confirmar persistencia em banco (`vacancies > 0`, `runs > 0`).
- [x] Confirmar `GET /api/v1/vacancies` retornando itens.
- [x] Validar scheduler controlado por flag com lock (`SCHEDULED SUCCESS` + `SKIPPED_LOCKED`).
- [x] Implementar bootstrap minimo de ingestao para banco vazio (flag + guardas).
- [x] Documentar operacao em `docs/operations/ingestion-bootstrap.md`.

### Pendentes / proximos ciclos
- [ ] Campanha de qualidade de dados e deduplicacao cross-source (proximo bloco recomendado).

## Bloco 5 - Operacao real controlada / validacao de staging

### Concluidas
- [x] Revalidar gate do Bloco 4 contra codigo real.
- [x] Corrigir divergencia de gate (teste de headers acoplado indevidamente a health com Redis down).
- [x] Segregar configuracao por ambiente (`application-dev`, `application-staging`, `application-prod`).
- [x] Implementar validacao fail-fast de misconfiguration critica em staging/prod.
- [x] Implementar token tecnico para scraping seguro de `/actuator/metrics` e `/actuator/prometheus`.
- [x] Revisar e calibrar regras de alerta (401/403/429/5xx/fallback/unavailable/p95/p99).
- [x] Versionar stack de staging com Redis/Postgres/Prometheus/Alertmanager e 2 instancias backend.
- [x] Criar teste de carga controlada multi-instancia condicionado por flag (`StagingOperationalLoadTest`).
- [x] Criar script operacional de carga (`ops/loadtest/run-staging-load.ps1`).
- [x] Executar suite backend (`.\mvnw.cmd -B test`) apos mudancas.

### Pendentes / bloqueadas nesta execucao
- [ ] Executar validacao real de carga multi-instancia no staging local.
  - Motivo: engine Docker indisponivel (`dockerDesktopLinuxEngine` ausente).
- [ ] Coletar evidencias reais de firing/roteamento de alertas no Alertmanager.
- [ ] Registrar baseline final de thresholds com dados reais de trafego de staging.

## Bloco 8 - IA controlada + melhoria de CV + sugestoes inteligentes

### Concluidas
- [x] Validar gate do Bloco 7 antes de iniciar IA.
- [x] Implementar modulo `ai` separado por camadas.
- [x] Definir contratos de entrada/saida para 3 fluxos de enriquecimento.
- [x] Implementar pipeline controlado (prompt fechado, validacao de schema, fallback seguro).
- [x] Adicionar provider HTTP configuravel com allowlist/HTTPS/timeout/retry.
- [x] Adicionar rate limit dedicado das rotas de IA.
- [x] Adicionar metricas e logs operacionais de IA.
- [x] Cobrir testes de schema, fallback, misconfiguration e rate limit.

### Pendentes / proximos ciclos
- [x] Validacao online com provider real em staging (Bloco 8.1).
- [x] Fechamento 8.1-final: `match-enrichment` sem fallback indevido + usage/tokens/custo capturados.
- [ ] Campanha prolongada de baseline de custo/latencia (p95/p99) por fluxo IA.
- [ ] Cenario controlado de schema invalido do provider (proxy/mock) para validar endurecimento adicional.
- [ ] Hardening adicional para deteccao semantica de prompt injection avancado.

## Bloco Produto - Frontend SaaS

### Concluidas
- [x] Mapear endpoints reais do backend para auth/vacancies/matching/ai/applications.
- [x] Reestruturar frontend em `app/(dashboard)` + modulos `components/lib/hooks/types`.
- [x] Implementar login funcional e sessao client-side.
- [x] Proteger rotas privadas com guarda e redirecionamento.
- [x] Implementar dashboard inicial com indicadores operacionais de decisao.
- [x] Implementar listagem de vagas com filtros basicos e score/recomendacao visiveis.
- [x] Implementar pagina de ranking por score deterministico.
- [x] Implementar detalhe da vaga com breakdown + painel de IA.
- [x] Integrar fluxos IA (`enrichment`, `cv-improvement`, `application-draft`) com loading/erro/fallback.
- [x] Implementar pagina de candidaturas com atualizacao de status existente.
- [x] Adicionar Tailwind e validar build/typecheck do frontend.

### Pendentes / proximos ciclos
- [ ] Criar endpoint backend simplificado para marcar intencao de aplicacao sem exigir `resumeVariantId`.
- [ ] Expor no contrato de vagas campos de origem e data para UX completa de listagem.
- [ ] Endurecer sessao no frontend com estrategia BFF/httpOnly quando escopo de produto permitir.

## Mitigacao 429 Frontend - Matching unitario

### Concluidas
- [x] Diagnosticar rajada de `GET /api/v1/matches/{vacancyId}` em dashboard/vagas/ranking.
- [x] Remover `Promise.all` massivo para carregamento de match.
- [x] Implementar carregamento progressivo com concorrencia limitada.
- [x] Implementar cache em memoria + deduplicacao de chamadas de match em andamento.
- [x] Adicionar retry leve com backoff para `429`.
- [x] Ajustar UX para estado parcial sob rate limit (sem quebrar pagina inteira).
- [x] Ajustar detalhe da vaga para carregar base primeiro e match de forma independente.
- [x] Validar build/typecheck do frontend apos mitigacao.
- [x] Diagnosticar e tratar `404` de match como estado funcional (sem variante de curriculo) em vez de erro fatal.
- [x] Corrigir detalhe/listagem/ranking para degradacao segura quando match estiver indisponivel.
- [x] Ajustar tipagem/renderizacao de `seniority` nulo e explicitar ausencia de `publishedAt` no contrato atual.

### Pendentes / proximos ciclos
- [ ] Migrar adaptador de match para endpoint agregador/backend batch quando disponivel.
- [ ] Adicionar metricas client-side de taxa de `429` por tela para observabilidade de UX.

## Bloco Produto/UX - Curriculos e candidatura assistida

### Concluidas
- [x] Mapear endpoints reais de curriculos/variantes/aplicacoes antes de implementar UX.
- [x] Criar pagina `/curriculos` com estado vazio, listagem de curriculos e registro de curriculo base (metadata).
- [x] Explicitar limitacao de contrato: backend nao expoe listagem direta de variantes.
- [x] Classificar estado de match para diferenciar `sem curriculo`, `sem variante`, `indisponivel` e erro real.
- [x] Adicionar CTA contextual no detalhe da vaga para:
  - [x] ir para curriculos quando faltar curriculo
  - [x] criar variante da vaga quando faltar variante
  - [x] criar draft quando `resumeVariantId` estiver disponivel
- [x] Integrar callback do AI draft para apoiar criacao de candidatura.
- [x] Validar build/typecheck apos alteracoes.

### Pendentes / proximos ciclos
- [ ] Expor endpoint backend de listagem de variantes por curriculo para eliminar inferencia via drafts.
- [ ] Expor endpoint simplificado de intencao de candidatura sem exigir `resumeVariantId`.

## Bloco Backend+Produto - PDF + jobUrl + fluxo assistido real

### Concluidas
- [x] Implementar migration `V4` para metadados de arquivo de curriculo (`storage_path`, `content_type`, `file_size`, `checksum`, `is_base`, `uploaded_at`).
- [x] Implementar upload real de curriculo PDF em `POST /api/v1/resumes` (multipart).
- [x] Validar upload por assinatura `%PDF-`, content type e limite de tamanho.
- [x] Implementar storage privado local para PDF com checksum SHA-256.
- [x] Expor `jobUrl` e `publishedAt` em `VacancyResponse`.
- [x] Criar endpoint `POST /api/v1/applications/drafts/assisted`.
- [x] Endurecer ownership no `createDraft` tradicional (variante precisa pertencer ao usuario e vaga).
- [x] Atualizar frontend `/curriculos` para upload real de PDF.
- [x] Atualizar detalhe da vaga para CTA de vaga original quando `jobUrl` existir.
- [x] Atualizar detalhe para criacao de draft assistido sem automacao externa.
- [x] Executar testes backend e build frontend apos mudancas.

### Pendentes / proximos ciclos
- [x] Validacao runtime completa em ambiente local/staging reiniciado com migration `V4` aplicada.
- [ ] Endpoint de listagem de variantes por curriculo (remove inferencia no frontend).
- [ ] Melhorar UX para casos sem `jobUrl` na origem (fallback orientado por fonte).

### Evidencia de fechamento runtime V4 (2026-04-22)
- [x] Confirmar em logs de boot que Flyway validou `4 migrations`.
- [x] Confirmar em `flyway_schema_history` que versao `4` foi aplicada com sucesso.
- [x] Confirmar em `information_schema.columns` presenca das colunas novas em `resumes`.
- [x] Revalidar `POST /api/v1/resumes` com multipart PDF autenticado retornando `201`.
- [x] Confirmar persistencia de metadados e arquivo em storage privado.
