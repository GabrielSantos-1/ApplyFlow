# Metrics Catalog - Bloco 4

## Escopo
Implementacao de observabilidade operacional com Micrometer + Actuator, mantendo separacao de camadas.

## Endpoints operacionais
- `GET /actuator/health` (publico)
- `GET /actuator/info` (publico)
- `GET /actuator/metrics` (ADMIN)
- `GET /actuator/prometheus` (ADMIN, quando endpoint estiver habilitado no ambiente)

## Metricas implementadas

### Metricas automaticas (Actuator/Micrometer)
- `http_server_requests_seconds_count` (total de requests)
- `http_server_requests_seconds_bucket` (latencia/histograma)
- `http_server_requests_seconds_sum` (tempo total)
- Taxa de erro 4xx/5xx via tag `status`

### Metricas customizadas ApplyFlow
- `applyflow_auth_login_total{outcome}`
  - `success`, `failed`
- `applyflow_auth_refresh_total{outcome}`
  - `success`, `missing`, `not-found-or-replay`, `revoked`, `expired`
- `applyflow_rate_limit_total{policy,outcome,mode}`
  - `outcome`: `allowed`, `blocked`, `unavailable`
  - `mode`: `redis`, `in-memory-fallback`, `unavailable`
- `applyflow_rate_limit_fallback_total{policy}`
  - contabiliza ativacoes de fallback
- `applyflow_authorization_total{outcome,endpoint}`
  - `outcome`: `unauthorized`, `forbidden`
  - `endpoint`: endpoint logico (cardinalidade controlada)
- `applyflow_application_status_transition_total{from,to,outcome}`
  - transicoes validas e invalidas
- `applyflow_observability_emit_failures_total{signal}`
  - falhas de emissao de metrica

## Convencao de labels/tags
- Permitido:
  - `policy`, `outcome`, `mode`, `endpoint`, `from`, `to`
- Proibido:
  - `userId` bruto
  - `correlationId`
  - payload de request

## Privacidade e seguranca
- Nenhuma metrica inclui PII.
- Logs estruturados evitam token, senha e payload sensivel.

## Referencias de implementacao
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/observability/MicrometerOperationalMetricsService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/observability/Slf4jOperationalEventLogger.java`
- `apps/backend/src/main/resources/application.yml`
