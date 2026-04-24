ALTER TABLE vacancies
    ADD COLUMN IF NOT EXISTS canonical_title VARCHAR(180),
    ADD COLUMN IF NOT EXISTS canonical_company VARCHAR(180),
    ADD COLUMN IF NOT EXISTS canonical_location VARCHAR(160),
    ADD COLUMN IF NOT EXISTS normalized_seniority VARCHAR(50),
    ADD COLUMN IF NOT EXISTS quality_score SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quality_flags JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS canonical_vacancy_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vacancies_canonical_vacancy'
    ) THEN
        ALTER TABLE vacancies
            ADD CONSTRAINT fk_vacancies_canonical_vacancy
            FOREIGN KEY (canonical_vacancy_id) REFERENCES vacancies(id);
    END IF;
END
$$;

UPDATE vacancies
SET canonical_title = NULLIF(regexp_replace(lower(trim(COALESCE(title, ''))), '\s+', ' ', 'g'), ''),
    canonical_company = NULLIF(regexp_replace(lower(trim(COALESCE(company, ''))), '\s+', ' ', 'g'), ''),
    canonical_location = CASE
        WHEN is_remote THEN 'remote'
        ELSE NULLIF(regexp_replace(lower(trim(COALESCE(location, ''))), '\s+', ' ', 'g'), '')
    END,
    normalized_seniority = CASE
        WHEN seniority IS NULL OR btrim(seniority) = '' THEN NULL
        WHEN lower(seniority) LIKE '%junior%' OR lower(seniority) LIKE '%jr%' THEN 'junior'
        WHEN lower(seniority) LIKE '%pleno%' OR lower(seniority) LIKE '%mid%' OR lower(seniority) LIKE '%middle%' THEN 'pleno'
        WHEN lower(seniority) LIKE '%senior%' OR lower(seniority) LIKE '%sr%' THEN 'senior'
        WHEN lower(seniority) LIKE '%staff%' OR lower(seniority) LIKE '%lead%' OR lower(seniority) LIKE '%principal%' OR lower(seniority) LIKE '%especialista%' THEN 'especialista'
        ELSE NULL
    END
WHERE canonical_title IS NULL
   OR canonical_company IS NULL
   OR canonical_location IS NULL
   OR normalized_seniority IS NULL;

UPDATE vacancies
SET quality_flags = '[]'::jsonb,
    quality_score = LEAST(
            100,
            (CASE WHEN canonical_title IS NOT NULL THEN 20 ELSE 0 END) +
            (CASE WHEN canonical_company IS NOT NULL THEN 20 ELSE 0 END) +
            (CASE WHEN canonical_location IS NOT NULL THEN 15 ELSE 0 END) +
            (CASE WHEN normalized_seniority IS NOT NULL THEN 15 ELSE 0 END) +
            (CASE WHEN required_skills IS NOT NULL AND btrim(required_skills) <> '' THEN 10 ELSE 0 END) +
            (CASE WHEN raw_description IS NOT NULL AND length(raw_description) >= 120 THEN 10 ELSE 0 END) +
            (CASE WHEN requirements IS NOT NULL AND length(requirements) >= 80 THEN 5 ELSE 0 END) +
            (CASE WHEN source_url IS NOT NULL AND btrim(source_url) <> '' THEN 5 ELSE 0 END)
    )
WHERE quality_score = 0
   OR quality_flags IS NULL;

UPDATE vacancies
SET dedupe_key = left(
        't:' || COALESCE(canonical_title, '') ||
        '|c:' || COALESCE(canonical_company, '') ||
        '|l:' || COALESCE(canonical_location, '') ||
        '|r:' || CASE WHEN is_remote THEN 'true' ELSE 'false' END ||
        '|s:' || COALESCE(normalized_seniority, ''),
        512
    )
WHERE dedupe_key IS NULL
   OR dedupe_key = '';

WITH ranked AS (
    SELECT id,
           dedupe_key,
           first_value(id) OVER (
               PARTITION BY dedupe_key
               ORDER BY COALESCE(discovered_at, created_at), created_at, id
           ) AS canonical_id,
           row_number() OVER (
               PARTITION BY dedupe_key
               ORDER BY COALESCE(discovered_at, created_at), created_at, id
           ) AS rn
    FROM vacancies
    WHERE dedupe_key IS NOT NULL
      AND dedupe_key <> ''
)
UPDATE vacancies v
SET is_duplicate = ranked.rn > 1,
    canonical_vacancy_id = CASE WHEN ranked.rn > 1 THEN ranked.canonical_id ELSE NULL END
FROM ranked
WHERE v.id = ranked.id;

CREATE INDEX IF NOT EXISTS idx_vacancies_dedupe_key
    ON vacancies(dedupe_key);

CREATE INDEX IF NOT EXISTS idx_vacancies_is_duplicate
    ON vacancies(is_duplicate);

CREATE INDEX IF NOT EXISTS idx_vacancies_canonical_vacancy_id
    ON vacancies(canonical_vacancy_id);
