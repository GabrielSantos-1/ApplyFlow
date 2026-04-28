# Checkpoint Tecnico - Runtime Validation & Operational Hardening
Data: 2026-04-28

## Objetivo do bloco

- Automatizar smoke E2E do fluxo principal.
- Endurecer validacao de runtime/ambiente.
- Melhorar higiene operacional de segredos locais.
- Corrigir gate de qualidade do frontend no contexto de Next 16.

## Escopo executado

### 1) Smoke E2E automatizado

Criados scripts:

- `apps/backend/ops/smoke/run-runtime-smoke.ps1`
  - valida `health`;
  - valida `401` sem token em `/vacancies`;
  - autentica (`/auth/login` ou token existente);
  - valida `/auth/me`;
  - executa fluxo `vacancy -> match -> draft -> status -> tracking`;
  - valida transicao invalida (`400`) e validas (`200`);
  - valida estado `GENERATED` em match;
  - valida `SUBMITTED` no tracking;
  - valida endpoints de `job-search-preferences` para detectar drift de schema runtime.

- `apps/backend/ops/smoke/run-staging-runtime-smoke.ps1`
  - sobe stack staging via compose;
  - bootstrap admin efemero por variavel de ambiente;
  - executa smoke script;
  - derruba stack ao final.

### 2) Runtime/operacao documentados

Criado:

- `docs/operations/runtime-smoke.md`

Com pre-condicoes, comandos e criterios de sucesso.

### 3) Higiene de secrets

Removidos do workspace:

- `tmp_token.txt`
- `tmp_token_runtime.txt`

### 4) Documentacao minima ausente

Criados:

- `apps/backend/README.md`
- `apps/frontend/README.md`
- `docs/checkpoints/README.md`

### 5) Gate de qualidade frontend

Ajustado `apps/frontend/package.json`:

- `lint` -> `tsc --noEmit`
- `typecheck` -> `tsc --noEmit`

Motivo: `next lint` estava quebrado/incompativel no estado atual com Next 16 e sem stack ESLint versionada no repositorio.

## Validacoes executadas

### Frontend

- `npm run lint` -> OK
- `npm run typecheck` -> OK
- `npm run build` -> OK

### Backend

- `.\mvnw.cmd -B test -DskipITs`
- Resultado: `76 tests`, `0 failures`, `0 errors`, `2 skipped`

### Smoke runtime em staging

Tentativas:

1. sem variaveis obrigatorias -> falha esperada de compose por `ACTUATOR_METRICS_TOKEN` ausente.
2. com `ACTUATOR_METRICS_TOKEN` -> falha esperada por `JWT_SECRET_BASE64` ausente.
3. com ambas variaveis -> falha de infraestrutura local:
   - Docker Engine indisponivel (`//./pipe/docker_engine` nao encontrado).

Conclusao:

- Smoke automatizado implementado.
- Execucao runtime ponta-a-ponta no ambiente atual: **NAO VALIDADA** por indisponibilidade de Docker Engine local.

## Riscos e observacoes

- Sem Docker Engine ativo, nao ha validacao operacional real de stack staging.
- `lint` atualmente cobre tipagem; lint semantico ESLint segue pendente de reintroducao controlada.

## Proximo passo recomendado

1. Executar `run-staging-runtime-smoke.ps1` em host com Docker Engine ativo.
2. Persistir evidencia de execucao (`PASS`) em novo checkpoint operacional.
3. Planejar reintroducao de lint semantico (ESLint) com configuracao versionada e sem quebrar pipeline.
