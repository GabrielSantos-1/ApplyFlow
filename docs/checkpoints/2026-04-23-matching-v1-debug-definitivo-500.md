# Checkpoint - Debug Definitivo do 500 em Matching (Stacktrace + Correcao Raiz)

**Data:** 2026-04-23  
**Incidente:** `GET /api/v1/matches/vacancy/{vacancyId}` retornando `500` em runtime real.

## 1) Reproducao objetiva do erro

Com runtime antigo ativo, foi reproduzido:

1. `GET /api/v1/matches/vacancy/{vacancyId}` -> `500 INTERNAL_ERROR`
2. `GET /api/v1/matches/vacancy/{vacancyId}/summary` -> `500 INTERNAL_ERROR`
3. `GET /api/v1/matches/{vacancyId}` -> `404 NOT_FOUND` (`Vaga nao encontrada`)

Isso caracteriza falha de roteamento/validacao de path no runtime em execucao, nao falha de rede.

## 2) Evidencia de runtime (stacktrace e estado do deploy)

### Stacktrace coletado no container

Foi coletado stacktrace real no backend em runtime:

- `HttpMessageNotWritableException: No converter for ApiErrorResponse with Content-Type application/openmetrics-text`
- origem em `GlobalExceptionHandler#handleDefault` no contexto de actuator/openmetrics.

Observacao:
- para o 500 de matching, o runtime retornava `INTERNAL_ERROR` via handler generico sem log de stacktrace do caso especifico de conversao de path.

### Estado do deploy no momento do incidente

Antes do rebuild, logs de startup mostravam:
- `Successfully validated 5 migrations`

O repositorio ja estava em V1 com:
- migration `V6__matching_v1_deterministic_pipeline.sql`
- endpoints stateful de matching

Conclusao: o runtime em execucao estava desatualizado em relacao ao codigo.

## 3) Causa raiz

Causa raiz composta:

1. **Runtime desatualizado** (imagem sem V6 / sem superficie completa de endpoints stateful).
2. **Tratamento insuficiente de erro de conversao de parametro de rota**:
   - conversao invalida de path/UUID caia no `Exception` generico e virava `500`,
   - em vez de resposta de validacao (`4xx`).

## 4) Correcao aplicada

1. Hardening definitivo no `GlobalExceptionHandler`:
   - `MethodArgumentTypeMismatchException` -> `422 VALIDATION_ERROR`
   - `IllegalArgumentException` -> `422 VALIDATION_ERROR`

2. Regressao automatizada:
   - teste de integracao `invalidUuidOnMatchPathMustNotReturn500`
   - garante que `/api/v1/matches/not-a-uuid` nao retorna `500`.

3. Rebuild/redeploy do staging local:
   - `docker compose -f apps/backend/infra/staging/docker-compose.yml up -d --build`
   - logs apos redeploy: `Successfully validated 6 migrations`

## 5) Validacao pos-correcao (runtime real)

Apos deploy da versao corrigida:

1. `GET /api/v1/matches/vacancy/{vacancyId}` -> `404 NOT_FOUND` (sem 500)
2. `GET /api/v1/matches/{vacancyId}` -> `404 NOT_FOUND` (sem 500)
3. `GET /api/v1/matches/vacancy/{vacancyId}/summary` -> `404 NOT_FOUND` (sem 500)
4. `GET /api/v1/matches/not-a-uuid` -> `422 VALIDATION_ERROR` (confirmado em runtime)

Resultado: 500 eliminado para os cenarios de path/conversao; endpoint volta a comportamento fail-safe.

## 6) Comandos/evidencias executados

1. Captura de logs runtime:
   - `docker compose -f apps/backend/infra/staging/docker-compose.yml logs backend-1 --tail ...`
   - `docker compose -f apps/backend/infra/staging/docker-compose.yml logs backend-2 --tail ...`
2. Reproducao HTTP:
   - login em `POST /api/v1/auth/login`
   - chamadas `GET /api/v1/matches/*`
3. Validacao de build/test:
   - `.\mvnw.cmd -B test` -> `BUILD SUCCESS`

