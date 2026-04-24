# Rate Limiting - Blocos 3 e 4

## Objetivo
Garantir controle de consumo previsivel em ambiente distribuido e mitigar abuso em endpoints criticos.

## Arquitetura
- Contrato estavel: `RateLimitService`.
- Implementacao principal: `RedisRateLimitService` (script atomico INCR+EXPIRE).
- Orquestracao: `CompositeRateLimitService`.
- Fallback explicito: `InMemoryRateLimitService` quando habilitado.
- Aplicacao em borda HTTP: `RateLimitFilter`.

## Chave de limitacao
Formato base:
- `policy` + `ip` + `userId` (quando autenticado)
- Exemplo logico: `auth-login | ip:203.0.113.10 | uid:anonymous`

## Politicas ativas
- `auth-login` (agressiva)
- `auth-refresh` (agressiva)
- `resume-variant-create` (restritiva)
- `application-draft-create` (restritiva)
- `vacancies-read` (moderada)

## Resposta
Quando politica ativa:
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `X-RateLimit-Policy`, `X-RateLimit-Mode`.
- Bloqueio: `429 RATE_LIMIT_EXCEEDED` + `Retry-After` + `correlationId`.
- Falha de backend de limite sem fallback: `503 RATE_LIMIT_UNAVAILABLE`.

## Observabilidade e operacao (Bloco 4)
- Metricas customizadas:
  - `applyflow_rate_limit_total{policy,outcome,mode}`
  - `applyflow_rate_limit_fallback_total{policy}`
- Logs estruturados com `eventType`, `severity`, `endpoint`, `outcome`, `correlationId`.
- Evento de auditoria em bloqueio e indisponibilidade.
- Simulacao controlada por flag:
  - `RATE_LIMIT_SIMULATION_ENABLED`
  - `RATE_LIMIT_SIMULATION_MODE` (`none|latency|unavailable`)
  - `RATE_LIMIT_SIMULATED_LATENCY_MS`

## Risco residual
Com fallback habilitado e Redis indisponivel, o comportamento deixa de ser totalmente distribuido.
