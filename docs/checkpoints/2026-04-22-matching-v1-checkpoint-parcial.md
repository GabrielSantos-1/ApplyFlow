# Checkpoint Parcial - Matching V1 Deterministico

**Data:** 2026-04-22  
**Escopo:** implementar pipeline de matching deterministico auditavel e reduzir ruído operacional de ausencia de match.

## 1) Diagnostico da rodada

- Ingestao estava operacional e frontend listava vagas normalmente.
- O problema observado era `404` frequente em `GET /api/v1/matches/{vacancyId}`.
- Causa funcional: match nao gerado/contexto incompleto (sem curriculo/sem variante) e contrato de leitura pouco explicito.
- Causa arquitetural: endpoint de leitura misturava comportamento de geracao com retorno por excecao.

## 2) O que foi implementado (parcial)

### Backend - contratos e dominio
- `MatchAnalysisResponse` evoluido com:
  - `state`
  - `algorithmVersion`
  - `generatedAt`
  - `hasResumeContext`
  - `hasVariantContext`
  - `keywordsToAdd`
  - `scoreBreakdown`
- Novos DTOs:
  - `MatchGenerateRequest`
  - `MatchSummaryResponse`
- Novo enum:
  - `MatchState`

### Backend - use case e controller
- `MatchingUseCase` evoluido para separar leitura/geracao.
- `MatchController` com endpoints:
  - `POST /api/v1/matches`
  - `GET /api/v1/matches/vacancy/{vacancyId}`
  - `GET /api/v1/matches/vacancy/{vacancyId}/summary`
  - alias legado `GET /api/v1/matches/{vacancyId}` mantido.
- `MatchingUseCaseService` reestruturado para:
  - gerar match de forma explicita e persistente;
  - retornar estados de ausencia de contexto na leitura;
  - preservar ownership por `userId`.

### Backend - persistencia
- `MatchResultJpaEntity` ampliada com:
  - `resume_id`
  - `recommendation`
  - `strengths_json`
  - `gaps_json`
  - `keywords_to_add_json`
  - `algorithm_version`
  - `generated_at`
- Unicidade declarada por `(user_id, vacancy_id)`.
- `MatchResultJpaRepository` com `findByUserIdAndVacancyId`.

### Backend - migration
- Criada `V6__matching_v1_deterministic_pipeline.sql` com:
  - alter table + novos campos;
  - backfill (`resume_id`, recommendation, defaults json/algorithmVersion/generatedAt);
  - deduplicacao historica por `(user_id, vacancy_id)`;
  - constraint unica `uq_match_results_user_vacancy`.

### Observabilidade e seguranca operacional
- `OperationalMetricsService` e `MicrometerOperationalMetricsService` ampliados com sinais de matching:
  - `matches_generated_total`
  - `match_generation_duration`
  - `match_context_missing_total`
- `EndpointTagResolver` e `RateLimitFilter` atualizados para novos paths de matching.

### Integracoes afetadas
- Camada IA ajustada para novo contrato de deterministic match:
  - `AiEnrichmentService`
  - `AiFallbackFactory`
  - `AiPromptFactory`

### Frontend (parcial)
- `types/api.ts` atualizado para novo shape de `MatchAnalysisResponse`.
- `lib/api/matching.ts` atualizado para:
  - `byVacancy` em `/api/v1/matches/vacancy/{vacancyId}`
  - `generate` em `POST /api/v1/matches`
- `match-adapter.ts` restaurado/ajustado para manter continuidade da base.

## 3) Testes/validacao nesta rodada

**Nao concluido ainda (checkpoint parcial):**
- suite backend nao executada apos todas as alteracoes;
- build/typecheck frontend nao executado apos mudanca de contratos;
- validacao runtime end-to-end do novo fluxo de matching pendente.

## 4) Riscos atuais

1. Existe risco de quebra de compilacao ate fechar ajuste fino de assinaturas entre backend/IA/frontend.
2. Sem rodada de testes, nao ha evidencia final de regressao zero.
3. Sem runtime validation, contrato stateful ainda nao foi comprovado ponta a ponta.

## 5) Proximo passo exato para retomada

1. Rodar backend tests (`mvn test`) e corrigir quebras de assinatura.
2. Rodar frontend build/typecheck e alinhar consumo de `scoreBreakdown/state`.
3. Validar runtime:
   - `GET /api/v1/matches/vacancy/{vacancyId}` para todos os estados;
   - `POST /api/v1/matches` com/sem contexto e com `forceRegenerate`.
4. Publicar checkpoint final do bloco Matching V1 com evidencias objetivas.
