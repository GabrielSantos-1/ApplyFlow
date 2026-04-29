# Checkpoint Tecnico - CSRF Security Review

## 1. Objetivo

Corrigir ou justificar tecnicamente o alerta CodeQL `java/spring-disabled-csrf-protection` em `SecurityConfig`, preservando JWT stateless e sem alterar contratos publicos.

## 2. Diagnostico

- `POST /api/v1/auth/login` emite `refresh_token` via cookie HttpOnly, Secure, SameSite Strict e path `/api/v1/auth`.
- `POST /api/v1/auth/refresh` aceita refresh token por body ou pelo cookie `refresh_token`.
- `POST /api/v1/auth/logout` aceita refresh token por body ou pelo cookie `refresh_token`.
- Como refresh/logout aceitam cookie, existe superficie CSRF real nesses endpoints quando chamados por navegador.
- Login nao depende de cookie previo e nao deve exigir protecao CSRF para autenticacao inicial.

## 3. Decisao tecnica

Foi adotada protecao minima por header customizado para os endpoints cookie-backed:

```text
X-ApplyFlow-CSRF: 1
```

A protecao se aplica somente quando:

- metodo e `POST`;
- path e `/api/v1/auth/refresh` ou `/api/v1/auth/logout`;
- existe cookie `refresh_token` nao vazio.

APIs bearer/JWT continuam stateless. DTOs e endpoints publicos nao foram alterados.

## 4. Alteracoes aplicadas

- `SecurityConfig` deixou de usar `csrf(AbstractHttpConfigurer::disable)`.
- `RefreshTokenCsrfProtectionFilter` foi criado para bloquear refresh/logout com cookie sem header customizado.
- CORS passou a permitir `X-ApplyFlow-CSRF`.
- Frontend passou a enviar `X-ApplyFlow-CSRF: 1` no client HTTP.
- Testes de integracao cobrem refresh/logout com e sem header e login sem header.

## 5. Seguranca

- Refresh token continua em cookie HttpOnly.
- O valor do refresh token nao volta no body de resposta.
- Nenhum token ou secret foi logado.
- A resposta de bloqueio usa `403` com mensagem generica.
- O header customizado impede submissao por formularios HTML simples e exige requisicao com header controlado pelo client.

## 6. Validacoes locais

### Backend tests

```text
.\mvnw.cmd -B test -DskipITs
Tests run: 81, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

### Frontend build

```text
npm run build
Compiled successfully
```

## 7. Arquivos alterados

- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/SecurityConfig.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/SecurityBeansConfig.java`
- `apps/backend/src/main/java/com/applyflow/jobcopilot/shared/infrastructure/security/RefreshTokenCsrfProtectionFilter.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/security/SecurityAuthorizationIntegrationTest.java`
- `apps/frontend/src/lib/api/client.ts`
- `SECURITY.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`
- `context/PROJECT_STATE.md`
- `context/TASKS.md`
- `context/DECISIONS.md`

## 8. Riscos remanescentes

- CodeQL remoto ainda precisa ser reexecutado apos push/PR para confirmar que o alerta foi encerrado.
- Spring Security CSRF token repository nao foi adotado porque o frontend/API permanecem stateless; a protecao efetiva para a superficie cookie-backed e o header customizado.

## 9. Recomendacao final

Abrir PR e validar CodeQL remoto. Se CodeQL ainda apontar risco por configuracao de CSRF ignorada globalmente, manter a protecao customizada e adicionar justificativa de falso positivo com referencia aos testes de refresh/logout.
