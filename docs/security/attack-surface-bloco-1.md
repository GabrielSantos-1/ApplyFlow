# Attack Surface - Bloco 3 (Hardening)

## Mapeamento por modulo/endpoints

### AUTH (`/api/v1/auth/*`)
| Risco | Vetor | Impacto | Controle | Prioridade |
|---|---|---|---|---|
| Brute force / credential stuffing | Ataques repetidos em login | Tomada de conta | Rate limit agressivo por politica (`auth-login`) + erro generico | Alta |
| Replay de refresh token | Reuso de token revogado | Sequestro de sessao | Rotacao + revogacao persistida + validacao de hash + bloqueio em replay | Alta |
| Enumeracao de usuario | Diferenca de resposta por usuario | Reconhecimento de contas | Mensagem uniforme em falha (`Credenciais invalidas`) | Alta |

### VACANCIES (`/api/v1/vacancies/*`)
| Risco | Vetor | Impacto | Controle | Prioridade |
|---|---|---|---|---|
| Abuso de listagem | Crawling massivo | Exaustao de recurso | Rate limit moderado (`vacancies-read`) + paginacao obrigatoria | Media |
| Abuso de filtros | Filtro/sort malicioso | Degradacao/perf | Allowlist de sort/filtro + limites de pagina | Media |

### RESUMES (`/api/v1/resumes/*`)
| Risco | Vetor | Impacto | Controle | Prioridade |
|---|---|---|---|---|
| BOLA/IDOR | Acesso por ID de outro usuario | Vazamento de PII | Ownership por `userId` no use case + `404` quando nao pertence | Alta |
| Texto malicioso | Payload com XSS em metadados/labels | XSS armazenado | Sanitizacao Unicode + remocao de controle + escape HTML + limites de tamanho | Alta |

### APPLICATIONS (`/api/v1/applications/*`)
| Risco | Vetor | Impacto | Controle | Prioridade |
|---|---|---|---|---|
| BOLA/IDOR | Leitura/edicao de draft alheio | Fraude e vazamento | Ownership por recurso em todos os handlers | Alta |
| XSS armazenado | `messageDraft` e `notes` maliciosos | Execucao no frontend | Sanitizacao defensiva + limites de payload | Alta |
| Mudanca indevida de historico | Status arbitrario | Integridade do pipeline | Transicoes por enum com matriz permitida | Alta |

### MATCHES (`/api/v1/matches/*`)
| Risco | Vetor | Impacto | Controle | Prioridade |
|---|---|---|---|---|
| Exposicao cruzada de resultado | Consulta de vaga sem contexto proprio | Vazamento de analise | Match calculado no contexto do usuario autenticado e ownership indireto por resume/variant | Alta |
| Abuso computacional | Chamadas repetidas de score | Exaustao de recurso | Rate limit e persistencia de resultado para auditoria | Media |

## OWASP Top 10 / API Top 10 cobertos
- A01 Broken Access Control / API1 BOLA: ownership por recurso + RBAC.
- A03 Injection / API8 Security Misconfiguration: validacao/sanitizacao e CSP.
- A07 Identification and Authentication Failures / API2 Broken Auth: JWT + refresh rotativo revogavel.
- A05 Security Misconfiguration: headers centralizados e defaults seguros.
- API4 Unrestricted Resource Consumption: rate limiting por politica.
- API10 Unsafe Consumption of APIs: contratos e validacoes estritas de entrada.

## Riscos remanescentes aceitos temporariamente
1. Quando Redis indisponivel com fallback habilitado, limite passa a local por instancia.
2. CSP precisa ajuste fino conforme frontend evoluir e integrar novos recursos externos.

## Atualizacao Bloco 4 - Risco operacional e observabilidade

### Exposicao de endpoints operacionais
| Endpoint | Risco | Controle |
|---|---|---|
| `/actuator/health` | Enumeracao de estado basico | Apenas status minimo, sem detalhes sensiveis |
| `/actuator/metrics` | Exposicao indevida de telemetria | Restrito a `ADMIN` |
| `/actuator/prometheus` | Vazamento de sinais internos | Restrito a `ADMIN` + segregacao de acesso por ambiente |

### Riscos monitorados por metrica/alerta
| Risco | Sinal operacional | Prioridade |
|---|---|---|
| Credential stuffing / brute force | crescimento de `auth_login failed`, `401`, `429` | Alta |
| Quebra de authorization/ownership | aumento de `403` por endpoint | Alta |
| Rate limit indisponivel | `applyflow_rate_limit_total{outcome="unavailable"}` | Alta |
| Redis degradado com fallback | `applyflow_rate_limit_fallback_total` | Alta |
| Regressao de estabilidade | aumento de `5xx` e latencia p95 | Alta |
