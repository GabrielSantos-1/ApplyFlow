# Repository Protection - ApplyFlow

## Objetivo

Impedir regressao na `main` com gates minimos, auditaveis e sem dependencia de secrets reais no CI padrao.

## Workflows obrigatorios

- `.github/workflows/ci.yml`
  - `backend-test`
  - `frontend-quality`
  - `repository-hygiene`
- `.github/workflows/runtime-smoke.yml`
  - `runtime-smoke` manual por `workflow_dispatch`
- `.github/dependabot.yml`
  - Maven backend
  - npm frontend
  - GitHub Actions

## Branch protection para `main`

Configurar manualmente no GitHub:

`Settings -> Branches -> Add branch protection rule`

Regras recomendadas:

- Branch name pattern: `main`
- Require a pull request before merging
- Require status checks to pass before merging
- Require branches to be up to date before merging
- Require conversation resolution before merging
- Block force pushes
- Block deletions
- Opcional: Require signed commits

Checks obrigatorios:

- `backend-test`
- `frontend-quality`
- `repository-hygiene`

Observacoes:

- Os status checks so aparecem depois de executarem ao menos uma vez no repositorio.
- Os nomes dos jobs devem permanecer unicos para evitar checks ambiguos.
- Branch protection nao fica concluida apenas por arquivo versionado; exige configuracao manual no GitHub.

Referencia oficial:

- [GitHub Branch Protection](https://docs.github.com/articles/about-required-status-checks)
- [Managing a branch protection rule](https://docs.github.com/articles/enabling-required-status-checks)

## Secret protection

Configurar manualmente no GitHub:

`Settings -> Code security and analysis`

Recomendado ativar quando disponivel:

- Secret scanning
- Push protection
- Dependabot alerts
- Dependabot security updates

Observacoes:

- Disponibilidade de secret scanning/push protection varia por tipo de repositorio, organizacao e plano.
- Push protection bloqueia secrets antes de chegarem ao repositorio quando habilitada.
- O job `repository-hygiene` e uma protecao minima adicional; ele nao substitui secret scanning completo.

Referencia oficial:

- [GitHub Push Protection](https://docs.github.com/en/code-security/secret-scanning/introduction/about-push-protection)
- [Secret scanning and push protection](https://docs.github.com/code-security/secret-scanning/protecting-pushes-with-secret-scanning)

## Runtime smoke manual

O smoke runtime nao roda em todo PR porque depende de Docker, stack staging e secrets operacionais.

Workflow manual:

- `.github/workflows/runtime-smoke.yml`

Secrets exigidos:

- `JWT_SECRET_BASE64`
- `ACTUATOR_METRICS_TOKEN`
- `SMOKE_ADMIN_EMAIL`
- `SMOKE_ADMIN_PASSWORD`

Se os secrets estiverem ausentes, o workflow falha com mensagem explicita sem expor valores.
