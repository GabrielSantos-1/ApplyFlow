# Security Policy

## Supported Scope

This policy covers the ApplyFlow repository, including:

- Spring Boot backend code and configuration.
- Next.js frontend code and public build-time configuration.
- Database migrations and operational scripts.
- GitHub Actions workflows, Dependabot configuration, and repository hygiene gates.
- Documentation that could affect secure deployment or public exposure.

This project is not currently advertised as a hosted public SaaS. Security reports should be scoped to the repository and any environment explicitly shared by the maintainer.

## Reporting a Vulnerability

If you believe you found a vulnerability, do not open a public issue containing exploit details, tokens, credentials, private URLs, personal data, or reproduction payloads with sensitive data.

Use a private GitHub contact channel when available, such as GitHub private vulnerability reporting or direct maintainer contact through GitHub. If no private channel is available, open a minimal public issue that states a security contact is needed, without technical exploit details.

## Do Not Report Secrets Publicly

Never post the following in public issues, discussions, pull requests, screenshots, or logs:

- API keys or provider tokens.
- JWTs, refresh tokens, session cookies, or actuator tokens.
- Database URLs, usernames, or passwords.
- Private keys, certificates, keystores, or signing material.
- Real resumes, candidate data, emails, or application records.
- Raw logs containing authentication headers, cookies, or request bodies.

If a real secret was exposed, remove it from the public channel and rotate it immediately. Deleting a GitHub comment is not a complete remediation if the value may have been copied, indexed, or delivered through notifications.

## Data Handling

Local and demo environments must use synthetic data only. Do not upload real resumes, real candidate profiles, real application history, or production exports into development, staging, screenshots, or public bug reports.

Resume files are treated as private data. The backend stores uploaded PDFs under a private storage path and validates upload type and size server-side. Public hosting of stored resumes is not part of the intended design.

## Secrets Policy

- Real `.env` files must never be committed.
- Use `apps/backend/.env.example` and `apps/frontend/.env.example` only as templates.
- Keep secrets in local environment variables, ignored `.env` files, GitHub Actions secrets, or a proper secret manager.
- Do not place private values in `NEXT_PUBLIC_*` variables because they are exposed to the browser bundle.
- Rotate any credential that was written to a temporary file, pasted into a public channel, committed, or exposed in logs.

## Local Development Safety

Before committing or pushing, run a repository hygiene check:

```powershell
git status --ignored
git grep -n "PASSWORD\|SECRET\|TOKEN\|DATABASE_URL\|PRIVATE KEY\|BEGIN\|AWS_SECRET_ACCESS_KEY\|JWT_SECRET\|ACTUATOR_METRICS_TOKEN" -- .
```

Classify findings before staging:

- Variable names and placeholders in examples: acceptable.
- Ignored local `.env` files: acceptable only if not staged.
- Real secrets, dumps, logs, private keys, or tokens in tracked/staged files: critical and must be removed before any push.

## Security Controls Implemented

ApplyFlow currently includes:

- JWT authentication with refresh flow.
- RBAC for user/admin access boundaries.
- Ownership checks for user-scoped resources.
- Rate limiting on sensitive flows.
- Private resume storage and PDF validation.
- Controlled ingestion provider configuration and allowlists.
- Structured operational logging and metrics.
- Error handling configured to avoid stack trace exposure.
- Repository hygiene checks in CI.
- Manual runtime smoke workflow gated by required secrets.
- Dependabot configuration for Maven, npm, and GitHub Actions.

## Known Security Limitations

- Branch protection must be configured manually in GitHub and is not complete just because workflows exist.
- GitHub secret scanning and push protection depend on repository/account settings and availability.
- The current frontend `lint` command is a minimal TypeScript gate; semantic ESLint is not yet reintroduced.
- Runtime smoke requires correctly configured secrets and is manual by design.
- License has not been selected yet, so public reuse terms are undefined.

## Responsible Disclosure

Give the maintainer reasonable time to investigate and remediate confirmed vulnerabilities before public disclosure. Avoid accessing, modifying, exfiltrating, or destroying data that is not yours. Keep proof of concept payloads minimal and synthetic.
