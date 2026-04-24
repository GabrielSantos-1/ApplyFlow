# Checkpoint Tecnico - Priorizacao Segura de Vagas
Data: 2026-04-24

## Objetivo do bloco
Transformar a tela `/vagas` em uma interface de priorizacao clara, rastreavel e segura, mantendo o backend como fonte de verdade para score, recomendacao e fluxo de candidatura.

O bloco nao altera backend, DTO publico, endpoints, arquitetura ou dependencias. A mudanca fica restrita ao frontend e reflete dados ja retornados pelo backend.

## Mudancas em `/vagas`
- A listagem passou a priorizar vagas por sinais do backend:
  - `score DESC`;
  - `publishedAt DESC`;
  - `qualityScore DESC` quando o campo estiver disponivel no contrato.
- O card de vaga passou a exibir:
  - score retornado pelo backend;
  - recomendacao traduzida para linguagem de produto;
  - ate 3 `strengths`;
  - ate 3 `gaps`;
  - empresa;
  - titulo;
  - data de publicacao.
- A tela passou a carregar drafts existentes por vaga para evitar duplicidade visual de candidatura.
- A acao `Aplicar` cria draft via endpoint existente `POST /api/v1/applications/drafts/assisted`.
- A acao `Ignorar` e estado local em memoria da tela, sem persistencia em storage.
- Filtros ganharam labels acessiveis.

## Backend como fonte de verdade
O frontend nao calcula score, nao altera recomendacao e nao decide regra de negocio.

Regras preservadas:
- `score` vem de `MatchAnalysisResponse.score`;
- `recommendation` vem de `MatchAnalysisResponse.recommendation`;
- `state` vem de `MatchAnalysisResponse.state`;
- criacao de draft depende da resposta HTTP do backend;
- erro de autorizacao ou ownership nao e mascarado como sucesso.

Qualquer divergencia entre vaga e match e tratada como estado inseguro de exibicao.

## Ordenacao por score / publishedAt / qualityScore
A ordenacao foi centralizada em helper frontend somente com dados recebidos do backend:

```text
score DESC
publishedAt DESC
qualityScore DESC, se disponivel
```

Observacoes:
- `qualityScore` nao foi adicionado ao backend nem ao DTO publico.
- O frontend aceita o campo como opcional para compatibilidade futura.
- Quando ausente, o desempate por qualidade nao inventa valor e usa fallback neutro.

## Mapeamento APPLY / REVIEW / IGNORE
Mapeamento visual:

| Backend | UI |
|---|---|
| `APPLY` | Alta prioridade |
| `REVIEW` | Revisar |
| `IGNORE` | Ignorar |

O mapeamento e apenas apresentacional. O frontend nao recalcula nem reclassifica recomendacao.

## Fallback seguro para divergencia
Foi criado helper de prioridade segura para validar:
- match existe;
- `state === GENERATED`;
- `match.vacancyId === vacancy.id`;
- `vacancy.status === PUBLISHED`;
- `score` e numero finito;
- `recommendation` pertence ao enum esperado.

Quando qualquer uma dessas condicoes falha, o card exibe:

```text
Nao foi possivel avaliar esta vaga com seguranca.
```

O frontend nao tenta corrigir divergencia localmente.

## CTAs seguros
CTAs disponiveis:
- `Aplicar`;
- `Revisar`;
- `Ignorar`;
- `Vaga original`, quando `jobUrl` existir.

Regras aplicadas:
- `Aplicar` so fica habilitado quando a avaliacao e segura.
- `Aplicar` bloqueia submit concorrente.
- sucesso visual so ocorre apos resposta HTTP do backend.
- draft existente marca o card como `Draft criado`.
- `Ignorar` nao persiste dado sensivel e nao usa storage do browser.
- `Revisar` apenas navega para o detalhe da vaga.

## Tratamento de erro HTTP
Mapeamento aplicado:

| HTTP | Mensagem |
|---|---|
| `400` | Acao invalida ou estado inconsistente |
| `401` | Sessao expirada |
| `403` | Acesso negado |
| `404` | Recurso inexistente ou sem permissao de acesso |
| `500` | Erro inesperado |

Proibicoes preservadas:
- sem stack trace;
- sem payload bruto;
- sem mascarar erro como sucesso;
- sem log de token/header/payload sensivel.

## Seguranca frontend aplicada
- Sem `dangerouslySetInnerHTML`.
- Sem renderizacao de HTML bruto de vaga.
- Sem `console.log` de token, headers ou payload sensivel.
- Sem `localStorage` ou `sessionStorage` para dados de decisao ou candidatura.
- Query params nao sao usados como fonte de verdade.
- IDs no client nao sao prova de acesso.
- Frontend reflete resposta do backend e trata divergencia como erro/fallback seguro.
- Dados externos (`title`, `company`, `strengths`, `gaps`) sao renderizados como texto React.

## OWASP mitigado
- A01 Broken Access Control:
  - frontend nao assume autorizacao;
  - backend continua decidindo draft, ownership e sessao.
- A03 Injection / XSS:
  - texto renderizado por React;
  - sem HTML bruto;
  - sem `dangerouslySetInnerHTML`.
- A05 Security Misconfiguration:
  - sem storage inseguro para estado sensivel;
  - erro tecnico nao e exposto ao usuario.
- API1 Broken Object Level Authorization:
  - IDs manipulaveis no client nao sao tratados como permissao.
- API3 Excess Data Exposure:
  - card usa campos minimos necessarios.
- API6 Sensitive Business Flow:
  - frontend nao forca candidatura;
  - acao depende do endpoint existente e da resposta HTTP real.

## Validacao executada
Build frontend:

```text
cmd /c npm run build
```

Resultado:

```text
Compiled successfully
TypeScript OK
Static pages generated
Route /vagas OK
Route /candidaturas OK
```

Servidor local:

```text
http://localhost:3000
```

Verificacoes estaticas:
- busca por `console.` nos arquivos alterados: nenhum resultado;
- busca por `localStorage`/`sessionStorage`: nenhum resultado nos arquivos alterados;
- busca por `dangerouslySetInnerHTML`/`innerHTML`: nenhum resultado nos arquivos alterados.

## Arquivos alterados
- `apps/frontend/src/app/(dashboard)/vagas/page.tsx`
- `apps/frontend/src/components/vacancies/VacancyListCard.tsx`
- `apps/frontend/src/components/matching/RecommendationBadge.tsx`
- `apps/frontend/src/lib/vacancies/prioritization.ts`
- `apps/frontend/src/types/api.ts`

## Riscos residuais
- `qualityScore` ainda nao faz parte do `VacancyResponse` backend; o frontend trata como opcional e nao depende dele para funcionar.
- A acao `Ignorar` e local em memoria; ao recarregar a pagina, a vaga volta a aparecer.
- A criacao de draft assistido depende de existir curriculo base; se nao houver, o frontend mostra erro amigavel.
- A listagem ainda carrega matches progressivamente por vaga; em volumes maiores, o endpoint batch/backend agregador seria melhor.
- Sem validacao browser automatizada neste checkpoint; a validacao executada foi build/typecheck e revisao estatica de seguranca.

## Proximo passo recomendado
Criar um endpoint backend agregador ou batch para `/vagas` que retorne vaga + match ja ordenados pelo servidor, reduzindo chamadas progressivas no frontend e eliminando a necessidade de ordenar apos carregamento parcial de matches.

Como etapa complementar, criar teste de navegador para:
1. login;
2. abrir `/vagas`;
3. validar exibicao de prioridade;
4. criar draft com sucesso;
5. validar tratamento de erro `401/403`.
