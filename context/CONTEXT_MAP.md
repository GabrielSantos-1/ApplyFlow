# CONTEXT_MAP.md
> Mapa de governança e ordem oficial de leitura para qualquer projeto.

---

## Para que serve este arquivo

Este documento define **a ordem de leitura, a hierarquia de decisões e o fluxo oficial de trabalho** para qualquer projeto conduzido com ou sem apoio de IA.

Ele previne:
- perda de contexto entre sessões
- implementação fora de ordem
- quebra silenciosa de arquitetura
- regressão de segurança
- continuação improvisada

---

## Ordem obrigatória de leitura

Antes de qualquer análise, modelagem ou código, ler nesta ordem:

| # | Arquivo | Quando é obrigatório |
|---|---|---|
| 1 | `PROJECT_BRIEF.md` | sempre |
| 2 | `ARCHITECTURE.md` | sempre |
| 3 | `SECURITY_RULES.md` | sempre |
| 4 | `CODING_STANDARDS.md` | sempre |
| 5 | `API_CONTRACTS.md` | quando houver endpoints ou eventos |
| 6 | `BACKLOG.md` | antes de iniciar qualquer fase |
| 7 | `TASKS.md` | antes de qualquer sessão de trabalho |
| 8 | `PROJECT_STATE.md` | antes de qualquer sessão de trabalho |
| 9 | `DECISIONS.md` | sempre que houver dúvida de decisão passada |
| 10 | `PROMPTING_RULES.md` | quando usar IA como implementador |
| 11 | `BOOTSTRAP_BLUEPRINT.md` | ao iniciar projeto do zero |
| 12 | `UI_GUIDELINES.md` | quando houver interface visual |

---

## Hierarquia de governança

### Prioridade 1 — Não negociável
- `SECURITY_RULES.md`
- `ARCHITECTURE.md`

### Prioridade 2 — Estrutural
- `API_CONTRACTS.md`
- `CODING_STANDARDS.md`

### Prioridade 3 — Operacional
- `BACKLOG.md`
- `TASKS.md`
- `PROJECT_STATE.md`

### Prioridade 4 — Histórico de decisões
- `DECISIONS.md`

### Prioridade 5 — Processo e interação
- `PROMPTING_RULES.md`
- `BOOTSTRAP_BLUEPRINT.md`

### Prioridade 6 — Visual
- `UI_GUIDELINES.md`

---

## Regras de conflito

Quando dois documentos conflitarem, aplicar esta ordem:

1. Segurança vence funcionalidade
2. Arquitetura vence conveniência
3. Contrato vence implementação
4. Padrão de código vence preferência pessoal
5. Checkpoint registrado vence memória de conversa
6. Decisão em `DECISIONS.md` vence improviso

---

## Fluxo oficial de desenvolvimento

Código **nunca** vem antes de estrutura, contrato e segurança.

```
1.  Entender objetivo do sistema         → PROJECT_BRIEF.md
2.  Definir tipo de aplicação            → PROJECT_BRIEF.md
3.  Projetar arquitetura em camadas      → ARCHITECTURE.md
4.  Propor árvore de diretórios          → ARCHITECTURE.md
5.  Definir entidades, DTOs, contratos   → API_CONTRACTS.md
6.  Mapear superfície de ataque          → SECURITY_RULES.md + API_CONTRACTS.md
7.  Definir autenticação e autorização   → SECURITY_RULES.md + DECISIONS.md
8.  Escolher dependências mínimas        → DECISIONS.md
9.  Implementar infraestrutura base      → BOOTSTRAP_BLUEPRINT.md
10. Implementar domínio                  → CODING_STANDARDS.md
11. Implementar aplicação                → CODING_STANDARDS.md
12. Implementar interface                → UI_GUIDELINES.md
13. Testar fluxos críticos               → TASKS.md
14. Executar checklist de hardening      → SECURITY_RULES.md
15. Atualizar checkpoint final           → PROJECT_STATE.md + TASKS.md
```

---

## Responsabilidade de cada documento

| Documento | Governa |
|---|---|
| `PROJECT_BRIEF.md` | problema, escopo, perfis, restrições, critérios de sucesso |
| `ARCHITECTURE.md` | camadas, módulos, boundaries, árvore de diretórios |
| `SECURITY_RULES.md` | autenticação, autorização, validação, hardening, gate de produção |
| `CODING_STANDARDS.md` | nomenclatura, estrutura de código, erros, dependências, testes |
| `API_CONTRACTS.md` | contratos de entrada/saída, paginação, erros, ameaças por operação |
| `BACKLOG.md` | fases de entrega, critérios de aceite, gates entre etapas |
| `TASKS.md` | trabalho ativo, status, bloqueios, evidências |
| `PROJECT_STATE.md` | checkpoint técnico, estado real, próxima ação |
| `DECISIONS.md` | decisões arquiteturais, trade-offs, justificativas, histórico |
| `PROMPTING_RULES.md` | protocolo de uso de IA como implementador |
| `BOOTSTRAP_BLUEPRINT.md` | procedimento de inicialização segura do projeto |
| `UI_GUIDELINES.md` | padrões de UX, acessibilidade, segurança na interface |

---

## Regra de encerramento de sessão

Nenhuma sessão é considerada concluída sem atualizar:

- [ ] `TASKS.md`
- [ ] `PROJECT_STATE.md`
- [ ] `DECISIONS.md` (se houver nova decisão)
- [ ] `BACKLOG.md` (se houver mudança de fase ou escopo)

---

## Regra final

Em caso de dúvida, o agente ou desenvolvedor deve:

1. Parar
2. Voltar aos documentos de prioridade máxima
3. Escolher a opção mais segura, simples e auditável

**Improviso não é permitido quando a decisão impacta segurança, arquitetura ou continuidade.**
