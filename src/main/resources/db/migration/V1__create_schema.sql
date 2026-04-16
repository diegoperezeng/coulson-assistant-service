CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE meetings (
    id                UUID          PRIMARY KEY,
    title             VARCHAR(500)  NOT NULL,
    recorded_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    duration_sec      NUMERIC(10,2),
    video_storage_key VARCHAR(1000),
    full_text         TEXT          NOT NULL,
    embedded_at       TIMESTAMPTZ
);

CREATE TABLE transcription_segments (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id  UUID          NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    speaker     VARCHAR(255)  NOT NULL,
    start_sec   NUMERIC(10,3) NOT NULL,
    end_sec     NUMERIC(10,3) NOT NULL,
    text        TEXT          NOT NULL,
    embedding   vector(1536)
);

CREATE INDEX idx_meetings_recorded_at   ON meetings(recorded_at DESC);
CREATE INDEX idx_meetings_embedded      ON meetings(embedded_at) WHERE embedded_at IS NULL;
CREATE INDEX idx_segments_meeting_id    ON transcription_segments(meeting_id);
CREATE INDEX idx_segments_embedding     ON transcription_segments USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
