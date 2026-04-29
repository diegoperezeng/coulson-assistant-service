CREATE TABLE video_asset (
    id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_id              VARCHAR(255)     NOT NULL UNIQUE,
    meeting_id          UUID             NOT NULL REFERENCES meetings(id),
    status              VARCHAR(20)      NOT NULL DEFAULT 'UPLOADED',
    playback_key        VARCHAR(1000),
    hls_master_key      VARCHAR(1000),
    duration_ms         BIGINT,
    width               INTEGER,
    height              INTEGER,
    fps                 DOUBLE PRECISION,
    playback_size_bytes BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_video_asset_ref_id    ON video_asset(ref_id);
CREATE INDEX idx_video_asset_meeting   ON video_asset(meeting_id);
CREATE INDEX idx_video_asset_status    ON video_asset(status);
