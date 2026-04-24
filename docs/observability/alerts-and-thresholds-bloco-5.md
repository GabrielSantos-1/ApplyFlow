# Alerts and Thresholds - Bloco 5

## Objetivo
Calibrar thresholds iniciais para staging com Redis real e reduzir ruído operacional.

## Ajustes aplicados
- 401 (`unauthorized`) ajustado para `> 8 req/sustentado` em 10m por `application`.
- 403 (`forbidden`) ajustado para `> 4 req/sustentado` em 10m por `application`.
- 429 ajustado para `> 6` em 10m por `policy` e `application`.
- 5xx passou a excluir `/actuator.*` para evitar falso positivo.
- Latência p95 ajustada para `> 900ms` (API) em 10m.
- Nova regra de latência p99 crítica em `> 1.5s` (API) em 10m.
- Regras de fallback/unavailable mantidas com prioridade alta.

## Racional técnico
- Baseline anterior estava sensível para ambientes com burst curto.
- O recorte por `policy` melhora triagem de abuso por endpoint crítico.
- Exclusão de actuator reduz ruído não funcional.
- p95 e p99 em conjunto permitem separar degradação progressiva de incidente agudo.

## Arquivo versionado
- `apps/backend/ops/prometheus/alert-rules.yml`

## Risco residual
- Threshold ainda é baseline inicial; precisa recalibração após tráfego real contínuo de staging.

## Validacao operacional 5.2 (2026-04-20)
- Confirmado em ambiente real: `ApplyFlowRateLimitUnavailable` em `firing` no Prometheus e `active` no Alertmanager.
- Confirmado receiver operacional: `staging-webhook`.
- Alertas `ApplyFlowHighRateLimitedRequests` (429) e `ApplyFlowHigh5xxRate` permanecem com validacao parcial neste ciclo por dependerem de janela `for` prolongada sob carga sustentada.

## Calibracao adicional 5.3 (2026-04-20)
- `ApplyFlowHighRateLimitedRequests` validado em ciclo completo (`firing -> resolved`) com campanha de 12 minutos.
- `ApplyFlowHigh5xxRate` validado em ciclo completo via erro controlado (503 sob indisponibilidade Redis) com `firing -> resolved`.
- `ApplyFlowRateLimitUnavailable` novamente validado em ciclo completo, incluindo resolucao apos recuperacao.

### Ajuste anti-ruido aplicado
- Alertas de latencia (`ApplyFlowHighLatencyP95` e `ApplyFlowHighLatencyP99`) foram ajustados para:
  - excluir `status 5xx`;
  - exigir volume minimo de requests nao-5xx (`> 1 req/s` em 5m).

### Racional
- Falhas de dependencia ja sao cobertas por alertas criticos dedicados (`5xx` e `unavailable`).
- p95/p99 sem filtro de status e sem gate de volume geravam ruido durante chaos test.
