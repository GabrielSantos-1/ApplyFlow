# Repository Structure

## Root

The repository root contains project-level documentation, repository hygiene configuration, and public-facing governance files.

Key files:

- `README.md`: public project overview and operating guide.
- `SECURITY.md`: vulnerability reporting and security policy.
- `CONTRIBUTING.md`: contribution workflow and pre-PR checks.
- `.gitignore`: blocks local secrets, build artifacts, logs, dumps, private storage, and temporary files.
- `.env.example`: pointer to app-specific environment examples.

## Backend

`apps/backend` contains the Spring Boot backend. It is the source of truth for authorization, ownership, matching, deduplication, application state, resume storage, migrations, and operational controls.

Important areas:

- `src/main/java`: modular backend code organized by business capability and technical infrastructure.
- `src/main/resources/db/migration`: Flyway migrations.
- `src/main/resources/application*.yml`: environment-aware configuration.
- `infra/staging`: Docker Compose staging runtime assets.
- `ops/smoke`: runtime smoke scripts for the main flow.
- `ops/prometheus`: operational monitoring support.
- `ops/loadtest`: controlled load/operational validation scripts.
- `.env.example`: backend configuration template with no real secrets.

## Frontend

`apps/frontend` contains the Next.js App Router frontend. It is responsible for presentation, API consumption, user interaction, client-side feature flags, and safe UI state.

Important areas:

- `src/app`: application routes and pages.
- `src/components`: reusable UI components.
- `src/hooks`: frontend hooks.
- `src/lib`: API clients, config, session helpers, sanitization, and presentation helpers.
- `src/types`: TypeScript contracts used by the UI.
- `.env.example`: public frontend configuration template. Values prefixed with `NEXT_PUBLIC_` are browser-visible and must never contain secrets.

## Context

`context` stores the technical continuity layer for the project:

- current state and checkpoints;
- active tasks and pending work;
- architecture/security/process guidance;
- decision records.

These files are part of the project audit trail and should be updated after significant technical blocks.

## Docs

`docs` stores deeper technical documentation:

- `docs/architecture`: architecture and repository structure.
- `docs/checkpoints`: historical checkpoints and continuation records.
- `docs/contracts`: API contracts.
- `docs/observability`: metrics, alerts, and failure modes.
- `docs/operations`: runtime, staging, ingestion, smoke, and repository protection procedures.
- `docs/security`: attack surface, authorization, and rate-limit documentation.

## CI/CD

`.github` contains repository automation:

- `.github/workflows/ci.yml`: backend tests, frontend quality gate, and repository hygiene checks.
- `.github/workflows/runtime-smoke.yml`: manual staging runtime smoke workflow.
- `.github/dependabot.yml`: dependency update PR configuration for Maven, npm, and GitHub Actions.

Branch protection is not configured by files alone. It must be enabled manually in GitHub after checks have run at least once.

## Operational Scripts

Operational scripts are versioned when they are deterministic, auditable, and do not contain secrets.

Main scripts:

- `apps/backend/ops/smoke/run-runtime-smoke.ps1`: validates the authenticated main HTTP flow.
- `apps/backend/ops/smoke/run-staging-runtime-smoke.ps1`: starts staging runtime and runs smoke validation with credentials supplied through environment variables.

Scripts must fail clearly when required variables are missing and must not print tokens, passwords, or private payloads.

## Ignored / Non-versioned Files

The following must not be committed:

- real `.env` or `.env.*` files;
- `node_modules`;
- `.next`;
- `target`;
- `.m2` and `.mvn-cache`;
- logs and `logs/`;
- dumps and database exports;
- private keys, certificates, keystores, and signing material;
- temporary token files;
- private storage such as uploaded resumes;
- local Docker overrides.

`.env.example` files are allowed because they contain names, defaults, or empty placeholders only. Flyway migration SQL files under `apps/backend/src/main/resources/db/migration` are allowed.

## Security Notes

- Backend ownership and RBAC are mandatory security boundaries.
- The frontend must not be trusted for authorization, scoring, or state transitions.
- Public documentation must not include real tokens, credentials, private URLs, candidate data, or resume contents.
- Runtime smoke credentials must be synthetic and provided through environment variables or GitHub Actions secrets.
- Repository hygiene and GitHub push protection are complementary; neither replaces a manual security review before public release.
