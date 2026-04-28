# Retomada Tecnica - ApplyFlow
Data: 2026-04-28

## 1. Estado real do projeto
- Backend compila e testes passam (`.\mvnw.cmd -B test -DskipITs`).
- Frontend builda com sucesso (`npm run build`).
- Projeto segue arquitetura modular por dominio (`ai`, `applications`, `auth`, `matching`, `resumes`, `vacancies`, `shared`).
- Fluxo principal existe no codigo, mas runtime E2E nao foi reexecutado nesta retomada.

## 2. Ultimo checkpoint encontrado
- `context/CHECKPOINT_TECNICO_ATUAL.md` aponta para:
  - `docs/checkpoints/2026-04-24-query-driven-ingestion-and-repository-bootstrap.md`

## 3. Git
- branch: `main`
- commit: `b32a53b` (`chore: bootstrap seguro do ApplyFlow`)
- status: sem alteracoes tracked antes desta retomada
- arquivos sensiveis detectados (ignorados por git):
  - `apps/backend/infra/staging/.env`
  - `tmp_token.txt`
  - `tmp_token_runtime.txt`
- arquivos obrigatorios ausentes:
  - `docs/checkpoints/README.md`
  - `apps/backend/README.md`
  - `apps/frontend/README.md`

## 4. Backend
### 4.1 Estrutura
- Camadas presentes por modulo: `application`, `infrastructure`, `interfaces`, `domain` (variando por contexto).
- Seguranca central em `shared/infrastructure/security`.
- Rate limit central em `shared/infrastructure/ratelimit`.

### 4.2 Endpoints
- Vacancies:
  - `GET /api/v1/vacancies` (ok no controller)
- Matching:
  - `POST /api/v1/matches` (ok)
  - `GET /api/v1/matches/vacancy/{vacancyId}` (ok)
  - `GET /api/v1/matches/vacancy/{vacancyId}/summary` (ok)
- Applications:
  - `POST /api/v1/applications/drafts` (ok)
  - `PATCH /api/v1/applications/{id}/status` (ok)
  - `GET /api/v1/applications/{id}/tracking` (ok)
- Resumes:
  - upload PDF em `POST /api/v1/resumes` multipart (ok)
  - listagem base em `GET /api/v1/resumes` (ok)
  - criacao de variante em `POST /api/v1/resumes/{id}/variants` (ok)
  - endpoint de listagem de variantes dedicado: **NAO ENCONTRADO**
- Admin:
  - `GET /api/v1/admin/ingestion/overview` (ok, `@PreAuthorize("hasRole('ADMIN')")`)

### 4.3 Migrations
- Ultima migration: `V11__user_job_search_preferences.sql`
- Blocos existentes:
  - resumes/PDF: `V4`
  - matching: `V6`
  - dedupe/quality: `V7`
  - ingestion: `V3`, `V5`, `V8`, `V9`, `V10`, `V11`
  - tracking/applications: presente desde base (`V1`) + evolucoes de aplicacao

### 4.4 Testes
- Comando: `.\mvnw.cmd -B test -DskipITs`
- Resultado:
  - Tests run: `76`
  - Failures: `0`
  - Errors: `0`
  - Skipped: `2`
  - Build: `SUCCESS`
- Observacao: warnings/logs sao de cenarios controlados de teste (fallback/unavailable), sem quebra da suite.

### 4.5 Problemas
- Ausencia dos READMEs obrigatorios listados no prompt.
- Presenca de arquivos temporarios com token no workspace (ignorados, mas sensiveis operacionalmente).

## 5. Frontend
### 5.1 Telas
- Presentes:
  - `/vagas`
  - `/vagas/[id]`
  - `/candidaturas`
  - `/candidaturas/[id]`

### 5.2 Fluxos
- Consumo de API mapeado para `vacancies`, `matching/match-adapter`, `applications`, `resumes`.
- Tratamento de erro presente nas telas (mensagens seguras).
- Estados vazios presentes em vagas/candidaturas.
- Feature flag identificada:
  - `NEXT_PUBLIC_AI_ACTIONS_ENABLED` (`src/lib/config/features.ts`)
- Sessao:
  - em memoria (`src/lib/auth/session.ts`), sem `localStorage/sessionStorage`.

### 5.3 Build
- `npm run build`: **OK**
- TypeScript executado no build: **OK**
- `npm run typecheck`: **NAO VALIDADO** (script nao existe em `package.json`)
- `npm run lint`: **ERRO**
  - `next lint` invalido em Next 16 no estado atual (erro de diretorio `...\\frontend\\lint`)

### 5.4 Problemas
- Pipeline de lint do frontend quebrado por script desatualizado.

## 6. Fluxo principal
`vacancy -> match -> draft -> status -> tracking`

Status: **Parcial**
- Evidencia de codigo e testes unit/integration existe.
- Validacao runtime E2E completa nesta retomada: **NAO VALIDADO**.

## 7. Seguranca
### 7.1 Controles OK
- JWT stateless + filtro de autenticacao.
- Refresh token via cookie HttpOnly/SameSite/secure.
- RBAC com roles `USER`/`ADMIN` em config global e `@PreAuthorize`.
- Ownership aplicado em servicos de `matching`, `applications`, `resumes`, `job-search-preferences`.
- Rate limit ativo por endpoint com headers e modo observavel.
- Handler global sem stack trace no corpo da resposta.
- Upload PDF com validacao de content-type, assinatura `%PDF-`, limite de tamanho.
- Sem `dangerouslySetInnerHTML` detectado no frontend.
- Sem sessao persistida em `localStorage/sessionStorage`.

### 7.2 Riscos
- Arquivos temporarios com token no workspace raiz (`tmp_token*.txt`) apesar de ignorados.
- Ausencia de validação runtime nesta rodada para confirmar storage privado e politicas em execucao real.

### 7.3 Criticos
- Nenhuma falha critica de seguranca nova encontrada no codigo analisado.
- **CRITICO OPERACIONAL**: manter arquivos de token local eleva risco de vazamento humano/acidental fora do git.

## 8. Pendencias
### Critico
- Remover/rotacionar tokens de arquivos temporarios locais e reforcar rotina operacional de limpeza.

### Importante
- Corrigir script de lint frontend para compatibilidade com Next 16.
- Adicionar arquivos obrigatorios ausentes (`docs/checkpoints/README.md`, `apps/backend/README.md`, `apps/frontend/README.md`).
- Reexecutar smoke runtime E2E do fluxo principal com evidencia HTTP.

### Melhorias
- Adicionar script `typecheck` explicito no frontend.
- Automatizar validacao de higiene de workspace (tokens/tmp sensiveis).

## 9. Proximo passo recomendado
1. Corrigir pipeline de qualidade frontend (`lint` e `typecheck` explicito).
2. Executar smoke E2E controlado do fluxo `vacancy -> match -> draft -> status -> tracking` com evidencias versionadas.
3. Limpar artefatos sensiveis temporarios e rotacionar token usado em testes manuais.

## 10. Resumo executivo
- Backend: **OK** (76/0/0/2).
- Frontend: **build OK**, lint **quebrado**.
- Arquitetura e seguranca estrutural: **coerentes** no codigo.
- Fluxo principal: **parcialmente validado** (codigo + testes), sem revalidacao runtime E2E nesta rodada.
- Drift de ambiente: **NAO VALIDADO** nesta retomada (compose e migrations estao coerentes em codigo, mas sem prova de execucao agora).
