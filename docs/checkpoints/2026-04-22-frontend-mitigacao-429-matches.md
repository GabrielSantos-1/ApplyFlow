# Checkpoint - 2026-04-22 - Mitigacao 429 no Frontend (matches unitarios)

## Status
`concluido com mitigacao aplicada e build valido`

## 1) Diagnostico executado
Causa observada no frontend:
- `dashboard`, `vagas` e `ranking` disparavam `Promise.all` com chamada unitária para cada `vacancyId`.
- Filtros em `vagas` (query digitada) geravam novo ciclo completo sem debounce.
- Falha de uma chamada (`429`) quebrava carregamento da tela inteira por tratamento agregado de erro.
- `detalhe` carregava `vacancy + match + applications` em `Promise.all`, tornando o match um ponto único de falha.

## 2) Correcao aplicada (sem alterar backend)
1. Criado adaptador central de matching:
- `src/lib/api/match-adapter.ts`
- recursos:
  - `concurrency limit` (workers)
  - cache em memoria por escopo de usuario
  - deduplicacao de requests inflight
  - retry leve com backoff para `429`
  - estados de carga por item (`loading/success/rate_limited/error`)

2. Telas migradas para consumo via adaptador:
- `dashboard`: carregamento progressivo dos matches, sem `Promise.all` massivo.
- `vagas`: debounce de busca (`350ms`) + carregamento progressivo + estado parcial por card.
- `ranking`: lista renderiza com vagas e score entrando progressivamente.
- `detalhe`: vaga/applications carregam primeiro; match passa a ser carregamento independente com retry manual.

3. UX sob `429`:
- `429` deixa de derrubar tela inteira.
- cards/linhas exibem estado parcial (`429 temporario`, `Carregando...`, `Falha`).
- detalhe exibe aviso específico e botao `Tentar novamente`.

## 3) Preparacao para endpoint agregador
- UI parou de chamar `matchingApi` diretamente nas telas.
- ponto de troca futura centralizado no `match-adapter`.
- quando existir endpoint batch/agregador no backend, troca ocorre no adaptador sem reescrita estrutural das páginas.

## 4) Validacao
- Comando executado:
  - `npm run build`
- Resultado:
  - build/typecheck `OK` (Next 16.2.4).

## 5) Riscos remanescentes
1. Ainda ha N chamadas unitarias para N vagas (agora controladas), pois backend ainda nao possui endpoint agregador.
2. Sem telemetria frontend dedicada, a taxa real de `429` por tela depende de observacao manual de rede.
3. Em bases muito grandes, UX ainda pode degradar sem paginação/estratégia de viewport match-first.

## 6) Proximo passo recomendado
1. Implementar endpoint agregador/batch de matches no backend.
2. Migrar `match-adapter` para esse endpoint mantendo contrato da UI.
3. Adicionar telemetria client-side para `429` por tela e tempo de convergencia do ranking.
