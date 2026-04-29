# ApplyFlow

## Visão Geral

O **ApplyFlow** é uma plataforma orientada a segurança para otimizar o fluxo de candidatura a vagas, combinando ingestão de dados, normalização, matching determinístico e acompanhamento estruturado de candidaturas.

O objetivo não é automatizar decisões críticas com IA, mas **reduzir fricção operacional** no processo de candidatura, mantendo controle, auditabilidade e consistência.

---

## Problema

Candidatar-se a vagas manualmente é:

- repetitivo
- desorganizado
- difícil de rastrear
- inconsistente entre plataformas

Além disso:

- dados de vagas são heterogêneos
- não existe padronização de senioridade/localização
- candidatos perdem tempo com vagas irrelevantes

---

## Solução

O ApplyFlow resolve isso com um pipeline estruturado:

```text
Ingestão de Vagas
→ Normalização e Qualidade de Dados
→ Matching Determinístico
→ Geração de Draft de Candidatura
→ Controle de Status
→ Tracking de Progresso

Sem automação cega.
Sem scraping agressivo.
Sem decisões opacas.

Fluxo Principal
vacancy → match → draft → status → tracking
Etapas
Vacancy: vaga ingerida de múltiplas fontes
Match: cálculo determinístico de compatibilidade
Draft: criação de candidatura assistida
Status: transição controlada (DRAFT → APPLIED)
Tracking: timeline auditável da candidatura
Arquitetura

O projeto segue separação clara de responsabilidades:

apps/
  backend/     → Spring Boot (API, domínio, segurança)
  frontend/    → Next.js (interface e consumo de API)

context/       → governança, decisões e estado do projeto
docs/          → checkpoints, operações, arquitetura

.github/       → CI/CD, workflows, dependabot

Princípios:

arquitetura em camadas
contratos antes da implementação
segurança como requisito estrutural
separação entre autenticação e autorização
logs estruturados com correlation ID
Stack Tecnológica
Backend
Java 21
Spring Boot 3
Spring Security
JPA / Hibernate
PostgreSQL
Flyway (migrations)
Redis (rate limit / suporte)
Frontend
Next.js (App Router)
TypeScript
React
Infra / DevOps
Docker / Docker Compose
GitHub Actions (CI/CD)
Dependabot
Segurança

O ApplyFlow foi projetado com mentalidade security-first:

JWT com controle de claims
separação entre autenticação e autorização
RBAC (controle por papel)
proteção contra IDOR/BOLA via ownership
validação server-side obrigatória
rate limiting em endpoints críticos
logs estruturados (sem dados sensíveis)
upload de PDF validado
secrets fora do código
hygiene de repositório (CI bloqueando vazamentos)
Validação em Runtime

O fluxo principal é validado via smoke test automatizado:

vacancy → match → draft → status → tracking

Script:

apps/backend/ops/smoke/run-runtime-smoke.ps1

Modo staging (Docker):

apps/backend/ops/smoke/run-staging-runtime-smoke.ps1

Resultado esperado:

SMOKE_RUNTIME_RESULT=PASS
CI/CD

Pipeline com GitHub Actions:

backend-test → testes do backend
frontend-quality → lint + typecheck + build
repository-hygiene → bloqueio de arquivos sensíveis

Execução automática em:

push na main
pull requests
Estrutura do Repositório
.github/        → workflows CI/CD
apps/backend/   → API Spring Boot
apps/frontend/  → Next.js frontend
context/        → governança e estado do projeto
docs/           → checkpoints e documentação

Não versionado:

.env
node_modules
.next
target
logs
dumps
tokens temporários
Como Rodar
Backend
cd apps/backend
.\mvnw.cmd -B test -DskipITs
.\mvnw.cmd spring-boot:run
Frontend
cd apps/frontend
npm install
npm run dev
Variáveis de Ambiente

Utilizar .env.example como referência.

Nunca versionar:

.env
.env.local

Principais variáveis:

JWT_SECRET_BASE64=
ACTUATOR_METRICS_TOKEN=
DATABASE_URL=
SMOKE_ADMIN_EMAIL=
SMOKE_ADMIN_PASSWORD=
Comandos Úteis
# Backend
mvn test

# Frontend
npm run build
npm run typecheck
npm run lint

# Smoke runtime
run-runtime-smoke.ps1
Estado Atual
Backend: estável
Frontend: funcional
CI/CD: ativo
Smoke runtime: validado
Segurança: aplicada

Limitações atuais:

lint semântico (ESLint) ainda não reintroduzido
branch protection depende de configuração manual
smoke remoto depende de secrets no GitHub
Roadmap

Próximos passos:

reforço de lint semântico
melhoria de UX no frontend
evolução de matching
possíveis integrações adicionais de vagas
Aviso de Segurança
Nunca commitar .env real
Nunca expor tokens ou credenciais
Não utilizar dados reais em ambientes de teste
Reportar vulnerabilidades conforme SECURITY.md
Licença

Ainda não definida.