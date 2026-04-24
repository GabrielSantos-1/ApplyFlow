# API v1 - Contratos Atuais (Bloco 3)

## Endpoints mantidos (sem quebra de contrato)

### Auth
- `POST /api/v1/auth/login`
  - Request: `LoginRequest`
  - Response: `AuthTokensResponse` (refresh em cookie HttpOnly; campo `refreshToken` mascarado no payload)
- `POST /api/v1/auth/refresh`
  - Request: `RefreshTokenRequest` (body opcional, fallback cookie)
  - Response: `AuthTokensResponse`
- `POST /api/v1/auth/logout`
  - Request: `LogoutRequest` (body opcional, fallback cookie)
  - Response: `204 No Content`
- `GET /api/v1/auth/me`
  - Response: `CurrentUserResponse`

### Vacancies
- `GET /api/v1/vacancies`
  - Query com validacao + allowlist
  - Response: `PageResponse<VacancyResponse>`
- `GET /api/v1/vacancies/{id}`
  - Response: `VacancyResponse`

### Matches
- `GET /api/v1/matches/{vacancyId}`
  - Ownership por usuario autenticado
  - Persistencia de `MatchResult`
  - Response: `MatchAnalysisResponse`

### Resumes
- `GET /api/v1/resumes`
- `POST /api/v1/resumes`
- `GET /api/v1/resumes/{id}`
- `POST /api/v1/resumes/{id}/variants`

### Applications
- `GET /api/v1/applications`
- `POST /api/v1/applications/drafts`
- `PATCH /api/v1/applications/{id}/status`
- `GET /api/v1/applications/{id}`

## Controles transversais (Bloco 3)
- JWT access token curto + refresh token rotativo revogavel
- RBAC (`USER`, `ADMIN`) + ownership por recurso
- Rate limiting por politica com Redis (principal) e fallback explicito
- Headers de seguranca centralizados: `CSP`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `X-Frame-Options`, `HSTS` condicional por ambiente
- Erro padronizado: `ApiErrorResponse` com `errorCode` estavel e `correlationId`

## Headers de rate limit
Quando rota possui politica ativa:
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`
- `X-RateLimit-Policy`
- `X-RateLimit-Mode` (`redis` ou `in-memory-fallback`)
- `Retry-After` quando bloqueado (`429`)

## Codigos de resposta de seguranca relevantes
- `401 UNAUTHORIZED` para token ausente/invalido
- `403 FORBIDDEN` para role insuficiente ou refresh invalido/revogado
- `404 NOT_FOUND` para ownership negado em recursos com ocultacao
- `422 VALIDATION_ERROR` para input invalido
- `429 RATE_LIMIT_EXCEEDED` para abuso de consumo
- `503 RATE_LIMIT_UNAVAILABLE` quando fallback desabilitado e backend de rate-limit indisponivel
