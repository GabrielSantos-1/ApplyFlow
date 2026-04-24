# Checkpoint Tecnico Consolidado Geral

- **Projeto:** `ApplyFlow / Job Copilot`
- **Data:** `2026-04-22`
- **Escopo:** consolidacao completa do estado tecnico (Blocos 1 a 8.1 + Produto/UX frontend).

## 1) Implementado Ate Agora (visao executiva)

1. Arquitetura base modular consolidada (`domain/application/infrastructure/interfaces`) no backend.
2. Seguranca estrutural implementada:
   - JWT + refresh + RBAC/ownership;
   - rate limit com Redis e fallback explicito controlado;
   - headers de seguranca e sanitizacao defensiva;
   - actuator endurecido com token tecnico.
3. Observabilidade operacional:
   - Prometheus/Alertmanager integrados;
   - metricas de API/rate limit/IA;
   - logs estruturados com correlation ID.
4. Ingestao real de vagas:
   - conector Remotive;
   - normalizacao;
   - deduplicacao;
   - persistencia rastreavel;
   - scheduler e lock operacional.
5. Matching deterministico:
   - score auditavel e explicavel;
   - breakdown por criterio;
   - recomendacao deterministica.
6. Camada IA controlada:
   - 3 fluxos (`enrichment`, `cv-improvement`, `application-draft`);
   - prompt fechado, validacao de schema, fallback seguro;
   - protecoes contra uso inseguro e consumo nao controlado.
7. Validacao real do provider (8.1):
   - provider ativo em runtime real;
   - `fallbackUsed=false` nos fluxos saudaveis;
   - capture de usage/tokens/custo estimado;
   - rate limit IA validado.
8. Produto frontend (SaaS MVP):
   - login + guard de rotas;
   - dashboard/listagem/ranking/detalhe;
   - integracao IA e candidaturas;
   - mitigacao de rajada e resiliencia a `429`;
   - tratamento funcional de `404` de match.
9. Produto/UX de curriculos e candidatura assistida:
   - pagina `/curriculos`;
   - criacao de curriculo base (metadata);
   - criacao de variante no detalhe da vaga;
   - diferenciacao clara entre `sem curriculo`, `sem variante`, `sem match`.

## 2) Validado Com Evidencia

1. Backend:
   - suite automatizada executada com sucesso no ciclo 8.1 (`60` testes, `0` falhas).
2. IA em runtime:
   - chamadas reais ao provider com sucesso e fallback controlado em cenario de falha.
3. Frontend:
   - `npm run build`/typecheck validos apos os ultimos ajustes.
4. Contratos de match em runtime:
   - `404` por ausencia de curriculo;
   - `404` por ausencia de variante;
   - `200` apos criar variante adequada.

## 3) Parcialmente Validado / Nao Validado

1. Campanha longa de custo/latencia IA (p95/p99) ainda pendente.
2. Cenario dedicado de schema semanticamente invalido do provider ainda pendente.
3. Jornada de candidatura ainda depende de lacunas de contrato backend (ver riscos).

## 4) O Que Esta Sendo Feito Agora

1. Consolidacao de contexto tecnico e rastreabilidade dos blocos.
2. Fechamento de UX orientada a estado real (sem mascarar limitacoes de contrato).
3. Preparacao para proximo ciclo de backend focado em reduzir friccao da candidatura.

## 5) Riscos Atuais Para Continuar Sem Endurecimento

1. Sem endpoint de variantes por curriculo, o frontend precisa inferir variantes via drafts (visao incompleta).
2. Sem `sourceUrl` no contrato de vagas, nao ha CTA real para abrir vaga original.
3. Sem endpoint simplificado de intencao/draft, usuario ainda depende de `resumeVariantId`.
4. Sessao frontend em `sessionStorage` mantem risco residual de XSS (ja documentado).

## 6) Proximo Passo Exato Recomendado

1. Implementar no backend:
   - `GET /api/v1/resumes/{id}/variants` (listagem de variantes por curriculo).
2. Evoluir contrato de vagas:
   - incluir `sourceUrl` e `publishedAt` em `VacancyResponse`.
3. Criar endpoint simplificado de intencao de candidatura:
   - fluxo que reduza dependencia manual de `resumeVariantId` no primeiro clique do usuario.
4. Revalidar UX ponta a ponta apos itens 1-3:
   - login -> curriculo -> vaga -> match -> IA -> draft -> acao manual.
