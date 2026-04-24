# Authorization Matrix - Bloco 3

## Papel e ownership
| Recurso | USER | ADMIN | Ownership |
|---|---|---|---|
| `/api/v1/resumes/*` | permitido apenas recursos proprios | permitido (mantido por regra atual de rota) | obrigatorio |
| `/api/v1/applications/*` | permitido apenas recursos proprios | permitido (mantido por regra atual de rota) | obrigatorio |
| `/api/v1/matches/*` | permitido no proprio contexto de resume/variant | permitido (rota), calculo segue contexto autenticado | obrigatorio indireto |
| `/api/v1/vacancies/*` | leitura permitida | leitura permitida | nao aplicavel para vaga publica |
| `/api/v1/auth/me/logout` | usuario autenticado | usuario autenticado | n/a |
| `/actuator/health` e `/actuator/info` | publico | publico | n/a |
| `/actuator/metrics*` e `/actuator/prometheus` | negado | permitido | n/a |

## Observacoes
1. Ownership e aplicado no application service (nao no controller).
2. Sem ABAC complexo no Bloco 3; apenas RBAC + ownership por recurso.
3. Em recurso alheio, retorno esperado no contrato atual e `404` para evitar enumeracao.
