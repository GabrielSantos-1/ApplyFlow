# Failure Modes - Bloco 4

## Estrategia
Falhar de forma explicita e monitoravel, sem comportamento silencioso.

## Modos suportados

### 1) Redis offline (fallback desabilitado)
- Config:
  - `security.rate-limit.redis-enabled=true`
  - `security.rate-limit.fallback-enabled=false`
- Comportamento esperado:
  - `503 RATE_LIMIT_UNAVAILABLE`
  - `X-RateLimit-Mode: unavailable`
  - `correlationId` presente
  - evento de auditoria e metrica `outcome=unavailable`

### 2) Redis offline (fallback habilitado)
- Config:
  - `security.rate-limit.redis-enabled=true`
  - `security.rate-limit.fallback-enabled=true`
- Comportamento esperado:
  - endpoint continua funcional
  - modo `in-memory-fallback` em header
  - metrica de fallback incrementada
  - alerta operacional de fallback ativo

### 3) Latencia simulada no backend de rate limit
- Config de simulacao (somente por flag):
  - `security.rate-limit.simulation-enabled=true`
  - `security.rate-limit.simulation-mode=latency`
  - `security.rate-limit.simulated-latency-ms=<valor>`
- Comportamento esperado:
  - aumento de latencia observavel
  - sem quebra funcional por si so

### 4) Indisponibilidade simulada no backend de rate limit
- Config de simulacao:
  - `security.rate-limit.simulation-enabled=true`
  - `security.rate-limit.simulation-mode=unavailable`
- Comportamento esperado:
  - aplica mesma estrategia dos modos 1 ou 2 conforme fallback

### 5) Falha na emissao de metrica
- Comportamento esperado:
  - nao derrubar request path
  - log `observability.metric_emission_failed`
  - tentativa de contabilizar `applyflow_observability_emit_failures_total`

## Controle de seguranca da simulacao
- Simulacao desabilitada por padrao (`false`).
- Nao ha endpoint HTTP para ativar simulacao.
- Ativacao somente por configuracao de ambiente.

## Evidencias em testes
- `RateLimitRedisUnavailableIntegrationTest`
- `RateLimitRedisFallbackEnabledIntegrationTest`
- `RateLimitConcurrencyIntegrationTest`
- `MicrometerOperationalMetricsServiceTest`
