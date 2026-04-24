# Project Governance Kit
> Kit de governança para desenvolvimento seguro, estruturado e com continuidade.

---

## O que é este kit

Conjunto de documentos de governança reutilizáveis para qualquer projeto de software, com foco em:

- Arquitetura em camadas com responsabilidades claras
- Segurança desde o início (não como etapa final)
- Contratos antes da implementação
- Checkpoints obrigatórios para continuidade
- Compatibilidade com desenvolvimento assistido por IA

---

## Estrutura do kit

```
context/
├── CONTEXT_MAP.md          → mapa de governança e ordem de leitura
├── PROJECT_BRIEF.md        → escopo, perfis, ameaças, critérios de sucesso
├── ARCHITECTURE.md         → camadas, árvore de diretórios, fluxo de dependências
├── SECURITY_RULES.md       → regras de segurança, OWASP, gate de produção
├── API_CONTRACTS.md        → contratos de endpoints, DTOs, ameaças por operação
├── CODING_STANDARDS.md     → padrões de código, nomenclatura, erros, testes
├── BACKLOG.md              → fases de entrega com critérios de aceite
├── TASKS.md                → controle de tarefas ativas por sessão
├── PROJECT_STATE.md        → checkpoint técnico do projeto
├── DECISIONS.md            → registro de decisões arquiteturais (ADR)
├── BOOTSTRAP_BLUEPRINT.md  → procedimento de inicialização segura
├── PROMPTING_RULES.md      → protocolo de uso de IA como implementador
└── UI_GUIDELINES.md        → padrões de interface, acessibilidade, segurança na UI
```

---

## Como usar este kit

### Para um projeto novo

1. **Copiar** a pasta `context/` para a raiz do projeto
2. **Preencher** `PROJECT_BRIEF.md` — escopo, perfis, ameaças
3. **Adaptar** `ARCHITECTURE.md` — árvore de diretórios real do projeto
4. **Adaptar** `SECURITY_RULES.md` — marcar o que se aplica ao contexto
5. **Preencher** `API_CONTRACTS.md` — um contrato por operação crítica
6. **Planejar** `BACKLOG.md` — ajustar fases ao projeto real
7. **Ativar** `TASKS.md` — tarefas da primeira sessão
8. Seguir `BOOTSTRAP_BLUEPRINT.md` para iniciar o desenvolvimento

### Para um projeto existente

1. Copiar os documentos para `context/` ou `docs/`
2. Preencher `PROJECT_STATE.md` com o estado atual real
3. Mapear o que já foi feito em `BACKLOG.md` (marcar fases concluídas)
4. Preencher `DECISIONS.md` com decisões arquiteturais já tomadas
5. Usar `CONTEXT_MAP.md` como guia daqui para frente

### Para uso com IA (Claude, Codex, GPT, etc.)

1. Fornecer os documentos relevantes no início da sessão
2. Pedir que a IA leia `CONTEXT_MAP.md` primeiro
3. Seguir os templates em `PROMPTING_RULES.md`
4. Exigir checkpoint ao final de cada sessão

---

## Hierarquia de prioridade

```
SECURITY_RULES.md          ← prioridade máxima
ARCHITECTURE.md            ← prioridade máxima
API_CONTRACTS.md           ← estrutural
CODING_STANDARDS.md        ← estrutural
BACKLOG.md + TASKS.md      ← operacional
PROJECT_STATE.md           ← operacional
DECISIONS.md               ← histórico
PROMPTING_RULES.md         ← processo
UI_GUIDELINES.md           ← visual
```

Em conflito: segurança vence funcionalidade. Arquitetura vence conveniência.

---

## Fluxo mínimo por sessão de desenvolvimento

```
Início da sessão:
  1. Ler PROJECT_STATE.md → entender onde parou
  2. Ler TASKS.md → pegar próxima tarefa
  3. Ler documentos relevantes para a tarefa

Durante a sessão:
  4. Seguir ARCHITECTURE.md e SECURITY_RULES.md
  5. Contratos antes de código
  6. Implementação incremental

Fim da sessão:
  7. Atualizar TASKS.md
  8. Atualizar PROJECT_STATE.md
  9. Registrar nova decisão em DECISIONS.md se houver
```

---

## Gate mínimo de produção

Checklist rápido — ver `SECURITY_RULES.md` para versão completa:

- [ ] Autenticação definida e testada
- [ ] Autorização testada por operação crítica
- [ ] Validação server-side em todas as entradas
- [ ] Segredos fora do código
- [ ] Logs com correlation ID
- [ ] Rate limiting em superfícies expostas
- [ ] Respostas de erro sem stack trace
- [ ] Dependências auditadas
- [ ] Headers de segurança configurados
- [ ] Checkpoint técnico atualizado

---

## Adaptação por tipo de projeto

| Tipo | Documentos obrigatórios | Documentos opcionais |
|---|---|---|
| API / backend | todos exceto UI_GUIDELINES | UI_GUIDELINES |
| SaaS / full-stack | todos | — |
| App interno / CRUD | todos exceto UI_GUIDELINES (opcional) | — |
| Worker / integração | ARCHITECTURE, SECURITY_RULES, API_CONTRACTS | UI_GUIDELINES |
| Frontend puro | ARCHITECTURE, SECURITY_RULES, CODING_STANDARDS, UI_GUIDELINES | API_CONTRACTS |

---

## Convenção de IDs

| Documento | Prefixo | Exemplo |
|---|---|---|
| `DECISIONS.md` | `DECISION-XXX` | `DECISION-001` |
| `TASKS.md` | `T-XXX` | `T-001` |
| `TASKS.md` (bloqueios) | `B-XXX` | `B-001` |
| `PROJECT_STATE.md` (pendências) | `P-XXX` | `P-001` |
| `PROJECT_BRIEF.md` (requisitos funcionais) | `RF-XX` | `RF-01` |
| `PROJECT_BRIEF.md` (requisitos não funcionais) | `RNF-XX` | `RNF-01` |

---

## Versão do kit

- Versão: `1.0.0`
- Última atualização: `2025`
- Compatível com: qualquer stack, qualquer linguagem, projetos solo ou em equipe
