# Checkpoint - Bloco Backend+Produto

- **Data:** `2026-04-22`
- **Escopo:** curriculo PDF + `jobUrl` de vaga + candidatura assistida real sem automacao externa.

## Gate inicial (estado real)

1. `resumes` tinha apenas metadata (sem upload binario).
2. `vacancies` persistia `source_url/published_at`, mas DTO REST nao expunha esses campos.
3. `applications` exigia `resumeVariantId` e havia lacuna de validacao de ownership no draft tradicional.
4. Frontend ja possuia `/curriculos` e detalhe assistido, mas sem upload PDF real e sem CTA funcional de vaga original.

## Implementacoes realizadas

### Backend

1. **Upload PDF real**
   - Endpoint: `POST /api/v1/resumes` (`multipart/form-data`).
   - Regras: assinatura `%PDF-`, `contentType=application/pdf`, limite maximo configuravel.
   - Persistencia: metadados completos no `resumes` via migration `V4`.
   - Storage: privado em disco (`resumes.storage.base-dir`) com checksum SHA-256.
   - Base resume: novo upload passa a marcar `is_base=true` e desmarca anteriores do mesmo usuario.

2. **Contrato de vaga**
   - `VacancyResponse` agora expoe `jobUrl` e `publishedAt`.

3. **Fluxo assistido de candidatura**
   - Endpoint novo: `POST /api/v1/applications/drafts/assisted`.
   - Resolve curriculo base/mais recente e variante da vaga de forma controlada.
   - Cria variante automaticamente quando necessario (sem autopilot externo).
   - Mantem rastreabilidade de draft/status.

4. **Seguranca endurecida**
   - `createDraft` tradicional agora valida ownership de variante e consistencia com vaga.
   - Rate limit aplicado tambem para upload de curriculo e draft assistido.
   - Nenhuma URL publica de arquivo foi exposta.

### Frontend

1. `/curriculos`
   - Upload real de PDF com feedback de erro/sucesso.
   - Listagem com metadados (`base`, tipo, tamanho, horario).
2. Detalhe da vaga
   - CTA "Abrir vaga original" quando `jobUrl` existe.
   - Draft assistido integrado (`createDraftAssisted`).
   - Mantem estados funcionais de match (`sem curriculo`, `sem variante`, etc.).
3. API client
   - `apiRequest` passou a suportar `FormData`.
   - `resumesApi.uploadPdf` e `applicationsApi.createDraftAssisted`.

## Evidencias objetivas

1. **Backend tests**
   - `.\mvnw.cmd -B test` executado com sucesso.
   - Resultado: `62` testes, `0` falhas, `0` erros, `2` skipped.
2. **Frontend build/typecheck**
   - `npm run build` executado com sucesso.
3. **Runtime**
   - Tentativa de validacao manual de upload PDF no runtime local retornou `500` em instancia ja em execucao (ambiente legado sem reinicio pos-migration).
   - Conclusao: precisa reinicio com migrations novas aplicadas para prova runtime completa nesse ambiente.

## Riscos remanescentes

1. Falta validacao runtime completa do upload em instancia reiniciada no ambiente manual atual.
2. Nao existe endpoint de listagem de variantes por curriculo (frontend ainda infere por drafts).
3. Parte das vagas pode nao conter `jobUrl` na origem de ingestao.
