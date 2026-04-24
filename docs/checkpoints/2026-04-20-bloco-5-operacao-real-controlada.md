# Checkpoint - 2026-04-20 - Bloco 5 Operação Real Controlada

## Status
`implementado com bloqueio externo de validação real`

## Implementado
1. Segregação de ambiente:
   - `application-dev.yml`, `application-staging.yml`, `application-prod.yml`.
   - defaults de staging/prod com `fallback=false` e headers de produção.
2. Hardening de configuração:
   - `OperationalSecurityConfigurationValidator` (fail-fast em staging/prod).
3. Scraping operacional seguro:
   - `ActuatorTokenAuthenticationFilter`.
   - token técnico para `/actuator/prometheus` e `/actuator/metrics`.
4. Alerting operacional:
   - regras Prometheus calibradas com p95/p99 e anti-noise.
   - arquivos de Prometheus/Alertmanager versionados.
5. Staging multi-instância:
   - compose com 2 instâncias backend + Redis + Postgres + Prometheus + Alertmanager.
6. Validação de carga preparada:
   - `StagingOperationalLoadTest` + script `run-staging-load.ps1`.

## Alinhamento de gate prévio
- Divergência encontrada: teste de headers acoplado a `/actuator/health` falhava com Redis offline.
- Correção: teste migrou para `/actuator/info`, preservando semântica de health.

## Testes executados nesta execução
- `.\mvnw.cmd -B test`:
  - 34 testes
  - 0 falhas / 0 erros
  - 2 skipped (`StagingOperationalLoadTest`, intencional sem flag).

## Validação operacional real
- Tentativa de execução do script de staging realizada.
- Bloqueio externo: engine Docker local indisponível (`dockerDesktopLinuxEngine` pipe ausente).
- Consequência:
  - validação real multi-instância e carga **não concluída** no ambiente atual.

## Risco residual
1. Falta evidência empírica local de carga multi-instância com Redis real nesta máquina.
2. Integração Alertmanager preparada, mas não exercida fim-a-fim sem engine Docker ativo.
3. Token técnico do actuator exige rotação e armazenamento seguro no ambiente alvo.
