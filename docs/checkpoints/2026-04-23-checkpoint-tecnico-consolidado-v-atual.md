# Checkpoint Técnico Consolidado — ApplyFlow
Data: 2026-04-23  
Escopo: consolidação técnica pós estabilização de Matching V1, UX core, Data Quality/Dedup e Candidatura Assistida com runtime hardening.

## 1) Versão atual do sistema (estado operacional)
- Ingestão de vagas multi-source operacional.
- Matching V1 determinístico, stateful e auditável.
- Frontend estabilizado para contrato stateful.
- IA desativada por padrão no frontend quando backend não expõe capacidade.
- Pipeline de qualidade de dados ativo na ingestão (normalização canônica + dedup cross-provider soft + quality score).
- Fluxo de candidatura assistida operacional com timeline e ownership.
- Hardening de runtime aplicado para eliminar `500` em tracking de candidatura por dados legados/inconsistentes.

## 2) Linha de evolução consolidada (rodada atual)

### 2.1 Matching V1 (finalização + segurança + runtime)
- Fluxo stateful consolidado com geração explícita:
  - `POST /api/v1/matches`
  - `GET /api/v1/matches/vacancy/{vacancyId}`
  - `GET /api/v1/matches/vacancy/{vacancyId}/summary`
  - `GET /api/v1/matches/{vacancyId}` (legado de leitura mantido)
- Testes de ownership endurecidos para evitar vazamento cross-user de score/recommendation/strengths/gaps.
- Backend tornou leitura fail-safe para resultados persistidos inconsistentes.
- Handler global ajustado para erros de argumento/UUID inválido retornarem `422` em vez de `500`.

### 2.2 Core UX stabilization (IA sem endpoint em runtime)
- Causa: frontend exibia botões IA com backend sem capacidade efetiva (`/api/v1/ai/*` indisponível/disabled).
- Correção:
  - feature flag frontend `NEXT_PUBLIC_AI_ACTIONS_ENABLED` (default `false`);
  - painel IA renderizado condicionalmente;
  - tratamento explícito de `404` no painel para mensagem funcional.
- Resultado: fim do fluxo quebrado por CTA visível chamando endpoint inexistente.

### 2.3 Data Quality & Deduplication (produção)
- Nova etapa determinística na ingestão:
  - `canonicalTitle`
  - `canonicalCompanyName`
  - `canonicalLocation`
  - `normalizedSeniority`
  - `dedupeKey`
  - `qualityScore` (0–100)
  - `qualityFlags`
- Deduplicação cross-provider por soft-flag:
  - `is_duplicate`
  - `canonical_vacancy_id`
- Sem hard delete e sem perda de `raw_payload`.
- Migration V7 aplicada no código:
  - `V7__vacancy_data_quality_and_deduplication.sql`
  - com backfill canônico, dedupe histórico e índices.

### 2.4 Assisted Application Flow (candidatura assistida)
- Fluxo de candidatura assistida consolidado sem automação abusiva:
  - criação de draft vinculado;
  - progressão de status;
  - timeline de tracking.
- Endpoint novo:
  - `GET /api/v1/applications/{id}/tracking`
- Ownership garantido por `findByIdAndUserId` antes da leitura da timeline.
- Frontend ajustado para transições válidas de status (não envia transições inválidas para backend).

### 2.5 Application Tracking Runtime Hardening
- Problema real em runtime: `GET /api/v1/applications/{id}/tracking` retornando `500`.
- Causa técnica: parse rígido de stage (`TrackingStage.valueOf`) quebrando com valor legado/inválido.
- Correção fail-safe:
  - mapeamento backward-compatible para valores legados;
  - fallback de stage desconhecido para `CLOSED` com log de warning;
  - fallback de `createdAt` nulo para evitar NPE.
- Resultado: endpoint resiliente a dados parciais/legados sem quebrar contrato.

## 3) Contratos e endpoints (estado atual)

### 3.1 Matching
- `POST /api/v1/matches` (geração explícita)
- `GET /api/v1/matches/vacancy/{vacancyId}` (leitura stateful)
- `GET /api/v1/matches/vacancy/{vacancyId}/summary` (resumo stateful)
- `GET /api/v1/matches/{vacancyId}` (compat)

### 3.2 Applications
- `GET /api/v1/applications`
- `GET /api/v1/applications/{id}`
- `GET /api/v1/applications/{id}/tracking`
- `POST /api/v1/applications/drafts`
- `POST /api/v1/applications/drafts/assisted`
- `PATCH /api/v1/applications/{id}/status`

## 4) Segurança aplicada (resumo objetivo)
- Isolamento por `userId` reforçado em leitura/escrita de aplicações, tracking e matching.
- Sem BOLA/IDOR no tracking: lookup com ownership obrigatório.
- Sem automação de candidatura externa, sem scraping, sem credenciais de terceiros.
- Sem IA no bloco de candidatura assistida e sem dependência externa no bloco de qualidade.
- Erros de input/routing endurecidos para não escalar para `500` em cenários funcionais esperados.

## 5) Evidências de validação (local)
- Backend:
  - `.\mvnw.cmd -B test` com sucesso nas últimas rodadas:
    - 70/71/72 testes verdes conforme evolução incremental dos blocos.
- Frontend:
  - `cmd /c npx tsc --noEmit` sem erros.
  - `cmd /c npm run build` com sucesso.

## 6) Arquivos-chave alterados nesta consolidação
- Backend:
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/matching/...`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/interfaces/http/GlobalExceptionHandler.java`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/application/service/VacancyIngestionNormalizer.java`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/persistence/VacancyIngestionPersistenceRepository.java`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/vacancies/infrastructure/persistence/entity/VacancyJpaEntity.java`
  - `apps/backend/src/main/resources/db/migration/V7__vacancy_data_quality_and_deduplication.sql`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/applications/application/service/StubApplicationService.java`
  - `apps/backend/src/main/java/com/applyflow/jobcopilot/applications/interfaces/http/ApplicationController.java`
- Frontend:
  - `apps/frontend/src/lib/config/features.ts`
  - `apps/frontend/src/components/ai/AiActionPanel.tsx`
  - `apps/frontend/src/app/(dashboard)/vagas/[id]/page.tsx`
  - `apps/frontend/src/app/(dashboard)/candidaturas/page.tsx`
  - `apps/frontend/src/lib/api/applications.ts`
  - `apps/frontend/src/types/api.ts`

## 7) Riscos residuais
- Dedupe cross-provider pode produzir falso positivo em vagas muito semelhantes (mitigado por soft-flag, não destrutivo).
- Timeline usa `notes` técnicas curtas; pode evoluir para labels UX mais amigáveis.
- Métricas de custo de IA continuam em log estruturado (não em série agregada dedicada), mas IA está desativada por padrão no frontend.

## 8) Decisão de versionamento do contexto
- Este checkpoint representa a **versão técnica atual consolidada em 2026-04-23**.
- Próxima iteração recomendada:
  1. Refino de UX de timeline (labels amigáveis).
  2. Painel admin para auditoria de dedupe/quality flags.
  3. Campanha runtime em staging com evidências HTTP pós-deploy (matching + applications tracking).

