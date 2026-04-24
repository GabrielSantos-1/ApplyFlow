CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE candidate_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    headline VARCHAR(160),
    summary TEXT,
    location VARCHAR(160),
    primary_skills TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE resume_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    vacancy_id UUID,
    variant_label VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE platforms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vacancies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_id UUID REFERENCES platforms(id),
    external_id VARCHAR(120),
    title VARCHAR(180) NOT NULL,
    company VARCHAR(180) NOT NULL,
    location VARCHAR(160),
    is_remote BOOLEAN NOT NULL DEFAULT FALSE,
    seniority VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    required_skills TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(platform_id, external_id)
);

ALTER TABLE resume_variants
ADD CONSTRAINT fk_resume_variants_vacancy
FOREIGN KEY (vacancy_id) REFERENCES vacancies(id);

CREATE TABLE match_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    vacancy_id UUID NOT NULL REFERENCES vacancies(id),
    resume_variant_id UUID REFERENCES resume_variants(id),
    score SMALLINT NOT NULL CHECK (score >= 0 AND score <= 100),
    score_breakdown JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    vacancy_id UUID NOT NULL REFERENCES vacancies(id),
    resume_variant_id UUID REFERENCES resume_variants(id),
    message_draft TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_draft_id UUID NOT NULL REFERENCES application_drafts(id),
    stage VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    resource_id UUID,
    correlation_id VARCHAR(64),
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_profiles_user_id ON candidate_profiles(user_id);
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resume_variants_resume_id ON resume_variants(resume_id);
CREATE INDEX idx_vacancies_status_created_at ON vacancies(status, created_at DESC);
CREATE INDEX idx_match_results_user_vacancy ON match_results(user_id, vacancy_id);
CREATE INDEX idx_application_drafts_user_status ON application_drafts(user_id, status);
CREATE INDEX idx_application_tracking_draft ON application_tracking(application_draft_id);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource, resource_id);
