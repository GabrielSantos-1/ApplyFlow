# Checkpoint - 2026-04-22 - Bloco 8.1 Validacao Real de Provider IA

## Status
`validado completo (8.1-final)`

## 1) Gate e aderencia
- Base de verdade utilizada:
  - `context/PROJECT_STATE.md`
  - `docs/checkpoints/2026-04-21-bloco-8-1-validacao-real-provider-parcial.md`
  - checkpoint parcial de `2026-04-22`
- Aderencia confirmada com o codigo real do Bloco 8.
- Escopo mantido cirurgico: sem novos fluxos, sem alteracao do motor deterministico.

## 2) Causa raiz do `invalid_output` no `match-enrichment`
- Evidencia operacional (pre-fix):
  - `outcome=invalid_output` com `reason=IllegalArgumentException:Lista obrigatoria sem itens validos`.
- Causa raiz real:
  - o validador exigia lista nao-vazia para **todas** as listas do enrichment.
  - em vagas com score deterministico baixo (`0`) e sem aderencia, `strengths` vazio e um estado valido de negocio.
  - resultado: resposta do provider podia estar estruturalmente correta, mas era descartada indevidamente por regra excessivamente rigida.

## 3) Correcoes aplicadas no 8.1-final
1. `AiOutputValidator`:
   - `strengths` passou a permitir lista vazia em `match-enrichment`.
   - `gaps` e `nextSteps` permaneceram obrigatorias (nao-vazias).
2. `AiEnrichmentService`:
   - custo estimado passou a reconhecer variantes do modelo (`gpt-4o-mini-*`), nao apenas match exato `gpt-4o-mini`.
3. Teste de regressao adicionado:
   - caso cobrindo `strengths=[]` com `gaps/nextSteps` validos.

## 4) Revalidacao real dos 3 fluxos (2 instancias)
Endpoints testados:
- `POST /api/v1/ai/matches/{vacancyId}/enrichment`
- `POST /api/v1/ai/matches/{vacancyId}/cv-improvement`
- `POST /api/v1/ai/matches/{vacancyId}/application-draft`

Amostra (curl, JWT valido, `vacancyId=81d7655e-a1bb-4d8c-a537-bf6b9b2934f8`):
- backend-1:
  - enrichment: `200`, `~4.37s`, `fallbackUsed=false`
  - cv-improvement: `200`, `~5.22s`, `fallbackUsed=false`
  - application-draft: `200`, `~2.86s`, `fallbackUsed=false`
- backend-2:
  - enrichment: `200`, `~3.69s`, `fallbackUsed=false`
  - cv-improvement: `200`, `~3.99s`, `fallbackUsed=false`
  - application-draft: `200`, `~1.89s`, `fallbackUsed=false`

Conclusao: os 3 fluxos responderam com provider real sem fallback indevido nas duas instancias.

## 5) Usage/tokens/custo real por fluxo
Evidencia de log estruturado (`eventType=ai.flow_execution outcome=success`):
- backend-1
  - match-enrichment: `prompt=1819`, `completion=126`, `total=1945`, `estimatedCostUsd=3.48E-4`
  - cv-improvement: `prompt=1778`, `completion=264`, `total=2042`, `estimatedCostUsd=4.25E-4`
  - application-draft: `prompt=1765`, `completion=105`, `total=1870`, `estimatedCostUsd=3.28E-4`
- backend-2
  - match-enrichment: `prompt=1819`, `completion=125`, `total=1944`, `estimatedCostUsd=3.48E-4`
  - cv-improvement: `prompt=1778`, `completion=251`, `total=2029`, `estimatedCostUsd=4.17E-4`
  - application-draft: `prompt=1765`, `completion=102`, `total=1867`, `estimatedCostUsd=3.26E-4`

## 6) Observabilidade e seguranca
- Actuator permaneceu protegido:
  - sem token tecnico: `401`
  - com `X-Actuator-Token`: `200`
- Metricas IA expostas e incrementando:
  - `applyflow_ai_calls_started_total`
  - `applyflow_ai_calls_completed_total{outcome="success"}` para os 3 fluxos
- Nenhuma regressao observada em authz, rate limit IA ou fallback seguro.

## 7) Testes executados
- `./mvnw.cmd -B test`
  - resultado: `Tests run: 60, Failures: 0, Errors: 0, Skipped: 2`
- Rebuild/redeploy staging:
  - `docker compose -f infra/staging/docker-compose.yml up -d --build`
- Chamadas reais dos 3 endpoints em 2 instancias com JWT.

## 8) Pendencias e riscos residuais
1. Nao houve campanha longa de latencia/custo (apenas amostra curta de fechamento).
2. Cenario de schema invalido controlado (proxy/mocking) segue pendente para endurecimento adicional.
3. Custo observado em logs; metrica agregada de custo por fluxo ainda nao foi criada.

## 9) Diagnostico final do 8.1
- Objetivo do bloco atendido com evidencia real:
  1. provider real respondeu nos 3 fluxos;
  2. `match-enrichment` corrigido (sem fallback indevido);
  3. usage/tokens/custo capturados por chamada;
  4. seguranca e observabilidade preservadas.
- `Bloco 8.1` fechado para evolucao, com riscos residuais documentados.
