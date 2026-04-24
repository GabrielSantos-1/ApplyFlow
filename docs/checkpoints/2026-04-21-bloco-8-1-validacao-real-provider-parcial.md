# Checkpoint - 2026-04-21 - Bloco 8.1 Validacao Real de Provider IA (Parcial)

## Status
`parcial - execucao interrompida por solicitacao do usuario apos evidencias iniciais`

## 1) Escopo executado ate a pausa

### Gate tecnico
- Leitura e confronto de:
  - `docs/checkpoints/2026-04-21-bloco-8-ia-controlada.md`
  - `context/PROJECT_STATE.md`
  - codigo real em `ai` + `matching`.
- Divergencia identificada e corrigida antes de continuar:
  - compose de staging nao propagava `AI_*` para runtime das duas instancias.

### Ajustes operacionais aplicados
- `apps/backend/infra/staging/docker-compose.yml` atualizado para incluir, em `backend-1` e `backend-2`:
  - `AI_ENABLED`
  - `AI_PROVIDER_NAME`
  - `AI_PROVIDER_BASE_URL`
  - `AI_PROVIDER_CHAT_PATH`
  - `AI_PROVIDER_MODEL`
  - `AI_PROVIDER_API_KEY`
  - `AI_PROVIDER_CONNECT_TIMEOUT_MS`
  - `AI_PROVIDER_READ_TIMEOUT_MS`
  - `AI_PROVIDER_MAX_RETRIES`
  - `AI_PROVIDER_MAX_COMPLETION_TOKENS`
  - `AI_PROVIDER_TEMPERATURE`
  - `AI_PROVIDER_ALLOWED_HOSTS`
  - `RATE_LIMIT_AI_ENRICHMENT_LIMIT`

### Correcao critica encontrada durante validacao real
- Erro sob carga real de IA:
  - persistencia de `matching.score_breakdown` quebrava transacao (`jsonb` vs `varchar`), gerando `500`.
- Correcao aplicada:
  - `MatchResultJpaEntity.scoreBreakdown` migrado para `JsonNode` com `@JdbcTypeCode(SqlTypes.JSON)`.
  - `MatchingUseCaseService` ajustado para persistir breakdown com `objectMapper.valueToTree(...)`.

## 2) Evidencia real coletada

### Ambiente
- Staging em execucao com:
  - `backend-1`, `backend-2`, `postgres`, `redis`, `prometheus`, `alertmanager`.
- `AI_*` confirmadas em runtime no container.

### Chamadas reais aos 3 fluxos IA (2 instancias)
- Fluxos executados:
  - `match-enrichment`
  - `cv-improvement`
  - `application-draft`
- Resultado observado (campanha com API key invalida de teste):
  - `HTTP 200` com `fallbackUsed=true` em todos os fluxos/instancias.
  - latencias observadas na amostra: ~`515ms` a `967ms`.

### Rate limit distribuido (uso real)
- Burst de `25` chamadas alternando `backend-1`/`backend-2`:
  - `20` respostas `200`
  - `5` respostas `429`
- Evidencia de consistencia multi-instancia para rota IA.

### Observabilidade comprovada no Prometheus endpoint protegido
- Coleta em `/actuator/prometheus` com `X-Actuator-Token`.
- Series observadas:
  - `applyflow_ai_calls_started_total`
  - `applyflow_ai_calls_completed_total{outcome="provider_failed"}`
  - `applyflow_ai_call_duration_seconds_*`
  - `applyflow_ai_fallback_total{reason="provider_failed"}`

## 3) Validacoes concluídas vs pendentes

### Concluidas com evidencia
1. Provider integrado e acionado em runtime real.
2. Fluxo principal nao quebra sob falha de provider (fallback seguro).
3. Observabilidade da camada IA ativa e coerente com comportamento real.
4. Rate limit dedicado de IA validado em 2 instancias.

### Pendentes nesta execucao
1. Sucesso real do provider com resposta sem fallback (faltou key valida de homologacao nesta campanha).
2. Medicao real de uso de tokens/custo por fluxo (sem usage confiavel por chamadas falhadas).
3. Cenarios adicionais de falha operacional:
   - timeout real controlado,
   - indisponibilidade total do provider,
   - erro 4xx/5xx especifico de provider,
   - resposta fora de schema em ambiente real/mock.

## 4) Testes automatizados
- Comando executado:
  - `.\mvnw.cmd -B test`
- Resultado:
  - `Tests run: 59, Failures: 0, Errors: 0, Skipped: 2`
  - `BUILD SUCCESS`

## 5) Ponto exato de parada
- Tentativa de iniciar novo cenario de falha (reconfigurar compose para timeout forçado) exigiu elevacao de permissao Docker.
- A elevacao foi negada e, em seguida, o usuario solicitou interrupcao e consolidacao de estado.
- Execucao encerrada sem novas mudancas funcionais apos a solicitacao.

## 6) Riscos remanescentes para escalar sem fechar 8.1
1. Sem baseline de custo/token por fluxo, ha risco financeiro e de capacidade.
2. Sem prova de sucesso real do provider, ha risco de comportamento degradado permanente (fallback continuo) em operacao.
3. Sem campanha completa de falhas, pode haver lacunas de resiliencia nao exercitadas.

## 7) Proximo passo recomendado
1. Retomar 8.1 com API key valida de homologacao.
2. Executar matriz de validacao por fluxo:
   - sucesso, latencia, tokens, custo.
3. Executar matriz de falhas:
   - key invalida, timeout, indisponibilidade, 4xx/5xx, schema invalido.
4. Consolidar resultados em checkpoint final 8.1 (`validado` ou `parcial`) com numeros objetivos.
