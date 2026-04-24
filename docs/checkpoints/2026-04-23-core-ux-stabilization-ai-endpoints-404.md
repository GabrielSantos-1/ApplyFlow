# Checkpoint — 2026-04-23 — Core UX Stabilization (AI 404)

## Diagnostico
- Sintoma: botoes de IA visiveis no detalhe da vaga com `POST /api/v1/ai/*` retornando `404`.
- Causa raiz: frontend exibia `AiActionPanel` sem gate de capacidade, enquanto backend opera com `ai.enabled=false` por padrao.

## Correcoes aplicadas
1. Gate de feature no frontend:
   - Novo arquivo `apps/frontend/src/lib/config/features.ts` com flag publica:
     - `NEXT_PUBLIC_AI_ACTIONS_ENABLED`
     - default efetivo: `false`.
2. Renderizacao condicional do painel de IA:
   - `apps/frontend/src/app/(dashboard)/vagas/[id]/page.tsx`
   - `AiActionPanel` so aparece quando `featureFlags.aiActionsEnabled === true`.
3. Fail-safe de UX no painel de IA:
   - `apps/frontend/src/components/ai/AiActionPanel.tsx`
   - Tratamento de `ApiError` 404 com mensagem explicita:
     - `Acoes de IA indisponiveis neste ambiente.`

## Evidencias de validacao
- `cmd /c npx tsc --noEmit` (apps/frontend): sem erros.
- `cmd /c npm run build` (apps/frontend): sucesso.

## Seguranca e regressao
- Sem mudanca de arquitetura.
- Sem nova dependencia.
- Sem alteracao de regra de negocio.
- Reducao de superficie de erro em runtime:
  - evita chamada para endpoint inexistente por padrao.
  - elimina fluxo quebrado de CTA visivel sem backend habilitado.

## Risco residual
- Se um ambiente habilitar `NEXT_PUBLIC_AI_ACTIONS_ENABLED=true` sem backend IA disponivel, o painel volta a aparecer.
- Mitigacao aplicada: mensagem clara em 404 no proprio painel.
