# Contributing to ApplyFlow

## Scope

Contributions must preserve the current architecture and security model. Do not change DTOs, business rules, authentication, matching, application state, resume handling, or ingestion behavior unless the change is explicitly scoped and reviewed.

## Branches

Use short, descriptive branches. Recommended pattern:

```text
codex/<technical-scope>
```

Examples:

- `codex/repository-docs-hardening`
- `codex/frontend-quality-gate`

## Local Validation

Before opening a pull request, run:

```powershell
cd apps/backend
.\mvnw.cmd -B test -DskipITs
```

```powershell
cd apps/frontend
npm run lint
npm run typecheck
npm run build
```

From the repository root, review hygiene:

```powershell
git status --ignored
git diff
git diff --cached
```

## Pull Requests

A pull request should include:

- a clear description of the technical scope;
- security impact, if any;
- commands executed locally;
- known limitations or follow-up work.

Required CI checks:

- `backend-test`
- `frontend-quality`
- `repository-hygiene`

## Commit Messages

Use concise commit messages in Portuguese, consistent with the repository history.

Example:

```text
chore: atualiza documentacao publica e politica de seguranca
```

## Secrets and Sensitive Data

Never commit or paste:

- `.env` files;
- tokens, passwords, cookies, or JWTs;
- private keys or certificates;
- database dumps or logs;
- real resumes or candidate data;
- private API provider credentials.

If a secret is exposed, remove it from the change and rotate it before any push or publication.
