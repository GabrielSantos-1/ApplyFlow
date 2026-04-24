# Checkpoint — 2026-04-23 — Assisted Application Flow (Candidatura Assistida)

## Diagnóstico
- O fluxo de candidatura assistida já existia, mas com dois gaps de produto:
1. rastreio de progresso não exposto ao usuário;
2. frontend permitia transições de status inválidas (ex.: `DRAFT -> APPLIED`), gerando erro operacional.

## Correções aplicadas
1. **Timeline de candidatura no backend (ownership-safe)**
   - Novo DTO: `ApplicationTrackingEventResponse`.
   - Novo endpoint: `GET /api/v1/applications/{id}/tracking`.
   - Regra de segurança: validação de ownership via `findByIdAndUserId` antes de retornar timeline.
   - Sem exposição cross-user.

2. **Persistência e contrato de tracking**
   - Repositório de tracking com leitura ordenada:
     - `findByApplicationDraftIdOrderByCreatedAtAsc`.
   - `ApplicationUseCase` expandido com `listTracking(UUID id)`.

3. **Frontend com fluxo de status válido**
   - Tela de vaga:
     - `DRAFT -> READY_FOR_REVIEW -> APPLIED` (sem pular etapa).
     - botão de `WITHDRAWN` mantido como saída manual.
     - timeline renderizada no card de candidatura.
   - Tela de candidaturas:
     - botões restritos ao estado atual + transições permitidas.
     - remoção de tentativa de transição inválida por UX.

4. **Auditoria e segurança**
   - Sem automação de envio externo.
   - Sem scraping.
   - Sem IA.
   - Sem alteração do core de matching.
   - Sem quebra de isolamento por `userId`.

## Evidências de validação
- Backend: `.\mvnw.cmd -B test`
  - `Tests run: 71, Failures: 0, Errors: 0, Skipped: 2`
  - `BUILD SUCCESS`
- Frontend:
  - `cmd /c npx tsc --noEmit` sem erros
  - `cmd /c npm run build` com sucesso

## Riscos residuais
- A timeline exibe histórico textual (`notes`) técnico/curto; UX pode evoluir com labels mais amigáveis no futuro.
- Não houve inclusão de lembretes/agenda (fora de escopo deste bloco).
