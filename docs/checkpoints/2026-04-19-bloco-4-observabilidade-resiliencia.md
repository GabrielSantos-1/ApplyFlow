# Checkpoint Final - 2026-04-19 - Bloco 4 Observabilidade e Resiliencia

## 1) Estado geral
**Status do bloco:** `concluido com ressalvas`

Ressalvas:
1. `prometheus` esta configurado e protegido por role, mas depende da habilitacao operacional no ambiente final.
2. Ainda nao houve validacao com Redis real multi-instancia sob carga prolongada.
3. Integracao real de alerta (Alertmanager/SIEM) foi preparada por regras, mas nao operada aqui.

## 2) O que foi implementado

### Observabilidade
- Criada camada compartilhada de observabilidade:
  - `OperationalMetricsService` (contrato application)
  - `MicrometerOperationalMetricsService` (infra)
  - `OperationalEventLogger` + `Slf4jOperationalEventLogger`
  - `EndpointTagResolver` para endpoint logico e cardinalidade controlada
- Adicionada dependencia `micrometer-registry-prometheus`.
- Ajustado `application.yml` para:
  - expor `health,info,prometheus,metrics`
  - histograma de latencia `http.server.requests`
  - pattern de log com `correlationId`.

### Instrumentacao de eventos
- Auth:
  - metrica de login `success/failed`
  - metrica de refresh `success/missing/not-found-or-replay/revoked/expired`
- Rate limit:
  - metrica `allowed/blocked/unavailable`
  - metrica de fallback ativado
  - evento estruturado de fallback e bloqueio
- Authorization:
  - metrica de `unauthorized/forbidden` por endpoint logico
- Applications:
  - metrica de transicao valida/invalida

### Hardening operacional
- Security de actuator:
  - publico: `/actuator/health`, `/actuator/info`
  - ADMIN: `/actuator/metrics`, `/actuator/prometheus`
  - demais `/actuator/**`: deny all
- Simulacao de falha em rate limit por configuracao:
  - `simulation-enabled`
  - `simulation-mode` (`none|latency|unavailable`)
  - `simulated-latency-ms`

### Alertas
- Arquivo versionado de regras base:
  - `apps/backend/ops/prometheus/alert-rules.yml`
- Cobertura inicial:
  - aumento de 401/403/429
  - indisponibilidade rate limit
  - fallback ativo
  - aumento de 5xx
  - latencia p95

## 3) Arquivos alterados

### Codigo (observability/config/security)
- `apps/backend/pom.xml`
- `apps/backend/src/main/resources/application.yml`
- `apps/backend/src/test/resources/application-test.yml`
- `apps/backend/.env.example`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/application/observability/OperationalMetricsService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/application/observability/OperationalEventLogger.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/observability/MicrometerOperationalMetricsService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/observability/Slf4jOperationalEventLogger.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/observability/EndpointTagResolver.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/ratelimit/RateLimitFilter.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/ratelimit/CompositeRateLimitService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/ratelimit/RedisRateLimitService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/SecurityConfig.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/SecurityProperties.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/RestAuthenticationEntryPoint.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/RestAccessDeniedHandler.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/auth/application/service/StubAuthService.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/applications/application/service/StubApplicationService.java`

### Testes
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/RateLimitRedisUnavailableIntegrationTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/RateLimitRedisFallbackEnabledIntegrationTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/RateLimitConcurrencyIntegrationTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/SecurityAuthorizationIntegrationTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/shared/infrastructure/observability/MicrometerOperationalMetricsServiceTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/shared/infrastructure/ratelimit/CompositeRateLimitServiceTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/auth/application/service/AuthServiceTest.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/applications/application/service/ApplicationServiceTest.java`

### Operacao e documentacao
- `apps/backend/ops/prometheus/alert-rules.yml`
- `docs/observability/metrics-catalog-bloco-4.md`
- `docs/observability/alerts-and-thresholds-bloco-4.md`
- `docs/observability/failure-modes-bloco-4.md`
- `docs/security/rate-limiting-bloco-3.md`
- `docs/security/attack-surface-bloco-1.md`
- `docs/security/authorization-matrix-bloco-3.md`
- `context/PROJECT_STATE.md`
- `context/TASKS.md`
- `context/DECISIONS.md`

## 4) Validacao executada

### Backend
Comando:
```bash
./mvnw -B test
```
Resultado:
- 27 testes executados
- 0 falhas / 0 erros
- BUILD SUCCESS

### Frontend
Comando:
```bash
npm run build
```
Resultado:
- build concluido com sucesso (Next.js 16.2.4).

## 5) Riscos mitigados
1. Falha silenciosa de rate limit agora gera sinal explicito em metrica, log e (quando aplicavel) resposta.
2. Picos de 401/403/429 e 5xx agora possuem base objetiva para alerta.
3. Falha de emissao de metrica nao interrompe fluxo funcional.
4. Exposicao de endpoints operacionais sensiveis foi restringida para ADMIN.

## 6) Riscos remanescentes
1. Com fallback habilitado, rate limit deixa de ser distribuido sob indisponibilidade do Redis.
2. Regras de alerta ainda nao estao acopladas a pipeline de notificacao real.
3. Falta teste de resiliencia com Redis real em topologia multi-instancia.

## 7) Status dos criterios do bloco
- [x] metricas uteis implementadas
- [x] observabilidade integrada sem vazamento de PII
- [x] logging estruturado revisado
- [x] sinais/thresholds de alerta definidos
- [x] testes de resiliencia adicionados
- [x] simulacao de falha implementada com controle por config
- [x] actuator/configuracao operacional revisados
- [x] docs/observability criadas/atualizadas
- [x] context atualizado
- [x] checkpoint final gerado
- [x] build frontend executado nesta rodada

## 8) SYSTEM_CONTEXT_UPDATE
- Estado atual real:
  - Backend com observabilidade operacional ativa e cobertura de resiliencia expandida.
- Modulos funcionais:
  - `auth`, `vacancies`, `resumes`, `applications`, `matching`, `shared`.
- Decisoes travadas:
  - stack de metricas nativa (Micrometer/Actuator), tags de baixa cardinalidade, eventos estruturados, simulacao de falha por flag.
- Limitacoes atuais:
  - fallback local nao distribuido, ausencia de alerting em producao, falta de teste multi-instancia real.
- Proximos passos:
  - calibrar thresholds com trafego real, integrar Alertmanager/SIEM, validar degradacao com Redis real em staging.
