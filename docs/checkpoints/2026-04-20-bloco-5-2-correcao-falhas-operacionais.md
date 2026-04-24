# Checkpoint - 2026-04-20 - Bloco 5.2 Correcao de Falhas Operacionais

## Status
`parcialmente validado com evidencia operacional real`

## Diagnostico de falhas (entrada do bloco)
1. **CRITICO** - 5xx sob carga em staging:
   - Evidencia: `StagingOperationalLoadTest` falhou com `expected 0 but was 80`.
   - Causa raiz: persistencia de auditoria gravando `String` em colunas `jsonb` (`audit_logs.after_state`), disparando erro SQL.
   - Impacto: indisponibilidade em caminho de seguranca (rate limit unavailable), risco de cascata operacional.

2. **MEDIO** - baixa visibilidade de falha de auditoria:
   - Nao havia metrica dedicada para erro de persistencia de trilha de auditoria.
   - Impacto: falha podia ocorrer sem sinal claro para threshold/alerta.

## Correcoes aplicadas
1. Mapeamento JPA de `before_state/after_state` atualizado para JSON real:
   - `AuditLogJpaEntity` agora usa `JsonNode` com `@JdbcTypeCode(SqlTypes.JSON)` e `columnDefinition = "jsonb"`.
2. `AuditEventService` endurecido:
   - Conversao segura `String -> JsonNode` (parse JSON quando aplicavel; fallback para JSON string).
   - Persistencia de auditoria encapsulada com tratamento explicito de excecao.
   - Falha de auditoria nao derruba fluxo HTTP de seguranca.
   - Falha agora gera:
     - evento operacional `audit_log_persist_failed`;
     - metrica `applyflow_audit_log_persist_failures_total`.
3. Testes adicionados/atualizados:
   - `AuditEventServiceTest` cobre persistencia JSON e nao propagacao da falha.
   - `MicrometerOperationalMetricsServiceTest` inclui nova metrica de falha de auditoria.

## Revalidacao executada
1. **Testes automatizados**
   - `.\mvnw.cmd -B test`
   - Resultado: 36 testes, 0 falhas, 0 erros, 2 skipped (load de staging sem flag).

2. **Carga real multi-instancia**
   - `powershell -ExecutionPolicy Bypass -File ops\loadtest\run-staging-load.ps1 -TotalRequests 180 -Concurrency 30 -KeepEnvironment`
   - Resultado:
     - `STAGING_LOAD_SUMMARY total=180 status={403=8, 429=172} p95Ms=515 p99Ms=879 redisMode=180 blocked429=172`
     - `STAGING_AUTHZ_SUMMARY total=120 status={401=120}`
   - Sem 5xx no baseline apos correcao.

3. **Falha controlada Redis DOWN**
   - Resultado observado: `REDIS_DOWN_STATUS=503` e `REDIS_DOWN_RATE_LIMIT_MODE=unavailable`.
   - Comportamento fail-explicit preservado, sem fallback silencioso em staging.

4. **Prometheus + Alertmanager**
   - `targets` up para `backend-1` e `backend-2`.
   - Alerta `ApplyFlowRateLimitUnavailable` em estado `firing` no Prometheus e `active` no Alertmanager (receiver `staging-webhook`).

## Validado vs parcial vs pendente
- **Corrigido e validado**
  - falha critica de 5xx causada por auditoria/jsonb;
  - consistencia de rate limit distribuido em 2 instancias (`mode=redis`);
  - Redis DOWN com resposta explicita (`503/unavailable`);
  - fluxo de alerta Redis unavailable ate Alertmanager.

- **Parcialmente validado**
  - disparo de alertas de 429 e 5xx: regras existem e foram exercitadas parcialmente, mas thresholds com janela longa (`for: 10m` e `for: 5m`) nao foram totalmente comprovados como `firing` neste ciclo.

- **Nao validado neste ciclo**
  - evidencia de firing de `ApplyFlowHighRateLimitedRequests` e `ApplyFlowHigh5xxRate` com janela completa de `for`.

## Risco residual
1. Thresholds de 429/5xx exigem campanha de carga/falha mais longa para prova operacional completa de firing.
2. Teste de indisponibilidade Redis elevou p99 e gerou alerta de latencia (esperado em falha), requer calibracao adicional para separar degradacao planejada de incidente real em janelas de chaos test.
