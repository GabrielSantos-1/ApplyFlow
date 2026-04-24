ALTER TABLE vacancies
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(400),
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(64),
    ADD COLUMN IF NOT EXISTS raw_description TEXT,
    ADD COLUMN IF NOT EXISTS normalized_payload JSONB,
    ADD COLUMN IF NOT EXISTS discovered_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_ingested_at TIMESTAMPTZ DEFAULT NOW();

UPDATE vacancies
SET discovered_at = COALESCE(discovered_at, created_at),
    last_ingested_at = COALESCE(last_ingested_at, updated_at);

ALTER TABLE vacancies
    ALTER COLUMN discovered_at SET NOT NULL,
    ALTER COLUMN last_ingested_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vacancies_platform_checksum
    ON vacancies(platform_id, source_checksum);

CREATE TABLE IF NOT EXISTS vacancy_ingestion_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(60) NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    fetched_count INTEGER NOT NULL DEFAULT 0,
    normalized_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    persisted_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    error_summary VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ingestion_runs_source_started
    ON vacancy_ingestion_runs(source, started_at DESC);

CREATE TABLE IF NOT EXISTS vacancy_ingestion_locks (
    source VARCHAR(60) PRIMARY KEY,
    locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO platforms (id, name, type, created_at, updated_at)
VALUES ('3f940286-62f6-4f86-b858-68fec7702642', 'Remotive', 'MANUAL', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
