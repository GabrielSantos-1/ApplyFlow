# Alerts and Thresholds - Bloco 4

## Objetivo
Definir sinais operacionais explicitos para detectar degradacao, abuso e indisponibilidade.

## Thresholds iniciais sugeridos
- 401 anormal:
  - `sum(rate(applyflow_authorization_total{outcome="unauthorized"}[5m])) > 5` por 10m
- 403 anormal:
  - `sum(rate(applyflow_authorization_total{outcome="forbidden"}[5m])) > 3` por 10m
- 429 anormal:
  - `sum(rate(applyflow_rate_limit_total{outcome="blocked"}[5m])) > 3` por 5m
- Backend de rate limit indisponivel:
  - `sum(rate(applyflow_rate_limit_total{outcome="unavailable"}[2m])) > 0` por 2m
- Fallback de rate limit ativo:
  - `sum(rate(applyflow_rate_limit_fallback_total[5m])) > 0` por 5m
- Erros 5xx:
  - `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 1` por 5m
- Latencia p95:
  - `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 0.8` por 10m

## Arquivo base de alertas
- `apps/backend/ops/prometheus/alert-rules.yml`

## Consumo operacional
- Prometheus coleta `/actuator/prometheus`.
- Alertmanager (ou stack equivalente) consome regras e notifica canal operacional.
- Quando `fallback` ativa repetidamente, escalar como incidente de disponibilidade de Redis.

## Anti-noise
- Thresholds foram definidos para reduzir falso positivo inicial.
- Ajuste obrigatorio apos baseline real de trafego.
