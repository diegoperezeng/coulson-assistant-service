ALTER TABLE participants
    ADD COLUMN voice_id VARCHAR(255),
    ADD COLUMN presentation_audio_key VARCHAR(1000);

ALTER TABLE teams
    ADD COLUMN presentation_audio_key VARCHAR(1000),
    ADD COLUMN presentation_audio_duration_seconds DOUBLE PRECISION;
