# Checkpoint Tecnico - CI/CD & Repository Protection

## 1. Objetivo do bloco

Implementar CI/CD minimo, protecao operacional de repositorio e prevencao de regressao para impedir que a `main` receba mudancas sem validacao basica de backend, frontend e higiene de segredos.

## 2. Estado antes do bloco

- Backend tests: PASS.
- Frontend build: PASS.
- Frontend lint minimo/typecheck: PASS.
- Smoke E2E automatizado existente.
- Runtime staging reportado como validado com `SMOKE_RUNTIME_RESULT=PASS`.
- Secrets locais temporarios removidos no bloco anterior.
- Ja existia `.github/workflows/ci.yml`, mas com jobs genericos (`backend`, `frontend`) e sem `repository-hygiene`.

## 3. Workflows criados

### ci.yml

Criado/reescrito em `.github/workflows/ci.yml` com triggers:

- `pull_request` para `main`;
- `push` para `main`.

### runtime-smoke.yml

Criado em `.github/workflows/runtime-smoke.yml` com `workflow_dispatch`.

O smoke runtime exige secrets:

- `JWT_SECRET_BASE64`;
- `ACTUATOR_METRICS_TOKEN`;
- `SMOKE_ADMIN_EMAIL`;
- `SMOKE_ADMIN_PASSWORD`.

Se ausentes, falha com mensagem clara sem imprimir valores.

### dependabot.yml

Criado em `.github/dependabot.yml` para:

- Maven em `/apps/backend`;
- npm em `/apps/frontend`;
- GitHub Actions em `/`.

Frequencia: `weekly`.

## 4. Jobs de CI

### backend-test

- Ubuntu latest.
- Java Temurin 21.
- Cache Maven via `actions/setup-java`.
- Comando: `./mvnw -B test -DskipITs`.

### frontend-quality

- Ubuntu latest.
- Node 20.
- Cache npm por `apps/frontend/package-lock.json`.
- Comandos:
  - `npm ci`;
  - `npm run lint`;
  - `npm run typecheck`;
  - `npm run build`.

### repository-hygiene

Valida arquivos versionados com nomes sensiveis e strings suspeitas.

Permite:

- `.env.example`;
- migrations SQL em `apps/backend/src/main/resources/db/migration`;
- classes Java de dominio/teste com `Token` no nome.

Bloqueia artefatos como `.env`, chaves, dumps, arquivos token/secret fora das excecoes e strings como chave privada, AWS secret key, atribuicao de segredo JWT e URL Postgres inline.

## 5. Protecao de secrets

### Gate implementado

`repository-hygiene` implementado no CI.

Validacao local equivalente: PASS.

### Push protection recomendada

Documentado em `docs/operations/repository-protection.md`:

- Secret scanning;
- Push protection;
- Dependabot alerts;
- Dependabot security updates.

### Riscos residuais

- O gate de higiene e minimo e nao substitui secret scanning completo.
- Push protection depende de disponibilidade/plano/configuracao do GitHub.

## 6. Branch protection

### Checks recomendados

- `backend-test`;
- `frontend-quality`;
- `repository-hygiene`.

### Configuracao manual necessaria

Configurar no GitHub:

`Settings -> Branches -> Add branch protection rule`

Para `main`:

- require pull request before merging;
- require status checks;
- require branches to be up to date before merging;
- require conversation resolution;
- block force pushes;
- block deletions;
- opcional: require signed commits.

### Limitacoes

- Branch protection ainda nao esta concluida ate ser configurada manualmente no GitHub.
- Status checks so aparecem depois de rodarem ao menos uma vez.
- Execucao remota do CI ainda nao foi validada.

## 7. Validacao local

### Backend tests

- `.\mvnw.cmd -B test -DskipITs`
- Resultado: PASS (`76 tests`, `0 failures`, `0 errors`, `2 skipped`).

### Frontend lint

- `npm run lint`
- Resultado: PASS.

### Frontend typecheck

- `npm run typecheck`
- Resultado: PASS.

### Frontend build

- `npm run build`
- Resultado: PASS.

## 8. Seguranca validada

- CI padrao nao depende de secrets reais.
- Smoke runtime manual valida presenca de secrets antes de subir stack.
- Gate de higiene local passou.
- Nenhum arquivo `.env`, token temporario, dump ou chave foi identificado como versionado.

## 9. Arquivos alterados

- `.github/workflows/ci.yml`
- `.github/workflows/runtime-smoke.yml`
- `.github/dependabot.yml`
- `docs/operations/repository-protection.md`
- `docs/checkpoints/2026-04-28-ci-cd-repository-protection.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`
- `context/PROJECT_STATE.md`
- `context/TASKS.md`
- `context/DECISIONS.md`
- `apps/backend/ops/smoke/run-staging-runtime-smoke.ps1`

## 10. Pendencias

### Criticas

- Configurar branch protection manualmente no GitHub.
- Fazer push para branch e validar GitHub Actions remoto.

### Importantes

- Ativar secret scanning/push protection no GitHub quando disponivel.
- Confirmar que os checks aparecem com nomes unicos apos a primeira execucao.

### Melhorias futuras

- Reintroduzir lint semantico com ESLint versionado.
- Evoluir `repository-hygiene` para script versionado reutilizavel localmente e no CI.

## 11. Proximo passo recomendado

Abrir PR ou push controlado para branch, validar os tres jobs no GitHub Actions e configurar branch protection da `main` exigindo `backend-test`, `frontend-quality` e `repository-hygiene`.

## 12. Resumo executivo

- CI minimo criado.
- Dependabot criado.
- Smoke runtime manual criado.
- Protecao de secrets minima criada.
- Documentacao de branch/secret protection criada.
- Validacao local passou.
- CI remoto e branch protection ainda precisam ser configurados/validados no GitHub.
