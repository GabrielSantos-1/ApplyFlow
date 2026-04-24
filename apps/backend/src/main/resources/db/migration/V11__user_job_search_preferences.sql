CREATE TABLE user_job_search_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    keyword VARCHAR(80) NOT NULL,
    normalized_keyword VARCHAR(80) NOT NULL,
    location VARCHAR(80),
    remote_only BOOLEAN NOT NULL DEFAULT FALSE,
    seniority VARCHAR(30),
    provider VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_run_at TIMESTAMPTZ,
    last_run_status VARCHAR(20),
    last_fetched_count INTEGER NOT NULL DEFAULT 0,
    last_inserted_count INTEGER NOT NULL DEFAULT 0,
    last_updated_count INTEGER NOT NULL DEFAULT 0,
    last_skipped_count INTEGER NOT NULL DEFAULT 0,
    last_failed_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_job_search_provider CHECK (provider IN ('REMOTIVE', 'ADZUNA')),
    CONSTRAINT chk_user_job_search_keyword_len CHECK (char_length(keyword) BETWEEN 2 AND 80),
    CONSTRAINT chk_user_job_search_normalized_keyword_len CHECK (char_length(normalized_keyword) BETWEEN 2 AND 80)
);

CREATE INDEX idx_user_job_search_preferences_user_id
    ON user_job_search_preferences(user_id);

CREATE INDEX idx_user_job_search_preferences_enabled_provider
    ON user_job_search_preferences(provider, enabled)
    WHERE enabled = TRUE;

CREATE UNIQUE INDEX ux_user_job_search_preferences_user_provider_keyword_location
    ON user_job_search_preferences(user_id, provider, normalized_keyword, coalesce(location, ''));
