# Checkpoint Técnico — Runtime Validation + Flow Consistency
Data: 2026-04-24

## 1) Objetivo
Validar em runtime o fluxo principal do ApplyFlow, ponta a ponta, no checkpoint consolidado de 2026-04-23, sem alterar arquitetura, DTOs, endpoints ou lógica de negócio.

Fluxo validado:

```text
vacancy -> match -> draft -> status -> tracking
```

## 2) Mapa do fluxo validado
Ambiente validado:
- Staging local em `http://localhost:8081`.
- Backend Spring Boot em profile `staging`.
- PostgreSQL e Redis via Docker Compose.
- Flyway confirmado em `v7` após rebuild/restart.

Sequência validada:

1. `GET /api/v1/vacancies`
   - Retorno: `200`.
   - Vaga selecionada: `bb2f8c83-f532-4599-93f5-3b9ce7b76e00`.

2. `POST /api/v1/matches`
   - Payload funcional:
     - `vacancyId=bb2f8c83-f532-4599-93f5-3b9ce7b76e00`
     - `resumeVariantId=c5a0da1b-3185-4146-96d6-31d56fb0d967`
     - `forceRegenerate=true`
   - Retorno: `200`.
   - Estado: `GENERATED`.
   - Score: `0`.
   - Recommendation: `IGNORE`.

3. `GET /api/v1/matches/vacancy/{id}`
   - Retorno: `200`.
   - Estado persistido confirmado: `GENERATED`.
   - Score persistido confirmado: `0`.
   - Recommendation persistida confirmada: `IGNORE`.

4. `POST /api/v1/applications/drafts`
   - Retorno: `201`.
   - Draft criado: `f746d841-1512-429d-a285-2487b2e852a3`.
   - Status inicial: `DRAFT`.

5. `PATCH /api/v1/applications/{id}/status`
   - Transição inválida testada:
     - `DRAFT -> APPLIED`
     - Retorno: `400 BAD_REQUEST`
     - Mensagem: `Transicao de status invalida`
   - Transições válidas testadas:
     - `DRAFT -> READY_FOR_REVIEW`
     - `READY_FOR_REVIEW -> APPLIED`
   - Retorno das transições válidas: `200`.

6. `GET /api/v1/applications/{id}/tracking`
   - Retorno: `200`.
   - Eventos retornados: `3`.
   - Stages: `CREATED,SCREENING,SUBMITTED`.

## 3) Falhas encontradas

### 3.1 Drift de runtime/schema
- Sintoma: banco de staging ativo não possuía colunas da V7, como `is_duplicate`.
- Evidência: `flyway_schema_history` indicava apenas migrations até `v6`.
- Impacto: runtime estava abaixo do checkpoint técnico consolidado de 2026-04-23.
- Risco: divergência entre código, documentação e banco poderia gerar erro funcional e falso diagnóstico de bug de produto.

### 3.2 `500` em tracking antes do rebuild
- Sintoma: `GET /api/v1/applications/{id}/tracking` retornou `500`.
- Evidência: endpoint reproduzido com draft criado durante a validação.
- Impacto: critério "nenhum endpoint retorna 500" falhava antes do alinhamento operacional.

### 3.3 Divergência frontend/backend em transição de status
- Sintoma: a tela de candidaturas renderizava o status atual como botão acionável.
- Comportamento backend correto: `PATCH` com mesmo status ou transição inválida deve ser rejeitado.
- Impacto: usuário poderia disparar uma chamada que resultaria em erro funcional evitável.

## 4) Causas raiz

### Drift de runtime/schema
- Causa raiz: containers backend em execução haviam sido iniciados com imagem anterior, validando apenas 6 migrations.
- Correção correta: rebuild/restart operacional do stack, não alteração de código ou contrato.

### `500` em tracking
- Causa raiz: runtime defasado em relação ao código atual, sem o hardening de tracking já presente no source.
- Correção correta: alinhar runtime ao código consolidado e aplicar Flyway V7.

### Divergência frontend/backend
- Causa raiz: UI permitia ação que não representa transição de domínio.
- Correção correta: impedir acionamento do status atual no frontend, mantendo o backend como autoridade de transição.

## 5) Patch aplicado

### Patch operacional
Executado rebuild/restart do staging local:

```powershell
docker compose -f apps\backend\infra\staging\docker-compose.yml up -d --build
```

Resultado:
- Backend reiniciado com código atual.
- Flyway validou 7 migrations.
- Migration V7 aplicada:
  - `V7__vacancy_data_quality_and_deduplication.sql`
  - `vacancy data quality and deduplication`

### Patch de consistência frontend
Aplicado patch mínimo em:

```text
apps/frontend/src/app/(dashboard)/candidaturas/page.tsx
```

Mudança:
- Botão do status atual passou a ficar `disabled`.
- Não houve mudança de DTO, endpoint, arquitetura ou regra de negócio.
- Backend permanece autoridade para validar transições.

## 6) Validações executadas

### Runtime HTTP
- `GET /api/v1/vacancies` -> `200`.
- `POST /api/v1/matches` -> `200`, `GENERATED`.
- `GET /api/v1/matches/vacancy/{id}` -> `200`, `GENERATED`.
- `POST /api/v1/applications/drafts` -> `201`, `DRAFT`.
- `PATCH /api/v1/applications/{id}/status`:
  - `DRAFT -> APPLIED` -> `400 BAD_REQUEST`.
  - `DRAFT -> READY_FOR_REVIEW` -> `200`.
  - `READY_FOR_REVIEW -> APPLIED` -> `200`.
- `GET /api/v1/applications/{id}/tracking` -> `200`, `CREATED,SCREENING,SUBMITTED`.

### Banco/Flyway
- `flyway_schema_history` confirmou versão final:
  - `version=7`
  - `description=vacancy data quality and deduplication`
  - `success=true`
- Coluna `is_duplicate` confirmada em `vacancies`.

### Testes automatizados
- Backend:

```powershell
.\mvnw.cmd -B test -DskipITs
```

Resultado:
- `72` testes executados.
- `0` falhas.
- `0` erros.
- `2` skipped.

- Frontend:

```powershell
cmd /c npm run build
```

Resultado:
- Build Next.js concluído com sucesso.
- TypeScript sem erro.

## 7) Evidências de segurança
- Endpoints exigem autenticação via Bearer token.
- Ownership validado no tracking:
  - tentativa de leitura do tracking por outro usuário retornou `404 NOT_FOUND`;
  - não houve exposição de dados de candidatura de outro usuário.
- Transição inválida bloqueada pelo backend:
  - `DRAFT -> APPLIED` retornou `400 BAD_REQUEST`.
- Matching preservou autoridade do backend:
  - score e recommendation foram gerados e lidos do backend.
- Nenhuma IA foi usada para lógica de negócio.
- Nenhum novo endpoint foi criado.
- Nenhum DTO foi alterado.
- Nenhuma biblioteca nova foi introduzida.
- Nenhum segredo foi persistido no repositório; tokens temporários de validação foram removidos do workspace.

## 8) Critérios aceitos
- [x] Fluxo completo executa sem erro após alinhamento de runtime.
- [x] Nenhum endpoint do fluxo retorna `500`.
- [x] Transições válidas funcionam.
- [x] Transição inválida é rejeitada com erro funcional controlado.
- [x] Tracking consistente com o histórico de status.
- [x] Ownership garantido.
- [x] Correções mínimas, sem mudança de arquitetura.
- [x] DTOs preservados.
- [x] Endpoints preservados.
- [x] Matching continua determinístico e sem IA no core.

## 9) Próximo passo recomendado
Criar uma validação operacional recorrente de smoke test para staging local/CI controlado cobrindo:

1. versão Flyway esperada;
2. `GET /api/v1/vacancies`;
3. geração e leitura stateful de match;
4. criação de draft;
5. transição válida e inválida de candidatura;
6. tracking com ownership.

Objetivo: detectar drift de runtime/schema antes que ele apareça como falha funcional em produto.
