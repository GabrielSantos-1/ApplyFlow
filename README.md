# ApplyFlow

![CI](https://github.com/GabrielSantos-1/ApplyFlow/actions/workflows/ci.yml/badge.svg)

## Visão Geral

O **ApplyFlow** é uma plataforma orientada a segurança para otimizar o fluxo de candidatura a vagas, combinando ingestão de dados, normalização, matching determinístico e acompanhamento estruturado de candidaturas.

O objetivo não é automatizar decisões críticas com IA, mas reduzir fricção operacional no processo de candidatura, mantendo controle, auditabilidade e consistência.

## Problema

Candidatar-se a vagas manualmente costuma ser:

- repetitivo;
- desorganizado;
- difícil de rastrear;
- inconsistente entre plataformas.

Além disso:

- dados de vagas são heterogêneos;
- nem sempre há padronização de senioridade, localização e modalidade;
- candidatos perdem tempo com vagas pouco aderentes.

## Solução

O ApplyFlow estrutura o processo em um pipeline auditável:

```text
Ingestão de Vagas
→ Normalização e Qualidade de Dados
→ Matching Determinístico
→ Geração de Draft de Candidatura
→ Controle de Status
→ Tracking de Progresso
```

O sistema evita automação cega, scraping agressivo e decisões opacas.

## Fluxo Principal

```text
vacancy → match → draft → status → tracking
```

Etapas principais:

- **Vacancy**: vaga ingerida de fontes externas.
- **Match**: cálculo determinístico de compatibilidade.
- **Draft**: criação de candidatura assistida.
- **Status**: transição controlada do fluxo de candidatura.
- **Tracking**: timeline auditável da candidatura.

## Arquitetura

O projeto segue separação clara de responsabilidades:

```text
apps/
  backend/      API Spring Boot
  frontend/     Interface Next.js
context/        Governança, decisões e estado do projeto
docs/           Checkpoints, operações e arquitetura
.github/        CI/CD, workflows e Dependabot
```

Princípios adotados:

- arquitetura em camadas;
- contratos antes da implementação;
- segurança como requisito estrutural;
- separação entre autenticação e autorização;
- logs estruturados com correlation ID;
- validação server-side em fluxos críticos.

## Stack Tecnológica

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JPA / Hibernate
- PostgreSQL
- Flyway
- Redis

### Frontend

- Next.js
- React
- TypeScript

### Infraestrutura e Operação

- Docker / Docker Compose
- GitHub Actions
- Dependabot
- Smoke tests operacionais

## Segurança

O ApplyFlow foi projetado com mentalidade **security-first**.

Controles aplicados:

- autenticação com JWT;
- refresh token;
- RBAC;
- ownership checks para mitigar IDOR/BOLA;
- validação server-side;
- rate limiting em endpoints críticos;
- upload de PDF com validação;
- logs sem dados sensíveis;
- secrets fora do código;
- hygiene gate no CI para bloquear arquivos sensíveis;
- documentação de segurança em `SECURITY.md`.

## Validação em Runtime

O fluxo principal é validado por smoke test automatizado:

```text
vacancy → match → draft → status → tracking
```

Scripts principais:

```text
apps/backend/ops/smoke/run-runtime-smoke.ps1
apps/backend/ops/smoke/run-staging-runtime-smoke.ps1
```

Resultado esperado:

```text
SMOKE_RUNTIME_RESULT=PASS
```

## CI/CD

O pipeline do GitHub Actions valida:

- `backend-test`: testes automatizados do backend;
- `frontend-quality`: lint mínimo, typecheck e build do frontend;
- `repository-hygiene`: bloqueio de arquivos sensíveis e padrões críticos.

O CI roda em:

- push na `main`;
- pull requests para `main`.

## Estrutura do Repositório

```text
.github/        Workflows CI/CD e Dependabot
apps/backend/   API Spring Boot
apps/frontend/  Frontend Next.js
context/        Estado técnico, decisões e tarefas
docs/           Checkpoints, operações e documentação técnica
```

Arquivos que não devem ser versionados:

```text
.env
.env.local
node_modules/
.next/
target/
logs/
dumps/
tmp_*
*token*.txt
```

## Como Rodar

### Backend

```powershell
cd apps/backend
.\mvnw.cmd -B test -DskipITs
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd apps/frontend
npm install
npm run dev
```

## Variáveis de Ambiente

Use os arquivos `.env.example` como referência.

Arquivos reais de ambiente não devem ser versionados.

Exemplos de variáveis:

```env
JWT_SECRET_BASE64=
ACTUATOR_METRICS_TOKEN=
DATABASE_URL=
SMOKE_ADMIN_EMAIL=
SMOKE_ADMIN_PASSWORD=
```

## Comandos Úteis

### Backend

```powershell
cd apps/backend
.\mvnw.cmd -B test -DskipITs
```

### Frontend

```bash
cd apps/frontend
npm run lint
npm run typecheck
npm run build
```

### Smoke Runtime

```powershell
powershell -ExecutionPolicy Bypass -File apps/backend/ops/smoke/run-runtime-smoke.ps1
```

## Estado Atual

Estado consolidado:

- backend com testes automatizados;
- frontend com build validado;
- CI/CD configurado;
- smoke runtime validado localmente/staging;
- documentação de segurança criada;
- repository hygiene ativo no CI.

Limitações conhecidas:

- lint semântico com ESLint ainda não foi reintroduzido;
- smoke remoto depende de secrets configurados no GitHub Actions;
- branch protection depende de configuração manual no GitHub.

## Roadmap

Próximas melhorias planejadas:

- reintroduzir ESLint semântico;
- melhorar experiência visual do frontend;
- adicionar screenshots e diagrama visual ao README;
- evoluir filtros e UX do matching;
- ampliar integrações de vagas de forma controlada.

## Aviso de Segurança

Nunca versionar:

- `.env` real;
- tokens;
- credenciais;
- dumps;
- logs sensíveis;
- currículos reais;
- dados pessoais.

Vulnerabilidades devem ser reportadas conforme as orientações em `SECURITY.md`.

## Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE`.