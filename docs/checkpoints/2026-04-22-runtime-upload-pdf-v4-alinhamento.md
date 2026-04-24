# Checkpoint - Runtime Upload PDF (2026-04-22)

## Contexto
- Bloco backend+produto de curriculo PDF ja implementado e testado em build.
- Falha em runtime observada: `POST /api/v1/resumes` retornando `500`.

## Causa raiz confirmada
- Ambiente staging estava com schema em `v3`.
- Flyway em boot validava apenas 3 migrations.
- `flyway_schema_history` sem entrada da `V4__resume_pdf_storage_and_base_flag.sql`.
- Tabela `resumes` sem colunas da V4 (`storage_path`, `content_type`, `file_size_bytes`, `file_checksum_sha256`, `is_base`, `uploaded_at`).

## Evidencias
1. Logs backend (antes): `Successfully validated 3 migrations`, `Current version of schema "public": 3`.
2. SQL (antes): `flyway_schema_history` com versoes 1..3.
3. SQL (antes): `information_schema.columns` de `resumes` com apenas 7 colunas legadas.

## Correcao executada
- Rebuild e restart do stack staging via `docker compose up -d --build`.
- Houve conflito de nome de container durante recreate; resolvido com `docker compose down` seguido de novo `up -d --build`.
- Flyway reaplicado no boot limpo com schema em versao 4.

## Validacao pos-correcao
1. Logs backend (depois): `Successfully validated 4 migrations`, `Current version of schema "public": 4`.
2. SQL (depois): `flyway_schema_history` com versoes 1..4 (V4 success=true).
3. SQL (depois): `resumes` contem todas as colunas novas da V4.
4. Runtime real:
   - usuario de teste seedado para autenticacao (`operational@test.local`)
   - login `POST /api/v1/auth/login` retornando token valido
   - upload multipart para `POST /api/v1/resumes` com PDF valido retornou `201`
   - resposta incluiu `contentType`, `fileSizeBytes`, `fileChecksumSha256`, `base=true`, `uploadedAt`
   - linha persistida no Postgres com `storage_path` privado e checksum
   - arquivo presente no container backend em `/app/data/private/resumes/{userId}/{resumeId}.pdf`
   - log operacional: `eventType=resume_pdf_upload outcome=success`

## Seguranca preservada
- Upload continua exigindo autenticacao/JWT.
- Ownership mantido por `user_id` autenticado.
- Validacao de assinatura `%PDF-`, content-type e limite de tamanho mantidas.
- Storage privado sem URL publica direta.

## Impactos e observacoes
- Stack atual de staging usa Postgres sem volume nomeado persistente; `docker compose down` recria banco limpo.
- Nao foi necessario alterar codigo para resolver o 500; causa foi desalinhamento de ambiente/schema.

## Estado final desta execucao
- Upload PDF funcionando em runtime real com ambiente alinhado ao codigo e schema V4.
- Sem bug residual identificado no endpoint nesta rodada.
