# API_CONTRACTS.md
> Contrato vem antes da implementação. Nenhum endpoint é implementado sem DTO, autorização e ameaças mapeadas.

---

## Regras centrais

| # | Regra |
|---|---|
| 1 | Contrato definido antes de qualquer implementação |
| 2 | DTO de entrada e DTO de saída sempre explícitos |
| 3 | Nunca expor entidade interna diretamente por acidente |
| 4 | Toda operação declara autenticação e autorização exigidas |
| 5 | Toda operação mapeia ameaças relevantes |
| 6 | Toda resposta segue o envelope padrão |
| 7 | Campos aceitos são allowlist — nunca objeto livre |

---

## Envelope padrão de resposta

### Sucesso

```json
{
  "success": true,
  "data": {},
  "meta": {
    "correlationId": "req_xxx"
  }
}
```

### Sucesso paginado

```json
{
  "success": true,
  "data": [],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 140,
    "hasNext": true,
    "correlationId": "req_xxx"
  }
}
```

### Erro

```json
{
  "success": false,
  "error": {
    "code": "BAD_REQUEST",
    "message": "Mensagem segura para o cliente.",
    "correlationId": "req_xxx"
  }
}
```

### Regras do envelope

- `message` deve ser segura — sem stack trace, sem dado interno
- `code` deve ser estável e útil para frontend e observabilidade
- `correlationId` deve existir em toda resposta quando rastreável
- Detalhes internos ficam nos logs, nunca na resposta

---

## Códigos de erro padronizados

| Código | HTTP | Quando usar |
|---|---|---|
| `BAD_REQUEST` | 400 | input inválido, schema incorreto |
| `UNAUTHORIZED` | 401 | não autenticado |
| `FORBIDDEN` | 403 | autenticado mas sem permissão |
| `NOT_FOUND` | 404 | recurso não encontrado |
| `CONFLICT` | 409 | conflito de estado (duplicata, etc.) |
| `UNPROCESSABLE_ENTITY` | 422 | entidade válida mas regra de negócio violada |
| `TOO_MANY_REQUESTS` | 429 | rate limit atingido |
| `DEPENDENCY_FAILURE` | 502/503 | integração externa indisponível |
| `INTERNAL_ERROR` | 500 | erro inesperado não tratado |

---

## Template oficial de contrato de endpoint

```markdown
### Operação: <NOME_DA_OPERAÇÃO>

- **Método:** `POST | GET | PUT | PATCH | DELETE`
- **Rota:** `/api/v1/<recurso>`
- **Objetivo:** `<o que a operação faz>`
- **Autenticação:** `<nenhuma | sessão | JWT | API key | interna>`
- **Autorização:** `<papel, atributo, ownership, tenant, escopo>`
- **Rate limit:** `<sim — N req/min por IP | por usuário | não>`
- **Idempotência:** `<sim — estratégia: chave de idempotência / hash | não>`
- **Auditoria:** `<sim | não>`

#### DTO de entrada
| Campo | Tipo | Obrigatório | Validações |
|---|---|---|---|
| `campo` | `string` | sim | min: 1, max: 100 |

```json
{
  "campo": "string"
}
```

#### Validações
- `campo` — obrigatório, string, 1–100 caracteres
- `outroId` — obrigatório, UUID v4 válido
- Campos não listados acima são rejeitados

#### DTO de saída
```json
{
  "id": "uuid",
  "campo": "string",
  "createdAt": "ISO 8601"
}
```

#### Erros esperados
| Código | HTTP | Condição |
|---|---|---|
| `BAD_REQUEST` | 400 | schema inválido |
| `UNAUTHORIZED` | 401 | token ausente ou inválido |
| `FORBIDDEN` | 403 | sem permissão para o recurso |
| `NOT_FOUND` | 404 | recurso não encontrado |
| `CONFLICT` | 409 | recurso já existe |

#### Ameaças relevantes
| Ameaça | Controle implementado |
|---|---|
| BOLA / IDOR | verificar `ownerId === req.user.id` antes de retornar |
| Mass assignment | allowlist de campos no DTO de entrada |
| Brute force | rate limit por IP e por usuário |
| Enumeração | IDs não sequenciais (UUID), resposta neutra em NOT_FOUND |

#### Observações
- `<qualquer detalhe relevante sobre comportamento, integração ou limitação>`
```

---

## Superfície de ataque — tabela por operação

Preencher antes de implementar qualquer operação nova:

| Operação | Método | Entrada | Recurso acessado | Risco principal | Controles obrigatórios |
|---|---|---|---|---|---|
| criar usuário | POST | body | users | mass assignment | DTO estrito, role, audit |
| obter recurso | GET | path param | resource by id | BOLA / IDOR | authz por ownership/tenant |
| login | POST | body | auth | brute force, enumeração | rate limit, resposta neutra |
| atualizar perfil | PATCH | body | user | mass assignment, IDOR | allowlist de campos, ownership |
| deletar recurso | DELETE | path param | resource | IDOR, BFLA | ownership + role check, soft delete + audit |
| listar recursos | GET | query params | collection | enumeração, over-fetching | filtros por ownership, paginação com limite |
| upload de arquivo | POST | multipart | storage | path traversal, MIME falso | validar MIME, extensão, tamanho, renomear |
| operação admin | POST/PATCH | body | config/user | BFLA, escalada vertical | role admin, auditoria, MFA quando aplicável |

---

## Padrões de paginação

### Query params

| Param | Tipo | Descrição | Padrão | Máximo |
|---|---|---|---|---|
| `page` | integer | número da página (1-indexed) | 1 | — |
| `pageSize` | integer | itens por página | 20 | 100 |
| `sort` | string (allowlist) | campo de ordenação | `createdAt` | — |
| `direction` | `asc \| desc` | direção | `desc` | — |
| `cursor` | string | cursor opaco para cursor-based pagination | — | — |

### Regras
- `pageSize` sempre limitado no servidor — nunca confiar no valor do cliente
- `sort` sempre validado contra allowlist explícita de campos
- Paginação sem índice adequado é risco de performance; documentar
- Cursor-based pagination preferida para grandes datasets

---

## Padrões de filtros

- Todo filtro é explícito e nomeado no contrato
- Nenhum filtro dinâmico por campo arbitrário
- Campos de ordenação validados contra allowlist
- Filtros que tocam dados de outros usuários respeitam autorização
- Filtros em campos não indexados devem ser avaliados antes de habilitar

---

## Separação de DTOs

### Regra
`CreateDTO`, `UpdateDTO` e `ResponseDTO` são **sempre** tipos separados.

| DTO | Finalidade |
|---|---|
| `CreateDTO` | campos permitidos na criação — sem campos administrativos |
| `UpdateDTO` | campos permitidos na atualização — geralmente subconjunto menor |
| `ResponseDTO` | campos que o cliente pode ver — sem campos internos sensíveis |

### Campos que exigem controle especial (nunca aceitar via input direto)

```
role, isAdmin, isActive, status, tenantId, ownerId, deletedAt,
createdAt, updatedAt, passwordHash, apiKeyHash, verifiedAt
```

---

## Contratos de autenticação

Documentar obrigatoriamente:

| Campo | Detalhe |
|---|---|
| Payload de login | `{ email, password }` ou equivalente |
| Resposta de sessão / token | access token + refresh token (nunca expor hash) |
| Expiração do access token | `<ex: 15min>` |
| Expiração do refresh token | `<ex: 7 dias>` |
| Rotação | refresh token rotacionado a cada uso |
| Revogação | `<blocklist de jti / invalidação de refresh token>` |
| Tratamento de falha | resposta neutra; não revelar se email existe |
| Rate limit | `<ex: 5 tentativas por 15min por IP>` |

---

## Contratos de integração externa

Para cada integração documentar:

| Campo | Detalhe |
|---|---|
| Endpoint externo | `<URL>` |
| Credencial usada | `<tipo e onde armazenada>` |
| Timeout | `<ex: connect 2s, read 5s>` |
| Retry | `<ex: 3 tentativas com backoff exponencial>` |
| Fallback | `<ex: retornar cached / degradar gracefully>` |
| Campos mínimos necessários | `<campos do payload que importam>` |
| Validação da resposta | `<schema de validação>` |
| Tratamento de indisponibilidade | `<circuit breaker / fila / erro controlado>` |

---

## Idempotência

Operações sujeitas a retry devem declarar estratégia:

| Estratégia | Quando usar |
|---|---|
| `Idempotency-Key` header | criação de recurso, pagamento, envio de mensagem |
| Hash do payload | deduplicação por conteúdo |
| Unique constraint no banco | fallback de deduplicação |
| Janela temporal | `<ex: mesma operação no mesmo segundo ignorada>` |

---

## Auditoria por operação

Operações administrativas ou sensíveis devem declarar:

| Campo | Detalhe |
|---|---|
| O que será auditado | ação, recurso, before/after |
| Quem pode executar | papel exigido |
| Campos que entram no log | `<listar>` |
| Campos mascarados | senha, token, CPF, cartão |

---

## Checklist por endpoint antes de implementar

- [ ] DTO de entrada definido com allowlist
- [ ] DTO de saída definido sem campos internos sensíveis
- [ ] Autenticação exigida declarada
- [ ] Autorização (papel, ownership, tenant) declarada
- [ ] Ameaças mapeadas
- [ ] Rate limit definido (ou ausência justificada)
- [ ] Idempotência considerada
- [ ] Auditoria definida quando operação é crítica
- [ ] Erros esperados listados
- [ ] Envelope de resposta respeitado

---

## Regra final

Nenhum endpoint está pronto para implementação sem:
- DTO de entrada
- DTO de saída
- Regra de autorização
- Ameaças relevantes mapeadas
- Formato de erro
- Estratégia de rate limiting quando exposto
