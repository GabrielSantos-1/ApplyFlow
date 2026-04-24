# Checkpoint - 2026-04-20 - Bloco 5.3 Validacao Prolongada de Alerting

## Status
`validado com evidencia real de firing e resolucao`

## Mapa de alertas (estado inicial)
1. `ApplyFlowHighUnauthorizedRate`
   - metrica: `applyflow_authorization_total{outcome="unauthorized"}`
   - condicao: `rate[5m] > 8`
   - janela: `for 10m`
   - severidade: `warning`
2. `ApplyFlowHighForbiddenRate`
   - metrica: `applyflow_authorization_total{outcome="forbidden"}`
   - condicao: `rate[5m] > 4`
   - janela: `for 10m`
   - severidade: `warning`
3. `ApplyFlowHighRateLimitedRequests`
   - metrica: `applyflow_rate_limit_total{outcome="blocked"}`
   - condicao: `rate[5m] > 6` por `application/policy`
   - janela: `for 10m`
   - severidade: `warning`
4. `ApplyFlowRateLimitUnavailable`
   - metrica: `applyflow_rate_limit_total{outcome="unavailable"}`
   - condicao: `rate[2m] > 0`
   - janela: `for 2m`
   - severidade: `critical`
5. `ApplyFlowRateLimitFallbackActive`
   - metrica: `applyflow_rate_limit_fallback_total`
   - condicao: `rate[5m] > 0`
   - janela: `for 5m`
   - severidade: `warning`
6. `ApplyFlowHigh5xxRate`
   - metrica: `http_server_requests_seconds_count{status=~"5..", uri!~"/actuator.*"}`
   - condicao: `rate[5m] > 1`
   - janela: `for 5m`
   - severidade: `critical`
7. `ApplyFlowHighLatencyP95`
   - metrica: `http_server_requests_seconds_bucket`
   - condicao inicial: `p95 > 0.9s`
   - janela: `for 10m`
   - severidade: `warning`
8. `ApplyFlowHighLatencyP99`
   - metrica: `http_server_requests_seconds_bucket`
   - condicao inicial: `p99 > 1.5s`
   - janela: `for 10m`
   - severidade: `critical`

## Cenarios executados
1. **Burst prolongado de 429 (12 minutos)**
   - carga continua em `POST /api/v1/auth/login` nas 2 instancias.
   - evidencia de carga:
     - `GLOBAL_STATUS: 403=96, 429=50802`.
2. **Erro 5xx controlado (Redis indisponivel + carga paralela)**
   - Redis down com workers paralelos por 6 minutos.
   - evidencia:
     - `STATUS_503=1524`.
3. **Redis DOWN (validacao especifica)**
   - Redis down por campanha dedicada e pos-calibracao.
   - evidencia de alerta especifico:
     - `ApplyFlowRateLimitUnavailable` em `pending -> firing`.

## Evidencia de firing
1. `ApplyFlowHighRateLimitedRequests`
   - Prometheus: `state=firing`, `activeAt=2026-04-20T23:58:25Z`.
   - Alertmanager: alerta `active` no receiver `staging-webhook`.
2. `ApplyFlowRateLimitUnavailable`
   - Prometheus: `pending` observado e depois `firing`.
   - Alertmanager: `active` com receiver `staging-webhook`.
3. `ApplyFlowHigh5xxRate`
   - Prometheus: `pending` durante carga 503, depois `firing` (`activeAt=2026-04-21T00:39:10Z`).
   - Alertmanager: `active` com receiver `staging-webhook`.

## Evidencia de resolucao
1. Alerta 429 (`ApplyFlowHighRateLimitedRequests`)
   - fim da carga: resolucao observada em ~`4m31s` (polling de estado `firing -> resolved`).
2. Alertas criticos Redis/5xx
   - apos `redis start`: ambos resolvidos em ~`240s`.
   - detalhado no polling:
     - `ApplyFlowRateLimitUnavailable` resolveu antes do 5xx.
     - `ApplyFlowHigh5xxRate` resolveu quando janela de erro caiu.
3. Redis unavailable pos-calibracao
   - resolvido em ~`105s` apos recuperacao.

## Ajustes de calibracao realizados
Arquivo: `apps/backend/ops/prometheus/alert-rules.yml`

1. `ApplyFlowHighLatencyP95`:
   - passou a considerar apenas `status!~"5.."`.
   - adicionada condicao de volume minimo:
     - `sum(rate(http_server_requests_seconds_count{...}[5m])) by (application) > 1`.
2. `ApplyFlowHighLatencyP99`:
   - mesma estrategia (`status!~"5.."` + gate de volume > 1 rps).

Racional:
- reduzir ruido de latencia durante incidentes ja cobertos por alertas criticos (5xx/unavailable);
- evitar firing de p95/p99 com amostra escassa.

## Validacao pos-calibracao
Teste curto com Redis DOWN (~3 min):
- observado apenas `ApplyFlowRateLimitUnavailable` (`pending -> firing`);
- **sem** firing/pending de `ApplyFlowHighLatencyP95/P99` no periodo observado.

## Seguranca operacional confirmada
- actuator continua protegido:
  - `/actuator/prometheus` sem token => `401`;
  - com token tecnico => `200`.
- rate limit permaneceu ativo durante toda campanha.
- nao houve desabilitacao de controles para facilitar teste.

## Riscos remanescentes
1. Alertas de 401/403/fallback nao foram forcados em campanha prolongada dedicada neste ciclo (permanece validacao indireta).
2. Thresholds ainda sao baseline de staging; exigem revisao com trafego real continuo de ambiente superior.
