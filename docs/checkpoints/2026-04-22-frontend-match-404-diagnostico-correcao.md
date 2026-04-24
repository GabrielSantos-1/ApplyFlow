# Checkpoint - Frontend Match 404 e Campos de Vaga

- **Data:** `2026-04-22`
- **Escopo:** diagnostico e correcao cirurgica de integracao frontend-backend para `matches/{vacancyId}` e exibicao de metadados de vaga.

## Evidencia objetiva de causa raiz

1. Endpoint backend confirmado: `GET /api/v1/matches/{vacancyId}`.
2. Contrato backend observado em runtime:
   - quando existe variante de curriculo para a vaga: `200` com `MatchAnalysisResponse`;
   - quando nao existe: `404 NOT_FOUND` com mensagem `Nenhuma variante do curriculo para a vaga informada`.
3. Conclusao: `404` nao era rota errada nem base URL incorreta; era ausencia legitima de prerequisito de dominio para calculo de match.

## Correcao aplicada no frontend

1. `match-adapter` passou a classificar `404` como estado `not_found`, nao como erro fatal.
2. Listagem/ranking/detalhe passaram a exibir estado degradado ("match indisponivel") sem quebrar a tela.
3. Fluxo de retry no detalhe corrigido para distinguir `429`, `404` e erro generico (antes tratava quase tudo como rate limit).

## Seniority e Data de Publicacao

1. `VacancyResponse` atual do backend **nao expoe** `publishedAt`.
2. `seniority` pode vir `null` em payload real de vagas ingeridas.
3. Frontend ajustado para:
   - aceitar `seniority: string | null`;
   - normalizar exibicao para "Nao informado";
   - explicitar no detalhe que data de publicacao esta indisponivel no contrato atual.

## Validacao executada

- `cmd /c npm run build` em `apps/frontend`: sucesso (build + typecheck).
- Chamadas runtime com usuario real para confirmar semantica de `404` e payload de vagas.

## Risco residual

1. Enquanto nao houver endpoint agregador/batch de matches, ainda existe dependencia de chamadas unitarias controladas por adaptador.
2. `publishedAt` continua indisponivel para UX completa ate evolucao do contrato backend.
