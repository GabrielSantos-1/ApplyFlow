# ApplyFlow Backend

Backend Spring Boot do ApplyFlow com foco em seguranca, rastreabilidade e operacao controlada.

## Requisitos

- Java 21
- Docker (para stack de staging)

## Comandos principais

- Testes:
  - `.\mvnw.cmd -B test -DskipITs`
- Build local:
  - `.\mvnw.cmd -B clean package -DskipTests`

## Smoke runtime E2E

Script operacional para validar fluxo principal em runtime:

- Arquivo:
  - `apps/backend/ops/smoke/run-runtime-smoke.ps1`
- Exemplo:
  - `powershell -ExecutionPolicy Bypass -File apps/backend/ops/smoke/run-runtime-smoke.ps1 -BaseUrl http://localhost:8081 -Email "<usuario>" -Password "<senha>"`

Fluxo validado pelo smoke:

`vacancy -> match -> draft -> status -> tracking`

## Seguranca operacional

- Nao versionar `.env` reais.
- Nao persistir tokens em arquivos temporarios.
- Usar apenas `.env.example` como referencia de configuracao.
