INSERT INTO vacancy_sources (id, source_type, display_name, config_json, enabled, created_at, updated_at)
VALUES
    ('8ca3c50e-a112-4014-a120-995c5d2f4531', 'GREENHOUSE', 'Greenhouse Stripe', '{"boardToken":"stripe","tenant":"stripe","maxJobsPerRun":120}'::jsonb, TRUE, NOW(), NOW()),
    ('568d46ec-2cd9-4ebe-b0fd-d2207006fb69', 'GREENHOUSE', 'Greenhouse Figma', '{"boardToken":"figma","tenant":"figma","maxJobsPerRun":80}'::jsonb, TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET config_json = EXCLUDED.config_json,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();
