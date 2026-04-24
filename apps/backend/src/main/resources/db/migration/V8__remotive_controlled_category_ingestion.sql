UPDATE vacancy_sources
SET config_json = config_json
    || '{"maxJobsPerRun":200,"categories":["software-dev","devops"]}'::jsonb,
    updated_at = NOW()
WHERE source_type = 'REMOTIVE'
  AND display_name = 'Remotive Public';
