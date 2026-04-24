# Checkpoint - Matching V1 Runtime Hardening (Fail-Safe + Null-Safety)

**Data:** 2026-04-23  
**Motivacao:** em runtime real, `GET /api/v1/matches/*` estava retornando `500`, com frontend caindo em fallback de erro.

## Diagnostico (causa raiz)

1. Leitura de resultado persistido assumia payload sempre consistente.
2. Se existir registro historico/parcial inconsistente, o fluxo tentava tratar como `GENERATED` sem validacao minima.
3. A leitura usava caminho suscetivel a falha por inconsistencias de dados historicos (ex.: duplicidade/parcialidade), o que em runtime pode escalar para `500`.
4. Mapeamento JPA de `score` como primitivo (`short`) reduzia tolerancia a dados incompletos.

## Correcao aplicada (sem mudar regra/arquitetura)

1. Harden de null-safety na entidade:
   - `MatchResultJpaEntity.score` alterado de `short` para `Short`.
2. Harden de leitura no `MatchingUseCaseService`:
   - leitura passou a usar `findTopByUserIdAndVacancyIdOrderByCreatedAtDesc` (mais tolerante a historico inconsistente);
   - resposta `GENERATED` agora exige payload minimo consistente (`vacancyId`, `score`, `recommendation` parseavel);
   - se resultado persistido for inconsistente, o fluxo nao quebra: ignora esse registro e segue resolucao stateful normal (`MISSING_*`/`NOT_GENERATED`).
3. Fallback seguro de `generatedAt`:
   - quando ausente, usa `updatedAt` e depois `createdAt`.
4. Mapper endurecido:
   - `MatchResultMapper` com score seguro (`null -> 0`) para evitar NPE em caminho de dominio legado.

## Validacao adicionada

Novo teste unitario:
- `MatchingUseCaseServiceFailSafeTest`
  - garante que `getByVacancy` nao explode com resultado persistido inconsistente;
  - valida retorno stateful esperado:
    - sem contexto: `MISSING_RESUME`
    - com contexto completo e resultado inconsistente: `NOT_GENERATED`.

## Evidencia objetiva

Comando executado:
- `.\mvnw.cmd -B test`

Resultado:
- **BUILD SUCCESS**
- `Tests run: 68, Failures: 0, Errors: 0, Skipped: 2`

## Impacto de seguranca e contrato

1. Nenhuma mudanca de regra de negocio.
2. Nenhuma mudanca de arquitetura.
3. Sem auto-geracao no `GET`.
4. Contrato stateful preservado.
5. Reducao direta de risco de `500` por dados parciais em runtime.
