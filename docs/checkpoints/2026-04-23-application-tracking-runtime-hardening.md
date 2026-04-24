# Checkpoint — 2026-04-23 — Application Tracking Runtime Hardening

## Diagnóstico
- Sintoma em runtime: `GET /api/v1/applications/{applicationId}/tracking` retornando `500`.
- Causa técnica mais provável e reproduzível em código:
  - parse rígido de stage em `listTracking` usando `TrackingStage.valueOf(item.getStage())`;
  - qualquer valor legado/inválido no banco (`APPLIED`, `DRAFT`, strings inesperadas, `null`) disparava `IllegalArgumentException` e quebrava o endpoint.

## Correção aplicada (fail-safe, sem mudar contrato)
1. Hardening no parse de stage em `StubApplicationService`:
   - mapeamento backward-compatible para valores legados:
     - `DRAFT -> CREATED`
     - `READY_FOR_REVIEW -> SCREENING`
     - `APPLIED -> SUBMITTED`
     - `OFFER -> FINAL`
     - `REJECTED/WITHDRAWN -> CLOSED`
   - valor desconhecido: fallback para `CLOSED` + log estruturado de warning.
   - `createdAt` nulo: fallback defensivo para `OffsetDateTime.now()`.

2. Testes adicionados/atualizados:
   - `ApplicationServiceTest#listTrackingMustHandleLegacyOrUnknownStageWithout500`.

## Arquivos alterados
- `apps/backend/src/main/java/com/applyflow/jobcopilot/applications/application/service/StubApplicationService.java`
- `apps/backend/src/test/java/com/applyflow/jobcopilot/applications/application/service/ApplicationServiceTest.java`

## Evidências de validação
- `.\mvnw.cmd -B test` (backend): `BUILD SUCCESS`
- Resultado: `Tests run: 72, Failures: 0, Errors: 0, Skipped: 2`

## Segurança e escopo
- Sem alterar regra de negócio de candidatura.
- Sem alterar arquitetura.
- Sem dependências novas.
- Sem exposição de dados cross-user.
- Endpoint permanece ownership-safe; hardening apenas evita erro 500 por dado legado.
