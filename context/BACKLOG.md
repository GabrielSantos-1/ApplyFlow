# BACKLOG.md
> Ordem oficial de construção do sistema. Não é lista solta — é sequência controlada com gates.

---

## Regras de execução

| # | Regra |
|---|---|
| 1 | Trabalhar uma fase por vez |
| 2 | Não pular da ideia para implementação final |
| 3 | Nenhuma fase avança sem critérios de aceite validados |
| 4 | Segurança entra desde a fase 0 |
| 5 | Toda fase concluída gera checkpoint em `PROJECT_STATE.md` |
| 6 | Mudança de escopo exige atualização deste documento |

---

## Fase 0 — Descoberta e enquadramento

### Objetivo
Entender o problema, escopo, perfis e riscos do sistema antes de qualquer decisão técnica.

### Entregas
- [ ] `PROJECT_BRIEF.md` preenchido
- [ ] Dados classificados por sensibilidade
- [ ] Perfis de acesso mapeados
- [ ] Restrições conhecidas documentadas
- [ ] Ameaças iniciais identificadas

### Critério de aceite
- Escopo definido (dentro e fora do MVP)
- Sistema classificado por criticidade
- Perfis de acesso com nível de risco
- Fora de escopo explicitamente declarado

---

## Fase 1 — Arquitetura base

### Objetivo
Definir a fundação estrutural antes de qualquer código definitivo.

### Entregas
- [ ] Árvore de diretórios proposta e aprovada
- [ ] Camadas definidas (domínio, aplicação, infraestrutura, interface)
- [ ] Módulos principais identificados
- [ ] Escolha de stack e dependências mínimas justificada
- [ ] Decisão de autenticação e autorização tomada e registrada
- [ ] `DECISIONS.md` atualizado

### Critério de aceite
- Arquitetura aprovada e documentada
- Fluxo de dependências sem violação
- Nenhum código relevante implementado antes da modelagem
- Decisão de authN/authZ registrada com justificativa

---

## Fase 2 — Contratos e modelo

### Objetivo
Definir a superfície do sistema antes de qualquer lógica.

### Entregas
- [ ] Entidades principais modeladas
- [ ] DTOs de entrada e saída definidos por operação
- [ ] Contratos de API / eventos documentados
- [ ] Regras de validação por campo
- [ ] Matriz de autorização por operação
- [ ] Mapeamento de ameaças por endpoint / caso de uso
- [ ] Superfície de ataque documentada em `API_CONTRACTS.md`

### Critério de aceite
- Toda entrada externa tem contrato com allowlist
- Operações críticas têm ameaça mapeada
- Risco de mass assignment e BOLA tratado no desenho
- `CreateDTO` e `UpdateDTO` separados

---

## Fase 3 — Bootstrap técnico

### Objetivo
Subir a base executável com estrutura correta — sem improviso.

### Entregas
- [ ] Estrutura de diretórios criada conforme `ARCHITECTURE.md`
- [ ] Configuração de linguagem / build / lint / formatter
- [ ] `.env.example` criado e `.env` no `.gitignore`
- [ ] Helpers de resposta padronizada (envelope, erro)
- [ ] Módulo de logging estruturado com correlation ID
- [ ] Base de autenticação funcionando
- [ ] Base de validação de schema implementada
- [ ] Projeto sobe localmente

### Critério de aceite
- [ ] Projeto sobe sem erro
- [ ] Build / typecheck / lint passam
- [ ] Base de auth e validação existem
- [ ] Nenhum segredo no código
- [ ] `.env.example` documentado

---

## Fase 4 — Infraestrutura e persistência

### Objetivo
Implementar persistência, integrações e componentes operacionais.

### Entregas
- [ ] Schema / migrations iniciais
- [ ] Repositórios com interfaces na aplicação
- [ ] Clients externos com timeout e retry
- [ ] Módulo de segredos / config integrado
- [ ] Logs estruturados funcionando
- [ ] Correlation ID propagado nas requisições

### Critério de aceite
- [ ] Persistência funcional com migration versionada
- [ ] Toda integração externa com timeout e erro tratado
- [ ] Logs disponíveis com correlation ID
- [ ] Repositório implementa interface da aplicação

---

## Fase 5 — Casos de uso e regras de negócio

### Objetivo
Implementar o comportamento do sistema com separação correta de camadas.

### Entregas
- [ ] Casos de uso implementados por funcionalidade
- [ ] Políticas / guards de autorização por caso de uso
- [ ] Serviços de domínio quando necessário
- [ ] Auditoria em operações críticas
- [ ] Testes de autorização por papel e ownership

### Critério de aceite
- [ ] Regra crítica não está no controller
- [ ] Autorização testada (acesso permitido + negado)
- [ ] Trilha de auditoria funcionando para operações críticas
- [ ] Testes unitários cobrindo regras de domínio

---

## Fase 6 — Interface e experiência

### Objetivo
Expor o sistema para consumo real.

### Entregas
- [ ] Rotas / controllers implementando contratos
- [ ] Páginas / telas com estados: normal, loading, erro, vazio
- [ ] Feedback de erro consistente sem vazar internos
- [ ] Formulários com validação client-side (UX) + server-side (obrigatório)
- [ ] Acessibilidade mínima: labels, foco, contraste

### Critério de aceite
- [ ] Interface respeita todos os contratos definidos
- [ ] Nenhuma lógica crítica na UI
- [ ] Estado de erro tratado e seguro
- [ ] Validação dupla (client + server) funcionando

---

## Fase 7 — Hardening

### Objetivo
Reduzir a superfície de risco antes de considerar produção.

### Entregas
- [ ] Headers de segurança configurados
- [ ] CORS revisado e mínimo
- [ ] Rate limiting em todas as superfícies expostas
- [ ] Revisão de autenticação e autorização
- [ ] Revisão de dependências vulneráveis (`npm audit`, `snyk`, etc.)
- [ ] Revisão de logs (sem segredo, sem PII exposta)
- [ ] CSRF validado quando aplicável
- [ ] Revisão de uploads e integrações quando existirem
- [ ] Respostas de erro auditadas (sem stack trace, sem dado interno)

### Critério de aceite
- [ ] Gate mínimo de produção em `SECURITY_RULES.md` atendido
- [ ] Vulnerabilidades conhecidas tratadas ou aceitas formalmente em `DECISIONS.md`
- [ ] Headers de segurança validados

---

## Fase 8 — Testes e validação final

### Objetivo
Comprovar funcionamento e controles críticos com evidência.

### Entregas
- [ ] Testes unitários em regras críticas de domínio
- [ ] Testes de integração em fluxos com persistência
- [ ] Testes de autorização (papel correto + papel incorreto + ownership)
- [ ] Testes de fluxos sensíveis (login, recuperação de senha, admin)
- [ ] Evidências registradas

### Critério de aceite
- [ ] Fluxos críticos com cobertura mínima
- [ ] Testes de autorização passando
- [ ] Regressões conhecidas protegidas por teste

---

## Fase 9 — Release readiness

### Objetivo
Verificar se o sistema pode ser liberado em ambiente controlado.

### Entregas
- [ ] Checklist de release executado
- [ ] Configs revisadas por ambiente (dev vs prod)
- [ ] Observabilidade mínima em produção configurada
- [ ] Plano de rollback documentado
- [ ] `PROJECT_STATE.md` com checkpoint final
- [ ] `DECISIONS.md` com decisões desta fase

### Critério de aceite
- [ ] Release documentado
- [ ] Riscos residuais conhecidos e aceitos formalmente
- [ ] Estado do projeto atualizado e coerente com o real

---

## Fases de manutenção (pós-release)

| Fase | Objetivo |
|---|---|
| M1 — Monitoramento | alertas, dashboards, SLAs |
| M2 — Evolução | novas features seguindo o mesmo fluxo de fases |
| M3 — Auditoria periódica | revisão de dependências, segurança, logs |
| M4 — Refatoração controlada | débito técnico registrado e priorizado |

---

## Regras de mudança de escopo

Toda mudança relevante de escopo deve:
- Atualizar `PROJECT_BRIEF.md`
- Registrar decisão em `DECISIONS.md`
- Atualizar `BACKLOG.md`
- Refletir no checkpoint atual em `PROJECT_STATE.md`

---

## Regra final

Backlog não é lista solta. É a ordem oficial de construção do sistema com gates entre cada etapa.
