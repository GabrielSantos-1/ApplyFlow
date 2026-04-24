# Checkpoint — 2026-04-23 — Data Quality & Deduplication (Production-Ready)

## Diagnóstico
- Problema confirmado: deduplicação existente cobria apenas a mesma origem (`source+tenant+externalJobId`) e não reduzia duplicatas cross-provider.
- Impacto: vagas repetidas, normalização fraca de campos e perda de precisão do ranking/matching por dados sujos.

## Correções aplicadas
1. **Normalização canônica determinística**
   - `VacancyIngestionNormalizer` agora gera:
     - `canonicalTitle`
     - `canonicalCompanyName`
     - `canonicalLocation`
     - `normalizedSeniority`
     - `dedupeKey` determinístico
     - `qualityScore` (0-100)
     - `qualityFlags` (explicáveis)
   - Mantido `rawPayload` original, sem perda de dado bruto.

2. **Contrato de ingestão expandido**
   - `NormalizedVacancyRecord` expandido com campos canônicos, score e flags de qualidade, e chave de deduplicação.

3. **Persistência e soft-dedup cross-provider**
   - `VacancyJpaEntity` recebeu colunas de qualidade/deduplicação.
   - `VacancyIngestionPersistenceRepository` aplica dedup com soft flag:
     - `is_duplicate=true`
     - `canonical_vacancy_id` apontando para a vaga canônica
   - Sem hard delete e sem descarte silencioso de registro.

4. **Visão de produto sem duplicatas**
   - `StubVacancyService` filtra duplicatas soft-flagged no `list` (`is_duplicate=false`) para reduzir ruído sem apagar dados.

5. **Migration de banco auditável**
   - Nova migration: `V7__vacancy_data_quality_and_deduplication.sql`
   - Inclui:
     - novas colunas de qualidade/dedupe
     - backfill determinístico dos campos canônicos
     - cálculo inicial de `quality_score`
     - geração de `dedupe_key`
     - marcação de duplicatas históricas via janela (`row_number`)
     - índices e FK para `canonical_vacancy_id`

## Evidências de validação
- Backend: `.\mvnw.cmd -B test`
  - Resultado: **BUILD SUCCESS**
  - `Tests run: 70, Failures: 0, Errors: 0, Skipped: 2`

## Segurança e auditoria
- Sem dependência externa.
- Sem IA.
- Sem mudança de regra do matching.
- Sem remoção física de dados.
- Dedupe e score com regras determinísticas e rastreáveis no payload normalizado e no banco.

## Riscos residuais
- Falsos positivos de dedupe podem ocorrer em vagas semanticamente parecidas com metadados quase idênticos.
- Mitigação atual: dedupe por soft flag (reversível) e preservação de todos os registros brutos.
