# DECISIONS.md

## DECISION-008 - Redis como mecanismo principal de rate limit
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca/disponibilidade`
- **Decisao:** `RedisRateLimitService` como principal, com fallback local somente por flag.
- **Motivo:** consistencia em ambiente distribuido sem indisponibilidade silenciosa.

## DECISION-009 - Fallback de rate limit explicitamente observavel
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca/observabilidade`
- **Decisao:** Explicitar modo em header (`X-RateLimit-Mode`) e registrar eventos de indisponibilidade/excedencia.
- **Motivo:** evitar comportamento magico e permitir investigacao.

## DECISION-010 - Headers de seguranca centralizados com CSP por ambiente
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca`
- **Decisao:** Centralizar em `SecurityConfig` e parametrizar via `SecurityProperties`.
- **Motivo:** reduzir misconfiguration e manter politica auditavel.

## DECISION-011 - Sanitizacao defensiva padronizada em componente compartilhado
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca/validacao`
- **Decisao:** `TextSanitizer` com normalizacao Unicode, remocao de controles invisiveis, truncamento seguro e escape HTML.
- **Motivo:** reduzir risco de XSS armazenado e payloads maliciosos em campos livres.

## DECISION-012 - Auditoria de rastreabilidade obrigatoria em fechamento de bloco
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `processo`
- **Decisao:** Toda alegacao de conclusao deve apontar arquivo e teste executado.
- **Motivo:** evitar discrepancia entre resumo narrativo e estado real do repositorio.

## DECISION-013 - CI sem fallback de segredo hardcoded
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca/ci`
- **Decisao:** Remover fallback inline de `JWT_SECRET_BASE64` no workflow.
- **Motivo:** reforcar politica de nao hardcode de segredo em pipeline.

## DECISION-014 - Micrometer + Actuator como base de observabilidade do Bloco 4
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `observabilidade`
- **Decisao:** Usar stack nativa Spring (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`) e manter metrica de negocio em componente compartilhado.
- **Motivo:** stack auditada, baixo acoplamento e integracao padrao com Prometheus.

## DECISION-015 - Metricas customizadas com cardinalidade controlada
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `observabilidade/seguranca`
- **Decisao:** Tags permitidas limitadas a `endpoint`, `policy`, `outcome`, `mode`, `from`, `to`.
- **Motivo:** evitar explosao de cardinalidade e vazamento de identificadores sensiveis.

## DECISION-016 - Falha de observabilidade nao pode interromper fluxo funcional
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `resiliencia`
- **Decisao:** Emissao de metrica protegida por `safeRecord` com tratamento interno de excecao.
- **Motivo:** observabilidade eh critica, mas nao pode derrubar autenticacao, autorizacao ou API principal.

## DECISION-017 - Simulacao de falha controlada por configuracao, sem endpoint administrativo
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `resiliencia/seguranca`
- **Decisao:** Simulacao `none|latency|unavailable` em rate limit ativada apenas por flags de ambiente.
- **Motivo:** permitir testes de degradacao sem criar backdoor operacional.

## DECISION-018 - Exposicao de actuator sensivel restrita a ADMIN
- **Data:** `2026-04-19`
- **Status:** `aceita`
- **Area:** `seguranca operacional`
- **Decisao:** Liberar publicamente apenas `/actuator/health` e `/actuator/info`; restringir `metrics/prometheus` para `ADMIN`.
- **Motivo:** principio do menor privilegio e reducao de superficie de enumeracao operacional.

## DECISION-019 - Token tecnico dedicado para scraping de metricas
- **Data:** `2026-04-20`
- **Status:** `aceita`
- **Area:** `seguranca operacional/observabilidade`
- **Decisao:** Permitir autenticacao tecnica em `/actuator/metrics*` e `/actuator/prometheus` via header `X-Actuator-Token`, mapeando para `ROLE_ADMIN`.
- **Motivo:** viabilizar Prometheus sem expor endpoint sensivel publicamente e sem depender de JWT interativo.

## DECISION-020 - Fail-fast de configuracao em staging/prod
- **Data:** `2026-04-20`
- **Status:** `aceita`
- **Area:** `seguranca/misconfiguration`
- **Decisao:** Bloquear bootstrap de `staging/prod` quando:
  - `security.rate-limit.redis-enabled=false`
  - `security.rate-limit.fallback-enabled=true`
  - `security.actuator.metrics-token` ausente
- **Motivo:** evitar operacao com configuracao insegura e falsa sensacao de resiliencia distribuida.

## DECISION-021 - ValidaÃƒÂ§ÃƒÂ£o de carga de staging fora da suite default
- **Data:** `2026-04-20`
- **Status:** `aceita`
- **Area:** `qualidade/operacao`
- **Decisao:** `StagingOperationalLoadTest` fica condicionado por `-Dstaging.load.enabled=true`.
- **Motivo:** manter CI deterministica sem abrir mao de teste de carga executavel e versionado.

## DECISION-022 - Matching deterministico sem IA no calculo de score
- **Data:** `2026-04-21`
- **Status:** `aceita`
- **Area:** `matching/seguranca/explicabilidade`
- **Decisao:** O score de matching e calculado apenas por formula deterministica auditavel no backend, com pesos fixos por criterio e breakdown explicito; IA fica restrita a enriquecimento opcional fora da decisao principal.
- **Motivo:** garantir previsibilidade, testabilidade, rastreabilidade e resistencia a manipulacao no nucleo de decisao.

## DECISION-023 - IA como camada auxiliar com score deterministico soberano
- **Data:** `2026-04-21`
- **Status:** `aceita`
- **Area:** `ai/seguranca/arquitetura`
- **Decisao:** A IA no ApplyFlow atua apenas em enriquecimento textual (explicacao, melhorias de CV, rascunho de candidatura), sem poder alterar score, recommendation, deduplicacao ou autorizacao.
- **Motivo:** manter auditabilidade, previsibilidade e resistencia a prompt injection/alucinacao em regras criticas.

## DECISION-024 - Pipeline de IA tratado como dependencia nao confiavel
- **Data:** `2026-04-21`
- **Status:** `aceita`
- **Area:** `ai/operacao`
- **Decisao:** Toda chamada de IA exige prompt fechado/versionado, sanitizacao de entrada, validacao de output por schema, fallback explicito e metricas/logs operacionais.
- **Motivo:** evitar falha silenciosa e manter comportamento seguro sob indisponibilidade ou resposta invalida do provider.

## DECISION-025 - Paridade de configuracao IA obrigatoria em staging multi-instancia
- **Data:** `2026-04-21`
- **Status:** `aceita`
- **Area:** `ai/operacao/misconfiguration`
- **Decisao:** `backend-1` e `backend-2` em staging devem receber o mesmo conjunto de variaveis `AI_*` e limite `RATE_LIMIT_AI_ENRICHMENT_LIMIT` por compose versionado.
- **Motivo:** evitar falso negativo/positivo de validacao operacional por divergencia de runtime entre instancias.

## DECISION-026 - Persistencia de breakdown de matching em JSON tipado
- **Data:** `2026-04-21`
- **Status:** `aceita`
- **Area:** `matching/persistencia/confiabilidade`
- **Decisao:** campos `jsonb` de `match_results` devem ser mapeados como JSON tipado (`JsonNode` + `JdbcTypeCode(SqlTypes.JSON)`), proibindo persistencia de string serializada como atalho.
- **Motivo:** eliminar erro transacional real (`jsonb` vs `varchar`) que causou `500` em fluxo de IA durante validacao operacional.

## DECISION-027 - `strengths` pode ser vazio no schema de match-enrichment
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `ai/schema/explicabilidade`
- **Decisao:** no fluxo `match-enrichment`, `strengths` passa a aceitar lista vazia; `gaps` e `nextSteps` permanecem obrigatorios e nao-vazios.
- **Motivo:** em cenarios reais com score deterministico muito baixo, ausencia de strengths e estado valido; exigir nao-vazio causava `invalid_output` indevido e fallback desnecessario.

## DECISION-028 - Custo de IA deve aceitar sufixo de versao do modelo
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `ai/observabilidade/custo`
- **Decisao:** estimativa de custo para `gpt-4o-mini` passa a aceitar variantes de runtime (`gpt-4o-mini-*`) em vez de comparacao estrita por igualdade.
- **Motivo:** provider retorna `model` versionado; comparacao estrita estava anulando `estimatedCostUsd` mesmo com usage real disponivel.

## DECISION-029 - Frontend consome score/recomendacao apenas do backend
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/produto/seguranca`
- **Decisao:** frontend nao recalcula score nem recommendation; listagem/ranking/detalhe usam exclusivamente `GET /api/v1/matches/{vacancyId}`.
- **Motivo:** preservar determinismo/auditabilidade no backend e evitar divergencia de regra critica no client.

## DECISION-030 - Sessao frontend em sessionStorage como estrategia inicial
- **Data:** `2026-04-22`
- **Status:** `aceita com risco`
- **Area:** `frontend/auth`
- **Decisao:** armazenar access token em `sessionStorage` para viabilizar MVP sem BFF, mantendo logout local e guarda de rota.
- **Motivo:** simplicidade operacional no primeiro ciclo de produto; risco residual de XSS explicitamente documentado para endurecimento posterior com httpOnly/BFF.

## DECISION-031 - Consumo de match no frontend com adaptador controlado
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/performance/confiabilidade`
- **Decisao:** centralizar consumo de `GET /api/v1/matches/{vacancyId}` em adaptador com limite de concorrencia, cache em memoria, dedupe de inflight e retry leve para `429`.
- **Motivo:** reduzir rajada de requests que saturava rate limit e quebrava ranking/detalhe, mantendo contratos atuais do backend.

## DECISION-032 - Preparacao para endpoint agregador sem alterar contrato atual
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/arquitetura/evolucao`
- **Decisao:** telas deixam de consumir `matchingApi` diretamente e passam a consumir `match-adapter`, ponto unico de troca futura para endpoint batch/agregador.
- **Motivo:** permitir migracao futura sem reescrever UI e evitar repeticao de logica de fetch/controle em cada pagina.

## DECISION-033 - `404` de match tratado como indisponibilidade funcional no frontend
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/contrato/ux`
- **Decisao:** respostas `404` de `GET /api/v1/matches/{vacancyId}` passam a ser tratadas como estado esperado de "match ainda nao disponivel", sem quebrar listagem/ranking/detalhe.
- **Motivo:** o backend retorna `NOT_FOUND` quando nao existe variante de curriculo para a vaga; tratar isso como erro fatal degradava a experiencia e mascarava contrato real.

## DECISION-034 - Classificacao funcional de `404` de match por causa de dominio
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/ux/contrato`
- **Decisao:** o frontend passa a classificar `404` de match em subestados (`resume_missing`, `variant_missing`, `not_found`) com base na mensagem contratual retornada pela API.
- **Motivo:** usuario precisa diferenciar ausencia de curriculo base, ausencia de variante e indisponibilidade generica, evitando UX de "falha" para cenarios esperados.

## DECISION-035 - Criacao de variante no detalhe da vaga para destravar match/draft
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `frontend/produto/orquestracao`
- **Decisao:** detalhe da vaga ganha CTA para criar variante via endpoint real (`POST /api/v1/resumes/{id}/variants`) quando estado de match indicar variante ausente.
- **Motivo:** reduz friccao operacional e fecha fluxo assistido sem inventar endpoint novo nem quebrar responsabilidade do backend.

## DECISION-036 - Upload de curriculo PDF com storage privado e checksum
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `backend/resumes/seguranca`
- **Decisao:** `POST /api/v1/resumes` passa a aceitar multipart PDF com validacao de assinatura `%PDF-`, limite de tamanho e armazenamento privado em disco, persistindo metadados (`contentType`, `fileSizeBytes`, `checksum`, `storagePath`, `isBase`).
- **Motivo:** criar entrada natural de curriculo sem expor arquivo em URL publica e sem depender de extensao de arquivo como unica validacao.

## DECISION-037 - Contrato de vaga expoe `jobUrl` e `publishedAt`
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `backend/vacancies/produto`
- **Decisao:** `VacancyResponse` inclui `jobUrl` e `publishedAt` para habilitar CTA real de abertura da vaga original no frontend.
- **Motivo:** fechar fluxo assistido de candidatura manual com saida clara para fonte externa sem automacao perigosa.

## DECISION-038 - Endpoint assistido de draft sem autopilot externo
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `backend/applications/produto`
- **Decisao:** criado `POST /api/v1/applications/drafts/assisted` com resolucao controlada de curriculo/variante e validacao de ownership; mantido sem qualquer envio automatico para terceiros.
- **Motivo:** reduzir friccao de `resumeVariantId` na UX, preservando rastreabilidade, autorizacao e limite de escopo de seguranca.

## DECISION-039 - Falha 500 de upload PDF tratada como desalinhamento de ambiente (nao bug de contrato)
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `operacao/staging/flyway`
- **Decisao:** para incidente de runtime no `POST /api/v1/resumes`, priorizar diagnostico de versao de schema (Flyway + `flyway_schema_history` + `information_schema`) antes de qualquer mudanca de codigo; correcao foi rebuild/restart do staging com aplicacao da V4.
- **Motivo:** evidencias mostraram backend em schema `v3` com codigo exigindo colunas da V4; alteracao de codigo seria mitigacao incorreta e arriscaria regressao de seguranca/contrato.

## DECISION-040 - Ativacao operacional de ingestao com bootstrap controlado por flag
- **Data:** `2026-04-22`
- **Status:** `aceita`
- **Area:** `operacao/ingestao/seguranca`
- **Decisao:** manter duas alavancas operacionais explicitas:
  - bootstrap de ingestao (`INGESTION_BOOTSTRAP_*`) para primeira carga somente em ambiente vazio;
  - bootstrap de admin (`BOOTSTRAP_ADMIN_*`) apenas para recuperacao emergencial, devendo ficar desligado em runtime normal.
- **Motivo:** evitar ambiente "saudavel porem morto" sem afrouxar RBAC, mantendo ativacao auditavel e reversivel por configuracao.

## DECISION-041 - Matching V1 com leitura stateful e geracao explicita
- **Data:** `2026-04-22`
- **Status:** `aceita (checkpoint parcial)`
- **Area:** `matching/produto/seguranca`
- **Decisao:**
  - separar leitura e geracao de match:
    - `POST /api/v1/matches` para gerar/regerar;
    - `GET /api/v1/matches/vacancy/{vacancyId}` para leitura stateful;
  - leitura por vaga retorna estado de dominio (`GENERATED`, `MISSING_RESUME`, `MISSING_VARIANT`, `NOT_GENERATED`) em vez de tratar ausencia de contexto como falha catastrÃƒÂ³fica.
- **Motivo:** remover ambiguidade operacional dos `404` em massa no frontend, preservar auditabilidade do score deterministico e manter ownership por usuario sem vazar match de terceiros.

## DECISION-042 - Ingestao query-driven por preferencias controladas
- **Data:** `2026-04-24`
- **Status:** `aceita`
- **Area:** `backend/ingestao/seguranca`
- **Decisao:** criar `UserJobSearchPreference` com endpoints autenticados para pesquisas controladas por usuario e execucao via scheduler, reutilizando normalizer, dedupe, qualityScore e upsert existentes.
- **Motivo:** aumentar cobertura de vagas por termos como `QA` e `Java Developer` sem scraping agressivo, sem URL arbitraria, sem remover dedupe e sem duplicar regra de negocio no frontend.

## DECISION-043 - Providers de pesquisa com allowlist e sem busca global forcada
- **Data:** `2026-04-24`
- **Status:** `aceita`
- **Area:** `backend/ingestao/providers`
- **Decisao:** habilitar busca incremental em Remotive, preparar Adzuna para `what`/`where` quando houver credenciais e manter Greenhouse como board-curated.
- **Motivo:** cada provider deve respeitar a capacidade real da API; forcar busca global em Greenhouse criaria comportamento inconsistente e risco operacional sem contrato claro.

## DECISION-044 - Bootstrap GitHub com varredura de segredos obrigatoria
- **Data:** `2026-04-24`
- **Status:** `aceita`
- **Area:** `operacao/repositorio/seguranca`
- **Decisao:** antes do primeiro commit/push, criar `.gitignore` raiz abrangente, manter `.env.example` apenas com placeholders e bloquear versionamento de `.env`, temporarios, builds, tokens, dumps e artefatos locais.
- **Motivo:** o repositorio remoto esta vazio; o commit inicial define a linha de base de seguranca e nao pode carregar credenciais ou artefatos sensiveis.

## DECISION-045 - Smoke runtime E2E como artefato operacional versionado
- **Data:** `2026-04-28`
- **Status:** `aceita`
- **Area:** `operacao/runtime/qualidade`
- **Decisao:** formalizar smoke E2E do fluxo principal como script versionado em `apps/backend/ops/smoke`, com validacoes de auth, ownership, transicoes de status, tracking e endpoints de schema recente (`job-search-preferences`), mais orquestracao staging separada.
- **Motivo:** reduzir risco de drift runtime/schema entre codigo e ambiente e criar evidencia repetivel de estabilidade operacional sem alterar contratos de produto.

## DECISION-046 - CI minimo com checks unicos e higiene de repositorio
- **Data:** `2026-04-28`
- **Status:** `aceita`
- **Area:** `ci-cd/repositorio/seguranca`
- **Decisao:** definir checks obrigatorios `backend-test`, `frontend-quality` e `repository-hygiene`, manter smoke runtime como workflow manual, e configurar Dependabot para Maven, npm e GitHub Actions.
- **Motivo:** criar baseline rastreavel para branch protection da `main`, reduzir risco de regressao e impedir versionamento acidental de artefatos sensiveis sem depender de secrets reais no CI padrao.

## DECISION-047 - Preparacao publica exige documentacao e politica de seguranca
- **Data:** `2026-04-29`
- **Status:** `aceita`
- **Area:** `repositorio/publicacao/seguranca`
- **Decisao:** preparar README publico, SECURITY.md, CONTRIBUTING.md, exemplos de ambiente e documentacao de estrutura para exposicao publica controlada.
- **Motivo:** melhorar clareza e seguranca operacional para exposicao publica.

## DECISION-048 - Licenca MIT para publicacao publica
- **Data:** `2026-04-29`
- **Status:** `aceita`
- **Area:** `repositorio/licenciamento`
- **Decisao:** licenciar o ApplyFlow sob MIT License, com copyright de 2026 para Gabriel Santos.
- **Motivo:** permitir reutilizacao publica com termos simples e reconhecidos, removendo o bloqueio de licenca indefinida antes da publicacao.
