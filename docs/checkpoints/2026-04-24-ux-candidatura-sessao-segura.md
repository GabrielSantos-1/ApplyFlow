# Checkpoint Tecnico - UX de Candidatura + Sessao Segura Frontend
Data: 2026-04-24

## 1) Contexto anterior
- Base consolidada no checkpoint de 2026-04-24 (runtime validation + flow consistency).
- Fluxo backend validado ponta a ponta: `vacancy -> match -> draft -> status -> tracking`.
- Ownership no tracking ja validado server-side.
- Transicoes invalidas ja bloqueadas no backend com `400`.
- Nenhum `500` no fluxo principal apos alinhamento de runtime/schema.

Referencia anterior:
- `docs/checkpoints/2026-04-24-runtime-validation-flow-consistency.md`

## 2) Mudancas de UX em /candidaturas e /candidaturas/[id]
- Mapeamento de status tecnico para labels amigaveis:
  - `DRAFT` -> `Preparando candidatura`
  - `READY_FOR_REVIEW` -> `Pronto para revisar`
  - `APPLIED` -> `Candidatura enviada`
  - `SCREENING` -> `Em analise`
  - `CLOSED` -> `Encerrado`
- Timeline visual com destaque de estado atual e historico de eventos.
- Progresso exibido de forma linear e legivel para usuario final.
- Proximo passo recomendado exibido conforme estado atual.
- Status atual mantido como elemento visual (nao acionavel).
- UI passou a exibir apenas a proxima transicao valida como acao.

## 3) Seguranca aplicada no frontend
- Frontend mantido como camada de UX (nao fonte de autorizacao).
- Nenhuma regra de ownership movida para frontend.
- Nenhuma confianca em status vindo de query string.
- Nenhuma confianca em `applicationId` manipulado como prova de acesso.
- Nenhum uso de `dangerouslySetInnerHTML`.
- Renderizacao textual mantida segura para `notes`, `companyName`, `title` e tracking notes.
- Sem armazenamento de token em `localStorage`/`sessionStorage`.
- Sem log de token/cookie/payload sensivel no console.
- Sem fallback fake de tracking quando API falha.

## 4) Tratamento seguro de erros HTTP
- `400`: acao invalida ou estado inconsistente, com mensagem amigavel.
- `401`: sessao expirada, com orientacao de reautenticacao.
- `403`: acesso negado, sem vazamento de detalhe interno.
- `404`: candidatura inexistente ou fora de ownership, sem enumeracao de recurso.
- `500`: erro inesperado com mensagem generica.
- Sem exposicao de stack trace, payload bruto ou detalhes internos.

## 5) Mudanca de sessao: memoria + reidratacao via refresh HttpOnly cookie
- Sessao client-side migrada para memoria (`in-memory`) no frontend.
- Reidratacao no bootstrap via `POST /api/v1/auth/refresh` usando `credentials: include`.
- Ajuste de robustez no client auth:
  - `refresh` e `logout` enviam `{}` quando nao ha refresh token explicito, evitando body vazio.
- Resultado esperado:
  - login cria sessao em memoria;
  - refresh da pagina tenta reidratacao por cookie HttpOnly;
  - sem cookie valido, sessao encerra de forma controlada;
  - logout limpa estado em memoria e encerra sessao no backend.

## 6) Validacoes executadas
- Login funcional (`POST /api/v1/auth/login` -> `200`).
- Chamadas autenticadas funcionais (`/auth/me`, `/vacancies` -> `200` com Bearer valido).
- Refresh sem cookie valido passou a retornar `403` (sem `500`) apos ajuste de body.
- Logout funcional (`POST /api/v1/auth/logout` -> `204`) com limpeza de estado.
- Busca no frontend sem ocorrencias de `localStorage`/`sessionStorage` para token.
- Build frontend executado com sucesso (`npm run build`).

## 7) Limitacao conhecida em HTTP local com cookie Secure
- Em ambiente local HTTP (sem HTTPS), cookie `Secure` pode nao ser enviado pelo navegador.
- Impacto: apos refresh de pagina, sessao pode encerrar mesmo com login previo.
- Status: comportamento esperado para ambiente nao-TLS; nao e regressao de seguranca.

## 8) Arquivos alterados
Frontend (checkpoint desta rodada):
- `apps/frontend/src/components/layout/AuthProvider.tsx`
- `apps/frontend/src/lib/api/auth.ts`
- `apps/frontend/src/lib/auth/session.ts`
- `apps/frontend/src/app/(dashboard)/configuracoes/page.tsx`

Documentacao (esta entrega):
- `docs/checkpoints/2026-04-24-ux-candidatura-sessao-segura.md`
- `context/CHECKPOINT_TECNICO_ATUAL.md`
- `context/PROJECT_STATE.md`
- `context/TASKS.md`

## 9) Riscos residuais
- UX de sessao em localhost HTTP pode parecer mais curta por restricao de cookie `Secure`.
- Dependencia do backend para emissao correta de refresh cookie (flags `HttpOnly`, `Secure`, `SameSite`) permanece critica.
- Sem BFF dedicado, o frontend ainda depende da disciplina de `credentials: include` e tratamento correto de erros.

## 10) Proximo passo recomendado
1. Validar fluxo de sessao em ambiente HTTPS real (staging) para confirmar persistencia via cookie `Secure`.
2. Registrar smoke test de autenticacao frontend cobrindo login, refresh, chamadas autenticadas e logout.
3. Opcional de medio prazo: avaliar BFF enxuto para consolidar politica de sessao e reduzir acoplamento de cliente HTTP.
