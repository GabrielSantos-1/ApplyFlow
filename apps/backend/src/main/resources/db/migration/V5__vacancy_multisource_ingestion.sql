CREATE TABLE IF NOT EXISTS vacancy_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vacancy_sources_type_display_name
    ON vacancy_sources(source_type, display_name);

ALTER TABLE vacancy_ingestion_runs
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_config_id UUID,
    ADD COLUMN IF NOT EXISTS inserted_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS skipped_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS triggered_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);

UPDATE vacancy_ingestion_runs
SET source_type = COALESCE(source_type, source),
    inserted_count = CASE WHEN inserted_count = 0 THEN COALESCE(persisted_count, 0) ELSE inserted_count END,
    skipped_count = CASE WHEN skipped_count = 0 THEN COALESCE(duplicate_count, 0) ELSE skipped_count END
WHERE source_type IS NULL OR inserted_count = 0 OR skipped_count = 0;

ALTER TABLE vacancy_ingestion_runs
    ALTER COLUMN source_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ingestion_runs_source_config'
    ) THEN
        ALTER TABLE vacancy_ingestion_runs
            ADD CONSTRAINT fk_ingestion_runs_source_config
            FOREIGN KEY (source_config_id) REFERENCES vacancy_sources(id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_ingestion_runs_source_type_started
    ON vacancy_ingestion_runs(source_type, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_ingestion_runs_source_config
    ON vacancy_ingestion_runs(source_config_id, started_at DESC);

ALTER TABLE vacancies
    ADD COLUMN IF NOT EXISTS source VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_tenant VARCHAR(180),
    ADD COLUMN IF NOT EXISTS external_job_id VARCHAR(180),
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(64),
    ADD COLUMN IF NOT EXISTS raw_payload JSONB,
    ADD COLUMN IF NOT EXISTS remote_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS employment_type VARCHAR(60),
    ADD COLUMN IF NOT EXISTS requirements TEXT;

UPDATE vacancies
SET source = COALESCE(source, 'REMOTIVE'),
    source_tenant = COALESCE(source_tenant, 'remotive.com'),
    external_job_id = COALESCE(external_job_id, external_id),
    checksum = COALESCE(checksum, source_checksum),
    raw_payload = COALESCE(raw_payload, normalized_payload),
    remote_type = COALESCE(remote_type, CASE WHEN is_remote THEN 'REMOTE' ELSE 'ONSITE' END),
    requirements = COALESCE(requirements, raw_description)
WHERE source IS NULL
   OR source_tenant IS NULL
   OR external_job_id IS NULL
   OR checksum IS NULL
   OR raw_payload IS NULL
   OR remote_type IS NULL
   OR requirements IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vacancies_source_tenant_external
    ON vacancies(source, source_tenant, external_job_id);

INSERT INTO platforms (id, name, type, created_at, updated_at)
VALUES
    ('3f940286-62f6-4f86-b858-68fec7702642', 'Remotive', 'MANUAL', NOW(), NOW()),
    ('d5c03468-e26a-4446-b72b-f050f9f4021f', 'Greenhouse', 'MANUAL', NOW(), NOW()),
    ('5a836ad4-0ca0-468b-bf9f-4cf9f47b5f10', 'Lever', 'MANUAL', NOW(), NOW()),
    ('8f853181-4db8-43cd-b741-5d5dc4f52f9a', 'Adzuna', 'MANUAL', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO vacancy_sources (id, source_type, display_name, config_json, enabled, created_at, updated_at)
VALUES
    ('b032f67e-5db6-491f-899a-c857c568f4db', 'REMOTIVE', 'Remotive Public', '{"tenant":"remotive.com","jobsPath":"/api/remote-jobs","maxJobsPerRun":200}'::jsonb, TRUE, NOW(), NOW()),
    ('ce971257-26f9-4de3-bf84-b1e23a43f73d', 'GREENHOUSE', 'Greenhouse Default', '{"boardToken":"example-board","tenant":"example-board"}'::jsonb, FALSE, NOW(), NOW()),
    ('5990e366-c485-4636-b931-93595d8e3fe9', 'LEVER', 'Lever Default', '{"site":"example","tenant":"example"}'::jsonb, FALSE, NOW(), NOW()),
    ('0ed2ed7b-b267-42d8-b2fb-d65b0b718fe0', 'ADZUNA', 'Adzuna Default', '{"country":"us","page":1,"resultsPerPage":30}'::jsonb, FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
