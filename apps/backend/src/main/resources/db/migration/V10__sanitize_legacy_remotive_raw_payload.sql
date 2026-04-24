UPDATE vacancies
SET raw_payload = (raw_payload - 'description') || '{"descriptionOmitted": true}'::jsonb,
    updated_at = NOW()
WHERE source = 'REMOTIVE'
  AND raw_payload ? 'description';
