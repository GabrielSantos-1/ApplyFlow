# Checkpoint - 2026-04-21 - Bloco 8 IA Controlada + Sugestoes Inteligentes

## Status
`concluido com camada de IA controlada, validacao de schema, fallback seguro e observabilidade, sem invadir o matching deterministico`

## Gate e aderencia inicial
1. Checkpoint do Bloco 7 e `PROJECT_STATE` lidos antes da implementacao.
2. Verificacao de aderencia no codigo:
   - score e recommendation deterministicas permanecem em `matching`;
   - nenhuma regra critica foi migrada para IA.
3. Decisao de gate:
   - apto para Bloco 8.

## Escopo implementado

### 1) Arquitetura e modulos
- Novo modulo `ai` com separacao por camadas:
  - `domain/ai`
  - `application/ai`
  - `infrastructure/ai`
  - `interfaces/rest`
- IA mantida fora da camada de calculo deterministico de score.

### 2) Contratos/DTOs de IA
Fluxos implementados com schema explicito:
1. `MatchEnrichmentResponse`
2. `CvImprovementResponse`
3. `ApplicationDraftSuggestionResponse`

Todos incluem sinais deterministicos (`score/recommendation/breakdown`) e flag `fallbackUsed`.

### 3) Pipeline controlado de IA
Implementado em `AiEnrichmentService`:
1. carrega dados minimos necessarios
2. usa `MatchingUseCase` como fonte de verdade deterministica
3. sanitiza/trunca dados externos no prompt
4. gera prompt versionado e fechado
5. chama provider via porta `AiTextGenerationPort`
6. valida output por schema (`AiOutputValidator`)
7. em falha/output invalido aplica fallback (`AiFallbackFactory`)
8. registra metricas e logs estruturados

### 4) Seguranca contra prompt injection e consumo inseguro
- Entrada da vaga/perfil/curriculo tratada como **untrusted data**.
- Prompt com politica explicita para ignorar instrucoes embutidas em dados externos.
- Sanitizacao e truncamento aplicados em todos os campos de contexto.
- Saida validada por JSON schema esperado e bloqueio de snippets suspeitos.
- Provider com:
  - HTTPS obrigatorio
  - host em allowlist
  - API key externa
  - timeout/retry limitados
- Nenhuma URL arbitraria controlada por usuario.

### 5) Endpoints e controle de abuso
Rotas adicionadas:
- `POST /api/v1/ai/matches/{vacancyId}/enrichment`
- `POST /api/v1/ai/matches/{vacancyId}/cv-improvement`
- `POST /api/v1/ai/matches/{vacancyId}/application-draft`

Controles:
- autenticacao/autorizacao herdadas da malha de seguranca existente;
- ownership preservado (sem acesso cruzado);
- rate limit dedicado `ai-enrichment`.

### 6) Observabilidade de IA
Novos sinais no `OperationalMetricsService`:
- `recordAiCallStarted`
- `recordAiCallCompleted`
- `recordAiCallDuration`
- `recordAiFallback`

Implementacao Micrometer adicionada com cardinalidade baixa (`flow`, `provider`, `outcome`, `reason`).

## Testes e evidencias

### Comando executado
- `./mvnw.cmd -B test`

### Resultado
- `Tests run: 59, Failures: 0, Errors: 0, Skipped: 2`
- `BUILD SUCCESS`

### Cobertura nova relevante
- `AiOutputValidatorTest`:
  - schema valido
  - rejeicao de conteudo tipo prompt injection
- `AiEnrichmentServiceTest`:
  - fallback quando provider falha
- `AiConfigurationValidatorTest`:
  - bloqueio de misconfiguration em profile estrito
- `AiRateLimitIntegrationTest`:
  - 429 em excedencia de limite das rotas de IA
- `SecurityAuthorizationIntegrationTest` atualizado:
  - ownership aplicado tambem nas rotas de IA

## OWASP / API Security mapeado
- A01 Broken Access Control: ownership e autenticacao preservados.
- A03 Injection: sanitizacao e validacao de output; prompt fechado.
- A05/API8 Misconfiguration: validator para staging/prod com requisitos de provider.
- A09 Logging & Monitoring Failures: metricas e logs estruturados para sucesso/falha/fallback.
- A10 SSRF / API10 Unsafe Consumption: sem URL arbitraria; host allowlist + HTTPS.
- API4 Unrestricted Resource Consumption: rate limit dedicado + timeout/retry curtos.

## Riscos remanescentes
1. Provider real nao foi validado online neste bloco (ambiente local sem chamada externa real).
2. Qualidade textual da IA ainda nao foi calibrada com dataset real de usuarios (somente valida estrutura e seguranca).
3. Prompt injection semantico sofisticado ainda pode exigir hardening adicional com classificadores dedicados em ciclo futuro.

## Proximo passo recomendado
Bloco 8.1: validacao online em staging com provider real, testes de timeout/erro HTTP reais, tuning de prompts por qualidade e observacao de custo/latencia por fluxo.
