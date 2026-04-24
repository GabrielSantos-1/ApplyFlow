# Bloco 5 - Operação Real Controlada (Staging)

## Topologia versionada
- `apps/backend/infra/staging/docker-compose.yml`:
  - `backend-1` e `backend-2` (Spring Boot profile `staging`)
  - `redis` (rate limit distribuído)
  - `postgres`
  - `prometheus`
  - `alertmanager`
- Prometheus scraping com token técnico em header:
  - `X-Actuator-Token`
  - configurado em `infra/staging/prometheus/prometheus.yml`

## Segurança operacional aplicada
- Profile `staging`:
  - `security.rate-limit.redis-enabled=true`
  - `security.rate-limit.fallback-enabled=false`
  - `security.actuator.metrics-token` obrigatório
- Validação fail-fast em boot:
  - `OperationalSecurityConfigurationValidator`
  - falha startup em staging/prod se houver misconfiguration crítica.
- Endpoints sensíveis de actuator continuam restritos a `ROLE_ADMIN`.
- Token técnico de scraping injeta autenticação apenas para `/actuator/metrics*` e `/actuator/prometheus`.

## Carga controlada
- Teste de carga orientado a staging:
  - `src/test/.../StagingOperationalLoadTest.java`
  - habilitado apenas com `-Dstaging.load.enabled=true`
  - mede status, modo de rate limit, p95 e p99.
- Script operacional:
  - `apps/backend/ops/loadtest/run-staging-load.ps1`
  - sobe stack, executa teste de carga, valida comportamento com Redis indisponível e derruba stack.

## Evidência de bloqueio no ambiente local desta execução
- Tentativa real de subir stack falhou por indisponibilidade do engine Docker:
  - pipe `//./pipe/dockerDesktopLinuxEngine` não encontrada.
- Resultado:
  - validação multi-instância real e carga real **preparadas**, mas **não concluídas** neste ambiente.

## Comandos de validação esperados
```powershell
powershell -ExecutionPolicy Bypass -File apps/backend/ops/loadtest/run-staging-load.ps1 -TotalRequests 180 -Concurrency 30
```

## Critério de aceite operacional do bloco
- `StagingOperationalLoadTest` executado com `staging.load.enabled=true`.
- Evidência de 429 sob carga e `X-RateLimit-Mode=redis`.
- Evidência de ausência de fallback silencioso em staging.
- Alertmanager recebendo alertas de Prometheus.

## Evidencia adicional do Bloco 5.2 (2026-04-20)
- Carga de staging reexecutada com sucesso:
  - `STAGING_LOAD_SUMMARY total=180 status={403=8, 429=172} p95Ms=515 p99Ms=879 redisMode=180 blocked429=172`
  - `STAGING_AUTHZ_SUMMARY total=120 status={401=120}`
- Redis DOWN validado com resposta explicita:
  - `REDIS_DOWN_STATUS=503`
  - `REDIS_DOWN_RATE_LIMIT_MODE=unavailable`
- Prometheus scraping validado para `backend-1` e `backend-2` (`health=up`).
- Alertmanager recebeu alerta ativo de indisponibilidade:
  - `ApplyFlowRateLimitUnavailable` (`severity=critical`, receiver `staging-webhook`).

## Evidencia adicional do Bloco 5.3 (2026-04-20)
- Campanha de 429 por 12 minutos:
  - `GLOBAL_STATUS: 403=96, 429=50802`
  - `ApplyFlowHighRateLimitedRequests` entrou em `firing` e depois resolveu automaticamente.
- Campanha de 5xx controlado (503) com Redis DOWN + carga paralela:
  - `STATUS_503=1524`
  - `ApplyFlowHigh5xxRate` entrou em `firing` e resolveu apos recuperacao.
- Redis DOWN validado em ciclo completo:
  - `ApplyFlowRateLimitUnavailable` em `firing` no Prometheus e `active` no Alertmanager, com resolucao observada apos `redis start`.
- Calibracao de ruido de latencia aplicada e validada: p95/p99 nao dispararam no teste curto pos-ajuste durante Redis DOWN.
