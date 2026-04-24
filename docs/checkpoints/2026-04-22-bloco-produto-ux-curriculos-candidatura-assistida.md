# Checkpoint - Bloco Produto/UX: Curriculos + Candidatura Assistida

- **Data:** `2026-04-22`
- **Escopo:** frontend Next.js conectado ao backend real, sem novos endpoints, para fechar fluxo do usuario entre curriculo, match e preparacao de candidatura.

## Gate e contratos validados

Endpoints reais confirmados antes da implementacao:

- `GET /api/v1/resumes`
- `POST /api/v1/resumes`
- `GET /api/v1/resumes/{id}`
- `POST /api/v1/resumes/{id}/variants`
- `GET /api/v1/applications`
- `POST /api/v1/applications/drafts`
- `PATCH /api/v1/applications/{id}/status`
- `GET /api/v1/matches/{vacancyId}`
- `POST /api/v1/ai/matches/{vacancyId}/enrichment`
- `POST /api/v1/ai/matches/{vacancyId}/cv-improvement`
- `POST /api/v1/ai/matches/{vacancyId}/application-draft`

Limitacoes contratuais confirmadas:

1. Sem endpoint de listagem de variantes por curriculo.
2. `VacancyResponse` nao expoe URL original da vaga.
3. Criacao de draft exige `resumeVariantId`.

## Implementacoes realizadas

1. Nova area `/curriculos`:
   - estado vazio e CTA claro;
   - listagem de curriculos do usuario;
   - criacao de curriculo base por metadata;
   - exibicao de variantes conhecidas via drafts (com limitacao explicitada).

2. Estados de match refinados no frontend:
   - `resume_missing` para ausencia de curriculo base;
   - `variant_missing` para ausencia de variante na vaga;
   - `not_found` para indisponibilidade funcional generica.

3. Detalhe da vaga com CTA contextual:
   - link para `/curriculos` quando faltar curriculo;
   - criacao de variante por vaga quando faltar variante;
   - criacao de draft quando `resumeVariantId` conhecido (variante criada no detalhe);
   - orientacao clara para aplicacao manual sem autopilot externo.

4. Integracao assistida com IA:
   - `AiActionPanel` agora pode devolver rascunho gerado para reutilizacao na abertura de draft.

## Evidencias objetivas

### Build/typecheck frontend
- `cmd /c npm run build` em `apps/frontend`: **sucesso**.

### Runtime backend (amostra real)
- Login usuario operacional e chamadas de validacao executadas:
  - criacao de curriculo: `POST /api/v1/resumes` -> sucesso.
  - criacao de variante: `POST /api/v1/resumes/{id}/variants` -> sucesso.
  - match apos variante: `GET /api/v1/matches/{vacancyId}` -> `200`.
- Validacao de estado `sem curriculo`:
  - usuario admin sem curriculo -> `GET /api/v1/matches/{vacancyId}` -> `404` com mensagem `Nenhum curriculo encontrado para o usuario`.
- Validacao de estado `sem variante`:
  - usuario com curriculo em vaga sem variante -> `404` com mensagem `Nenhuma variante do curriculo para a vaga informada`.

## Riscos remanescentes

1. Sem endpoint de variantes por curriculo, UX de inventario de variantes permanece parcial.
2. Sem URL original no contrato de vagas, CTA de abertura da fonte fica apenas informativo.
3. Fluxo de draft ainda depende de `resumeVariantId`, com friccao para casos onde variante exista mas nao esteja conhecida pelo frontend.
