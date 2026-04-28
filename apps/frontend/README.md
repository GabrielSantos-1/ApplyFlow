# ApplyFlow Frontend

Frontend Next.js (App Router) do ApplyFlow, consumindo API autenticada do backend.

## Requisitos

- Node.js 20+
- npm

## Comandos principais

- Desenvolvimento:
  - `npm run dev`
- Build:
  - `npm run build`
- Validacao de tipos:
  - `npm run typecheck`
- Gate de qualidade local (atual):
  - `npm run lint`

## Observacao sobre lint no Next 16

Neste repositorio o comando `next lint` foi removido do fluxo porque a stack atual nao possui configuracao ESLint versionada.
O script `lint` valida tipagem estrita (`tsc --noEmit`) para manter gate automatizado estavel enquanto o lint semantico nao e reintroduzido com configuracao dedicada.
