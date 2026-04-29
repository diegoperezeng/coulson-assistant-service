ALTER TABLE transcription_jobs
    ADD COLUMN file_hash VARCHAR(64);

CREATE INDEX idx_transcription_jobs_file_hash ON transcription_jobs(file_hash);
