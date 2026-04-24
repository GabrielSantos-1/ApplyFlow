# Checkpoint - 2026-04-21 - Bloco 6 Ingestao Real de Vagas

## Status
`concluido com base profissional de ingestao implementada e validada em testes`

## Gate de entrada
- Checkpoint anterior (`Bloco 5.3`) e `PROJECT_STATE` estavam coerentes com o codigo para avancar.
- Nao foi necessario alinhamento previo de Bloco 5.x.

## Decisoes travadas
1. **Fonte principal do bloco**: `Remotive` (API publica mais previsivel, sem scraping fragil como base).
2. **Contrato interno unico**:
   - `ExternalVacancyRecord` (adaptador de fonte)
   - `NormalizedVacancyRecord` (contrato interno de pipeline)
3. **Dedupe deterministico**:
   - prioridade: `platform + external_id`
   - fallback: `platform + source_checksum` (SHA-256 de campos normalizados)
4. **Controle concorrente explicito**:
   - lock por fonte em `vacancy_ingestion_locks`
   - resultado `SKIPPED_LOCKED` em tentativa paralela
5. **Controle operacional**:
   - endpoint manual admin (`POST /api/v1/vacancies/ingestion/runs`)
   - scheduler por configuracao (habilitado/desabilitado por ambiente)

## O que foi implementado

### Integracao
- `RemotiveVacancySourceConnector` com:
  - HTTPS obrigatorio
  - allowlist de host
  - limite de payload
  - timeout de conexao/leitura
  - parse controlado e erro explicito
- `RemotivePayloadMapper` para mapear payload bruto em contrato externo.

### Aplicacao / Pipeline
- `VacancyIngestionService` implementado com estagios:
  - fetch
  - normalizacao
  - deduplicacao/persistencia
  - contabilizacao de falhas por estagio
  - escrita de run result
- `VacancyIngestionNormalizer` com sanitizacao e checksum.

### Persistencia
- Migração `V3__vacancy_ingestion_foundation.sql`:
  - novos campos em `vacancies` para rastreabilidade de origem e ingestao
  - tabelas `vacancy_ingestion_runs` e `vacancy_ingestion_locks`
  - indice de checksum por plataforma
  - seed de plataforma Remotive
- `VacancyIngestionPersistenceRepository` para upsert com dedupe.

### Observabilidade
- Metricas dedicadas de ingestao com baixa cardinalidade.
- Logs estruturados por estagio (`source`, `trigger`, `runId`, `outcome`, contadores).

### Seguranca
- Endpoint de ingestao protegido com `@PreAuthorize("hasRole('ADMIN')")`.
- Sem URL arbitraria de fonte exposta a usuario.
- Sem fallback silencioso em falhas de ingestao.

## OWASP / API Security (impacto e mitigacao)
- **A01 Broken Access Control**: trigger manual de ingestao restrito a ADMIN.
- **A03 Injection**: sanitizacao de campos externos antes de persistir/propagar.
- **A05/API8 Security Misconfiguration**: validacao de perfil estrito (staging/prod) com requisitos de HTTPS/allowlist/payload bounds.
- **A09 Logging and Monitoring Failures**: logs estruturados + metricas por etapa de pipeline.
- **A10 SSRF / API10 Unsafe Consumption**: host allowlist, HTTPS, sem URL controlada por usuario, timeout e limite de payload.
- **API4 Unrestricted Resource Consumption**: limite de jobs por run + payload maximo + controle de concorrencia por lock.

## Testes executados
- Comando: `.\mvnw.cmd -B test`
- Resultado: `BUILD SUCCESS`
- Evidencia resumida:
  - `Tests run: 43, Failures: 0, Errors: 0, Skipped: 2`
- Cobertura adicionada no bloco:
  - `VacancyIngestionNormalizerTest`
  - `VacancyIngestionServiceTest`
  - `VacancyIngestionPersistenceRepositoryTest`
  - `SecurityAuthorizationIntegrationTest` (bloqueio de trigger de ingestao para USER)
  - `VacancyControllerValidationTest` (validacao de payload de trigger)

## Limitacoes reais
1. O bloco cobre **1 fonte real** (Remotive); segunda fonte ficou para proximo ciclo.
2. Nao houve carga operacional de ingestao em staging neste bloco (foi validacao por testes automatizados).
3. A tabela de lock nao possui timeout/lease; em crash extremo, limpeza pode exigir procedimento operacional.

## Proximo passo recomendado
1. Executar bloco incremental de **validacao operacional da ingestao em staging** (runs reais e verificacao de metricas/alertas de ingestao).
2. Adicionar segunda fonte real com o mesmo contrato para validar extensibilidade do desenho.
