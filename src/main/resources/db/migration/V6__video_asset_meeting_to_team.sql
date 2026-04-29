ALTER TABLE video_asset DROP CONSTRAINT IF EXISTS video_asset_meeting_id_fkey;

ALTER TABLE video_asset RENAME COLUMN meeting_id TO team_id;

ALTER TABLE video_asset
    ADD CONSTRAINT video_asset_team_id_fkey FOREIGN KEY (team_id) REFERENCES teams(id);

ALTER INDEX idx_video_asset_meeting RENAME TO idx_video_asset_team;
