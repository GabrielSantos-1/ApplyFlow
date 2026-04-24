ALTER TABLE resumes
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS file_checksum_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS is_base BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMPTZ;

WITH ranked AS (
    SELECT id,
           user_id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) AS rn
    FROM resumes
)
UPDATE resumes r
SET is_base = CASE WHEN ranked.rn = 1 THEN TRUE ELSE FALSE END
FROM ranked
WHERE r.id = ranked.id;

CREATE INDEX IF NOT EXISTS idx_resumes_user_base
    ON resumes(user_id, is_base);
