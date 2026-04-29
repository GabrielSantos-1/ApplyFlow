# Checkpoint Tecnico - CSRF Security Review
Data: 2026-04-29

## Versao atual do sistema
O ApplyFlow permanece no estado consolidado com backend Spring Boot, frontend Next.js, JWT stateless, refresh token em cookie HttpOnly, CI/CD, repository hygiene, dashboard de vagas e dashboard admin operacional.

Nesta data foi executado bloco de revisao CSRF para o alerta CodeQL `java/spring-disabled-csrf-protection`, sem novas features, sem mudanca de DTO, sem regra de negocio nova e sem alteracao arquitetural.

Referencia oficial desta versao:
- `docs/checkpoints/2026-04-29-csrf-security-review.md`

## Fluxo principal consolidado
```text
vacancy -> match -> draft -> status -> tracking
```

## Estado consolidado
- Backend Java/Spring Boot e PostgreSQL seguem como fonte de verdade.
- Frontend Next.js atua como apresentacao e nao decide regra de negocio.
- Matching segue deterministico.
- Ownership permanece obrigatorio.
- Rate limit segue aplicado a fluxos sensiveis.
- Painel admin de ingestao esta operacional.
- `/vagas` prioriza oportunidades sem recalcular score no frontend.
- Smoke runtime, CI/CD minimo e repository hygiene seguem versionados.

## Seguranca aplicada neste bloco
- Confirmado que login emite `refresh_token` por cookie HttpOnly, Secure, SameSite Strict.
- Confirmado que refresh/logout aceitam `refresh_token` por cookie, criando superficie CSRF real.
- `SecurityConfig` nao usa mais `csrf(AbstractHttpConfigurer::disable)`.
- `RefreshTokenCsrfProtectionFilter` bloqueia refresh/logout cookie-backed sem `X-ApplyFlow-CSRF: 1`.
- CORS permite `X-ApplyFlow-CSRF`.
- Frontend envia `X-ApplyFlow-CSRF: 1` sem persistir tokens.
- Login permanece sem exigencia de CSRF para nao quebrar autenticacao inicial.

## Evidencias recentes
- Backend tests (2026-04-29):
  - `.\mvnw.cmd -B test -DskipITs`;
  - 81 testes, 0 falhas, 0 erros, 2 skipped.
- Frontend build (2026-04-29):
  - `npm run build` em `apps/frontend`;
  - build passou.

## Limitacoes conhecidas
- CodeQL remoto ainda precisa ser reexecutado apos push/PR para confirmar que o alerta foi encerrado.
- Spring Security session-based CSRF token repository nao foi adotado porque a API permanece JWT stateless; a protecao efetiva para refresh/logout cookie-backed e o header customizado coberto por testes.

## Proxima retomada segura
1. Validar CodeQL remoto apos push/PR.
2. Se CodeQL ainda sinalizar a configuracao por ignorar CSRF globalmente, manter protecao customizada e registrar justificativa tecnica com referencia aos testes.
3. Executar validacao visual autenticada das telas `/vagas` e `/admin` em desktop/mobile.
