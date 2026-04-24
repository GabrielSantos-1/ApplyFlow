# Checkpoint Tecnico - Query-Driven Ingestion + Repository Bootstrap
Data: 2026-04-24

## Estado atual do produto
O ApplyFlow esta consolidado como monolito modular Java/Spring Boot + Next.js + PostgreSQL, com fluxo principal validado em runtime:

```text
vacancy -> match -> draft -> status -> tracking
```

O backend permanece fonte de verdade para matching, score, recomendacao, ownership, transicoes de status, dedupe e qualityScore. O frontend atua como camada de apresentacao e nao decide regras de negocio.

## Blocos concluidos
- Runtime validation do fluxo completo de candidatura.
- UX de candidatura e timeline consolidada em `/candidaturas` e `/candidaturas/[id]`.
- Sessao frontend segura com token em memoria e reidratacao via refresh HttpOnly cookie.
- Ingestao multi-source expandida com Remotive e Greenhouse curados.
- Dedupe e qualityScore ativos.
- Painel admin operacional de ingestao: `GET /api/v1/admin/ingestion/overview`.
- Priorizacao segura de vagas em `/vagas`, usando apenas dados do backend.
- Ingestao query-driven por preferencias do usuario.
- Preparacao segura para bootstrap do repositorio Git/GitHub.

## Ingestao multi-source
- Remotive ativo como provider principal.
- Greenhouse ativo como board-curated para fontes confiaveis.
- Adzuna preparado para query/location quando credenciais forem configuradas.
- Scheduler preservado e configuravel por ambiente.
- Logs agregados de ingestao mantidos sem payload bruto sensivel.

## Query-driven ingestion
Foi criado o modelo `UserJobSearchPreference` para pesquisas controladas:

- `id`
- `userId`
- `keyword`
- `normalizedKeyword`
- `location`
- `remoteOnly`
- `seniority`
- `provider`
- `enabled`
- `createdAt`
- `updatedAt`
- `lastRunAt`

Endpoints criados:

- `GET /api/v1/job-search-preferences`
- `POST /api/v1/job-search-preferences`
- `PATCH /api/v1/job-search-preferences/{id}`

Fluxo operacional:

```text
preferencia ativa -> scheduler -> connector com query segura -> normalizer -> dedupe -> qualityScore -> vacancies
```

## Providers suportados
- `REMOTIVE`: suporta busca por keyword com limite operacional por preferencia.
- `ADZUNA`: preparado para `what`/`where`, condicionado a credenciais e enablement.
- `GREENHOUSE`: mantido como board-curated; nao foi forcada busca global porque o conector atual trabalha por board token.

## Dedupe e qualityScore
- Dedupe continua ativo e nao foi removido ou enfraquecido.
- `qualityScore` continua sendo calculado no pipeline existente.
- Preferencias query-driven reutilizam o mesmo caminho de persistencia da ingestao consolidada.
- Na validacao runtime, `QA` e `Java Developer` buscaram resultados, mas todos foram deduplicados por ja existirem no banco.

## Painel admin de ingestao
O endpoint admin operacional permanece disponivel:

- `GET /api/v1/admin/ingestion/overview`

Dados retornados seguem agregados, sem `raw_payload`, PII ou dados individuais de alta cardinalidade.

## Priorizacao segura de vagas
`/vagas` segue usando somente dados do backend:

- score;
- recommendation;
- publishedAt;
- qualityScore quando disponivel;
- strengths/gaps retornados pelo backend.

O frontend nao recalcula score, nao altera recommendation e nao corrige divergencias localmente.

## Seguranca aplicada
- Ownership por `userId` em preferencias.
- Provider allowlist para pesquisa controlada.
- Normalizacao Unicode de keyword/location.
- Bloqueio de caracteres de controle.
- Limite de preferencias por usuario.
- Rate limit nos endpoints de preferencias.
- Query params montados com builder/encoding, sem concatenar input bruto.
- Sem URL arbitraria fornecida pelo usuario.
- Logs usam hash da keyword, nao termo completo.
- Anonimo recebe `401`.
- Outro usuario nao acessa nem altera preferencia alheia.
- Sem alteracao de contratos publicos existentes de vacancies.
- Sem remocao de dedupe, qualityScore ou ownership.

## Validacoes executadas
- Backend:
  - `.\mvnw.cmd -B test -DskipITs`;
  - 76 testes, 0 falhas, 0 erros, 2 skipped.
- Runtime staging:
  - Flyway validou 11 migrations;
  - `POST /api/v1/job-search-preferences` com `QA` retornou `201`;
  - `GET /api/v1/job-search-preferences` retornou `200`;
  - input invalido retornou `400`;
  - anonimo retornou `401`;
  - usuario diferente recebeu lista vazia e `PATCH` em preferencia alheia retornou `404`;
  - scheduler executou preferencias `QA` e `Java Developer` com `SUCCESS`;
  - contagem de vagas permaneceu `222` porque os resultados foram deduplicados.
- Frontend:
  - ultimo checkpoint de `/vagas` validou `cmd /c npm run build` com sucesso.

## Limitacoes conhecidas
- Adzuna depende de credenciais reais configuradas fora do repositorio.
- Greenhouse segue limitado a boards curados.
- Aumento de volume depende de as APIs externas retornarem vagas ainda nao cobertas pelo dedupe.
- `qualityScore` ainda e opcional no frontend porque nao foi assumido como campo oficial de contrato de `VacancyResponse`.
- Repositorio remoto ainda precisava de bootstrap seguro antes deste checkpoint.

## Proximo passo recomendado
Versionar o estado atual em Git/GitHub apos:

1. criar `.gitignore` raiz abrangente;
2. garantir `.env.example` sem segredo real;
3. varrer arquivos sensiveis;
4. executar backend tests e frontend build;
5. confirmar que `.env`, temporarios, builds e artefatos locais nao serao commitados.
