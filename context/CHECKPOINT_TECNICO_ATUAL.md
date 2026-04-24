# Checkpoint Tecnico - Query-Driven Ingestion + Repository Bootstrap
Data: 2026-04-24

## Versao atual do sistema
O ApplyFlow esta no estado consolidado pos-runtime validation, UX de candidatura, sessao frontend segura, ingestao multi-source, painel admin operacional, priorizacao segura de vagas e ingestao query-driven por preferencias do usuario.

Referencia oficial desta versao:
- `docs/checkpoints/2026-04-24-query-driven-ingestion-and-repository-bootstrap.md`

## Fluxo principal consolidado
```text
vacancy -> match -> draft -> status -> tracking
```

## Estado consolidado
- Backend Java/Spring Boot e PostgreSQL seguem como fonte de verdade.
- Frontend Next.js atua como apresentacao e nao decide regra de negocio.
- Matching segue deterministico.
- Ownership permanece obrigatorio.
- Dedupe e qualityScore seguem ativos.
- Rate limit segue aplicado a fluxos sensiveis.
- Painel admin de ingestao esta operacional.
- `/vagas` prioriza oportunidades sem recalcular score no frontend.

## Query-driven ingestion
Foi criado suporte a pesquisas controladas por usuario em `UserJobSearchPreference`.

Endpoints:
- `GET /api/v1/job-search-preferences`
- `POST /api/v1/job-search-preferences`
- `PATCH /api/v1/job-search-preferences/{id}`

Providers:
- `REMOTIVE`: busca por keyword.
- `ADZUNA`: busca por keyword/location quando credenciais existirem.
- `GREENHOUSE`: mantido como board-curated.

## Seguranca aplicada
- Ownership por `userId`.
- Provider allowlist.
- Normalizacao Unicode e bloqueio de caracteres de controle.
- Limite de preferencias por usuario.
- Rate limit em leitura/escrita.
- Sem URL arbitraria do usuario.
- Logs com hash de keyword.
- Dedupe/qualityScore preservados no pipeline existente.
- Outro usuario nao acessa preferencia alheia.

## Evidencias recentes
- Backend tests:
  - `.\mvnw.cmd -B test -DskipITs`;
  - 76 testes, 0 falhas, 0 erros, 2 skipped.
- Runtime:
  - migration V11 aplicada/validada por Flyway;
  - `POST /api/v1/job-search-preferences` -> `201`;
  - `GET /api/v1/job-search-preferences` -> `200`;
  - anonimo -> `401`;
  - input invalido -> `400`;
  - acesso cruzado -> `404`;
  - scheduler executou `QA` e `Java Developer` com `SUCCESS`;
  - total de vagas permaneceu 222 porque resultados foram deduplicados.

## Limitacoes conhecidas
- Adzuna depende de credenciais externas.
- Greenhouse nao suporta busca global no conector atual.
- Aumento real de volume depende de resultados novos nas APIs externas.
- Repositorio Git/GitHub deve ser inicializado com varredura de segredos e `.gitignore` abrangente.

## Proxima retomada segura
1. Finalizar bootstrap Git/GitHub sem versionar segredos, temporarios ou artefatos locais.
2. Criar endpoint batch/agregador para `/vagas`.
3. Criar teste de navegador para `/vagas` e fluxo de candidatura.
4. Criar tela admin frontend para consumir o overview de ingestao.
