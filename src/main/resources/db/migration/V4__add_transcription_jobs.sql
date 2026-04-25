CREATE TABLE transcription_jobs (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    video_name VARCHAR(500) NOT NULL,
    error      TEXT,
    result     TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status     ON transcription_jobs(status);
CREATE INDEX idx_jobs_created_at ON transcription_jobs(created_at DESC);
