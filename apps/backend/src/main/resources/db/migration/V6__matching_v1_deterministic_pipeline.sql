ALTER TABLE match_results
    ADD COLUMN IF NOT EXISTS resume_id UUID REFERENCES resumes(id),
    ADD COLUMN IF NOT EXISTS recommendation VARCHAR(20),
    ADD COLUMN IF NOT EXISTS strengths_json JSONB,
    ADD COLUMN IF NOT EXISTS gaps_json JSONB,
    ADD COLUMN IF NOT EXISTS keywords_to_add_json JSONB,
    ADD COLUMN IF NOT EXISTS algorithm_version VARCHAR(40),
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMPTZ;

UPDATE match_results mr
SET resume_id = rv.resume_id
FROM resume_variants rv
WHERE mr.resume_variant_id = rv.id
  AND mr.resume_id IS NULL;

UPDATE match_results
SET recommendation = CASE
    WHEN score >= 75 THEN 'APPLY'
    WHEN score >= 50 THEN 'REVIEW'
    ELSE 'IGNORE'
END
WHERE recommendation IS NULL;

UPDATE match_results
SET strengths_json = '[]'::jsonb
WHERE strengths_json IS NULL;

UPDATE match_results
SET gaps_json = '[]'::jsonb
WHERE gaps_json IS NULL;

UPDATE match_results
SET keywords_to_add_json = '[]'::jsonb
WHERE keywords_to_add_json IS NULL;

UPDATE match_results
SET algorithm_version = 'deterministic-v1'
WHERE algorithm_version IS NULL OR algorithm_version = '';

UPDATE match_results
SET generated_at = COALESCE(updated_at, created_at, NOW())
WHERE generated_at IS NULL;

ALTER TABLE match_results
    ALTER COLUMN recommendation SET NOT NULL,
    ALTER COLUMN strengths_json SET NOT NULL,
    ALTER COLUMN gaps_json SET NOT NULL,
    ALTER COLUMN keywords_to_add_json SET NOT NULL,
    ALTER COLUMN algorithm_version SET NOT NULL,
    ALTER COLUMN generated_at SET NOT NULL;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, vacancy_id
               ORDER BY generated_at DESC, updated_at DESC, created_at DESC, id DESC
           ) AS rn
    FROM match_results
)
DELETE FROM match_results mr
USING ranked r
WHERE mr.id = r.id
  AND r.rn > 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_match_results_user_vacancy'
    ) THEN
        ALTER TABLE match_results
            ADD CONSTRAINT uq_match_results_user_vacancy UNIQUE (user_id, vacancy_id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_match_results_user_generated
    ON match_results(user_id, generated_at DESC);
