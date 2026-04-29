# Checkpoint Tecnico - README + LICENSE Public Readiness Fix
Data: 2026-04-29

## Versao atual do sistema
O ApplyFlow permanece no estado consolidado pos-runtime validation, UX de candidatura, sessao frontend segura, ingestao multi-source, painel admin operacional, priorizacao segura de vagas, ingestao query-driven por preferencias do usuario, CI/CD minimo e repository hygiene.

Nesta data foi executado bloco de public release hardening e portfolio polish, sem novas features, sem mudanca de DTO, sem regra de negocio nova e sem alteracao arquitetural.

Referencia oficial desta versao:
- `docs/checkpoints/2026-04-29-readme-license-public-readiness-fix.md`

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
- Smoke runtime e CI/CD minimo seguem versionados.

## Public release hardening
- README raiz corrigido em portugues pt-BR com blocos Markdown fechados corretamente.
- `SECURITY.md` criado com politica de reporte e regras de secrets.
- `CONTRIBUTING.md` criado com fluxo basico de contribuicao e validacao.
- `.env.example` raiz reduzido a ponteiro seguro.
- `apps/backend/.env.example` padronizado sem valores sensiveis reais.
- `apps/frontend/.env.example` criado.
- `docs/architecture/repository-structure.md` criado.
- LICENSE MIT criado para publicacao publica.

## Seguranca aplicada
- Ownership por `userId`.
- RBAC `USER`/`ADMIN`.
- Provider allowlist.
- Normalizacao Unicode e bloqueio de caracteres de controle.
- Limite de preferencias por usuario.
- Rate limit em fluxos sensiveis.
- Storage privado para curriculos.
- Validacao de PDF por assinatura e limite de tamanho.
- Logs sem token/payload bruto intencional.
- Dedupe/qualityScore preservados no pipeline existente.
- Repository hygiene gate versionado.

## Evidencias recentes
- Backend tests (2026-04-28):
  - `.\mvnw.cmd -B test -DskipITs`;
  - 76 testes, 0 falhas, 0 erros, 2 skipped.
- Frontend quality gate (2026-04-28):
  - `npm run lint` -> OK (`tsc --noEmit`);
  - `npm run typecheck` -> OK;
  - `npm run build` -> OK.
- Runtime smoke E2E (2026-04-28):
  - script implementado em `apps/backend/ops/smoke`;
  - runtime staging reportado como validado com `SMOKE_RUNTIME_RESULT=PASS`.
- CI/CD (2026-04-28):
  - `.github/workflows/ci.yml` criado com `backend-test`, `frontend-quality`, `repository-hygiene`;
  - `.github/workflows/runtime-smoke.yml` criado como workflow manual;
  - `.github/dependabot.yml` criado.
- README + LICENSE public readiness fix (2026-04-29):
  - README corrigido com Markdown valido;
  - LICENSE MIT criado;
  - `SECURITY.md` atualizado;
  - backend tests, frontend lint/typecheck/build e secrets scan passando.

## Limitacoes conhecidas
- Adzuna depende de credenciais externas.
- Greenhouse nao suporta busca global no conector atual.
- ESLint semantico ainda nao foi reintroduzido; `npm run lint` e gate minimo com `tsc --noEmit`.
- Runtime smoke manual exige secrets configurados.
- GitHub Actions remoto ainda precisa ser validado apos push/PR.
- Branch protection depende de configuracao manual no GitHub.
- Secret scanning/push protection depende de disponibilidade/configuracao do GitHub.
- LICENSE MIT definido; publicacao ainda depende de branch protection, secret scanning/push protection quando disponivel e validacao remota do GitHub Actions.

## Proxima retomada segura
1. Reexecutar validacoes finais deste bloco e revisar scanner de secrets.
2. Fazer push/PR e validar GitHub Actions remoto.
3. Configurar branch protection manualmente para `main` exigindo `backend-test`, `frontend-quality` e `repository-hygiene`.
4. Ativar secret scanning/push protection no GitHub quando disponivel.
5. Confirmar publicacao no GitHub apos branch protection e secret scanning/push protection.
